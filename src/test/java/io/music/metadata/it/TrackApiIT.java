package io.music.metadata.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class TrackApiIT extends MusicMetadataApiIT {

    private long pop;

    private long ladyGaga;

    private long bradleyCooper;

    @BeforeEach
    void createCatalogue() {
        pop = jdbcTemplate.queryForObject("SELECT id FROM genre WHERE name = 'Pop'", Long.class);
        ladyGaga = createArtist("Lady Gaga");
        bradleyCooper = createArtist("Bradley Cooper");
    }

    @Nested
    class CreateTrack {

        @Test
        void returnsTheNewTrackWithCreditsInOrder() {
            MvcTestResult result = createTrack("Shallow", """
                    [{"artistId": %d, "role": "PRIMARY"}, {"artistId": %d, "role": "PRIMARY"}]"""
                    .formatted(ladyGaga, bradleyCooper));

            assertThat(result).hasStatus(HttpStatus.CREATED);
            long trackId = idOf(result, "$.trackId");
            assertThat(result).hasHeader("Location", "http://localhost/api/v1/tracks/" + trackId);
            assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Shallow");
            assertThat(result).bodyJson().extractingPath("$.genre.name").isEqualTo("Pop");
            assertThat(result).bodyJson().extractingPath("$.explicit").isEqualTo(false);
            assertThat(result).bodyJson().extractingPath("$.artists[*].name").asArray()
                    .containsExactly("Lady Gaga", "Bradley Cooper");
        }

        @Test
        void storesTheCreditOrder() {
            long trackId = idOf(createTrack("Shallow", """
                    [{"artistId": %d, "role": "PRIMARY"}, {"artistId": %d, "role": "FEATURED"}]"""
                    .formatted(bradleyCooper, ladyGaga)), "$.trackId");

            List<Long> artistsInOrder = jdbcTemplate.queryForList(
                    "SELECT artist_id FROM track_artist WHERE track_id = ? ORDER BY position", Long.class, trackId);
            assertThat(artistsInOrder).containsExactly(bradleyCooper, ladyGaga);
        }

        @Test
        void allowsManyTracksWithoutAnIsrc() {
            assertThat(createTrack("Shallow", primary(ladyGaga))).hasStatus(HttpStatus.CREATED);
            assertThat(createTrack("Always Remember Us This Way", primary(ladyGaga))).hasStatus(HttpStatus.CREATED);
        }

    }

    @Nested
    class GetTrack {

        @Test
        void getTrack() {
            MvcTestResult created = createTrack("Shallow", "QZABC2600001", """
                    [{"artistId": %d, "role": "PRIMARY"}, {"artistId": %d, "role": "PRIMARY"}]"""
                    .formatted(ladyGaga, bradleyCooper));
            String location = created.getResponse().getHeader("Location");

            MvcTestResult result = mvc.get().uri(location).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.trackId").isEqualTo((int) idOf(created, "$.trackId"));
            assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Shallow");
            assertThat(result).bodyJson().extractingPath("$.isrc").isEqualTo("QZABC2600001");
            assertThat(result).bodyJson().extractingPath("$.genre.name").isEqualTo("Pop");
            assertThat(result).bodyJson().extractingPath("$.artists[*].name").asArray()
                    .containsExactly("Lady Gaga", "Bradley Cooper");
        }

    }

    @Nested
    class ListArtistTracks {

        @Test
        void returnsEveryTrackTheArtistIsCreditedOnWithTheirRole() {
            long solo = idOf(createTrack("Bad Romance", primary(ladyGaga)), "$.trackId");
            long featured = idOf(createTrack("Maybe It's Time", """
                    [{"artistId": %d, "role": "PRIMARY"}, {"artistId": %d, "role": "FEATURED"}]"""
                    .formatted(bradleyCooper, ladyGaga)), "$.trackId");
            createTrack("Black Eyes", primary(bradleyCooper));

            MvcTestResult result = mvc.get().uri("/api/v1/artists/{id}/tracks", ladyGaga).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.content[*].trackId").asArray()
                    .containsExactly((int) solo, (int) featured);
            assertThat(result).bodyJson().extractingPath("$.content[*].role").asArray()
                    .containsExactly("PRIMARY", "FEATURED");
            assertThat(result).bodyJson().extractingPath("$.content[1].artists[*].name").asArray()
                    .containsExactly("Bradley Cooper", "Lady Gaga");
            assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
        }

        @Test
        void pagesThroughTheTracks() {
            for (int i = 1; i <= 3; i++) {
                createTrack("Track " + i, primary(ladyGaga));
            }

            MvcTestResult result = mvc.get().uri("/api/v1/artists/{id}/tracks?page=1&size=2", ladyGaga).exchange();

            assertThat(result).bodyJson().extractingPath("$.content[*].title").asArray().containsExactly("Track 3");
            assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
            assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        }

        @Test
        void returnsAnEmptyPageForAnArtistWithoutTracks() {
            assertThat(mvc.get().uri("/api/v1/artists/{id}/tracks", ladyGaga))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content").asArray().isEmpty();
        }

    }

    private MvcTestResult createTrack(String title, String artistsJson) {
        return post("/api/v1/tracks", """
                {"title": "%s", "durationMs": 215000, "genreId": %d, "artists": %s}"""
                .formatted(title, pop, artistsJson));
    }

    private MvcTestResult createTrack(String title, String isrc, String artistsJson) {
        return post("/api/v1/tracks", """
                {"title": "%s", "durationMs": 215000, "genreId": %d, "isrc": "%s", "artists": %s}"""
                .formatted(title, pop, isrc, artistsJson));
    }

    private static String primary(long artistId) {
        return "[{\"artistId\": %d, \"role\": \"PRIMARY\"}]".formatted(artistId);
    }

}
