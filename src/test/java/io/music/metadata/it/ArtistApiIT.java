package io.music.metadata.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ArtistApiIT extends MusicMetadataApiIT {

    @Nested
    class CreateArtist {

        @Test
        void returnsTheCreatedArtist() {
            MvcTestResult result = post("/api/v1/artists", """
                    {"name": "Nova Lane"}""");

            assertThat(result).hasStatus(HttpStatus.CREATED);
            long artistId = idOf(result, "$.artistId");
            assertThat(result).hasHeader("Location", "http://localhost/api/v1/artists/" + artistId);
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Nova Lane");
            assertThat(result).bodyJson().extractingPath("$.aliases").asArray().isEmpty();
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(0);
        }

        @Test
        void allowsTwoArtistsWithTheSameName() {
            createArtist("Nirvana");

            assertThat(post("/api/v1/artists", """
                    {"name": "Nirvana"}""")).hasStatus(HttpStatus.CREATED);
        }

    }

    @Nested
    class GetArtist {

        @Test
        void returnsTheSavedArtistWithItsAliases() {
            long artistId = createArtist("Nova Lane");
            rename(artistId, "Nova", 0);

            MvcTestResult result = mvc.get().uri("/api/v1/artists/{id}", artistId).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Nova");
            assertThat(result).bodyJson().extractingPath("$.aliases[*].name").asArray().containsExactly("Nova Lane");
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(1);
        }

    }

    @Nested
    class ListArtists {

        @Test
        void returnsAPageOrderedById() {
            long first = createArtist("Nova Lane");
            long second = createArtist("Kairo");
            createArtist("Ama Sol");

            MvcTestResult result = mvc.get().uri("/api/v1/artists?page=0&size=2").exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.content[*].artistId").asArray()
                    .containsExactly((int) first, (int) second);
            assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
            assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        }

    }

    @Nested
    class RenameArtist {

        @Test
        void renamingBackToAnAliasSwapsItWithTheCurrentName() {
            long artistId = createArtist("Kanye West");
            rename(artistId, "Ye", 0);

            MvcTestResult result = rename(artistId, "kanye west", 1);

            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("kanye west");
            assertThat(result).bodyJson().extractingPath("$.aliases[*].name").asArray().containsExactly("Ye");
        }

    }

    @Nested
    class GetArtistOfTheDay {

        private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 1);

        @Test
        void returnsNoContentWhenThereAreNoArtists() {
            setToday(DAY_1);

            assertThat(mvc.get().uri("/api/v1/artists/of-the-day")).hasStatus(HttpStatus.NO_CONTENT);
        }

        @Test
        void startsWithTheFirstArtistAndKeepsItForTheWholeDay() {
            long first = createArtist("Nova Lane");
            createArtist("Kairo");
            setToday(DAY_1);

            MvcTestResult result = mvc.get().uri("/api/v1/artists/of-the-day").exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-11-01");
            assertThat(result).bodyJson().extractingPath("$.artist.name").isEqualTo("Nova Lane");
            assertThat(artistOfTheDay()).isEqualTo(first);
            assertThat(artistOfTheDay()).isEqualTo(first);
            assertThat(rowsFor(DAY_1)).isEqualTo(1);
        }

        @Test
        void rotatesThroughEveryArtistInTheOrderTheyWereAddedThenStartsAgain() {
            long a = createArtist("A");
            long b = createArtist("B");
            long c = createArtist("C");

            assertThat(artistsOnConsecutiveDays(4)).containsExactly(a, b, c, a);
        }

        @Test
        void aNewArtistJoinsTheEndOfTheCurrentRotation() {
            long a = createArtist("A");
            long b = createArtist("B");
            assertThat(artistOn(DAY_1)).isEqualTo(a);

            long c = createArtist("C");

            assertThat(artistOn(DAY_1.plusDays(1))).isEqualTo(b);
            assertThat(artistOn(DAY_1.plusDays(2))).isEqualTo(c);
            assertThat(artistOn(DAY_1.plusDays(3))).isEqualTo(a);
        }

        @Test
        void showsTheArtistsCurrentName() {
            long artistId = createArtist("Kanye West");
            setToday(DAY_1);
            artistOfTheDay();

            patch("/api/v1/artists/" + artistId, """
                    {"name": "Ye", "version": 0}""");

            assertThat(mvc.get().uri("/api/v1/artists/of-the-day"))
                    .bodyJson().extractingPath("$.artist.name").isEqualTo("Ye");
        }

        private List<Long> artistsOnConsecutiveDays(int days) {
            List<Long> picks = new ArrayList<>();
            for (int day = 0; day < days; day++) {
                picks.add(artistOn(DAY_1.plusDays(day)));
            }
            return picks;
        }

        private long artistOn(LocalDate day) {
            setToday(day);
            return artistOfTheDay();
        }

        private long artistOfTheDay() {
            MvcTestResult result = mvc.get().uri("/api/v1/artists/of-the-day").exchange();
            assertThat(result).hasStatusOk();
            return idOf(result, "$.artist.artistId");
        }

        private void setToday(LocalDate day) {
            clock.setInstant(day.atTime(12, 0).toInstant(ZoneOffset.UTC));
        }

        private int rowsFor(LocalDate day) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM artist_of_the_day WHERE day = ?", Integer.class,
                    day);
        }

    }

    private MvcTestResult rename(long artistId, String name, int version) {
        return patch("/api/v1/artists/" + artistId, "{\"name\": \"" + name + "\", \"version\": " + version + "}");
    }

}
