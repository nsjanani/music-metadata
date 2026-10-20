package io.music.metadata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistAlias;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.api.model.ArtistSummary;
import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.configuration.TestClock;
import io.music.metadata.configuration.TestClockConfiguration;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.service.ArtistService;
import io.music.metadata.service.TrackService;

@WebMvcTest(ArtistController.class)
@Import(TestClockConfiguration.class)
class ArtistControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-01T23:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private TestClock clock;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private TrackService trackService;

    @BeforeEach
    void setTheTime() {
        clock.setInstant(NOW);
    }

    @Nested
    class CreateArtist {

        @Test
        void returnsCreatedArtist() {
            given(artistService.createArtist("Nova Lane")).willReturn(artist(7, "Nova Lane"));

            MvcTestResult result = post("/api/v1/artists", """
                    {"name": "Nova Lane"}""");

            assertThat(result).hasStatus(HttpStatus.CREATED)
                    .hasHeader("Location", "http://localhost/api/v1/artists/7");
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Nova Lane");
        }

        @Test
        void rejectsAMissingName() {
            assertValidationError(post("/api/v1/artists", "{}"), "name");
            verifyNoInteractions(artistService);
        }

        @Test
        void returnsBadRequestWhenTheServiceRejectsAField() {
            given(artistService.createArtist("   ")).willThrow(new InvalidFieldException("name", "must not be blank"));

            assertValidationError(post("/api/v1/artists", """
                    {"name": "   "}"""), "name");
        }

    }

    @Nested
    class GetArtist {

        @Test
        void returnsTheArtist() {
            given(artistService.getArtist(7)).willReturn(artist(7, "Ye").aliases(List.of(new ArtistAlias("Kanye West"))).version(1));

            MvcTestResult result = mvc.get().uri("/api/v1/artists/7").exchange();

            assertThat(result).hasStatusOk().hasHeader("Cache-Control", "no-cache");
            assertThat(result).bodyJson().extractingPath("$.aliases[0].name").isEqualTo("Kanye West");
            assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(1);
        }

        @Test
        void returnsNotFoundAsProblemDetails() {
            given(artistService.getArtist(7)).willThrow(new NotFoundException("Artist", 7));

            assertThat(mvc.get().uri("/api/v1/artists/7"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

        @Test
        void rejectsAnIdBelowOneAsPerTheSpec() {
            assertValidationError(mvc.get().uri("/api/v1/artists/0").exchange(), "artistId");
            verifyNoInteractions(artistService);
        }

    }

    @Nested
    class ListArtists {

        @Test
        void usesTheDefaultPageAndSizeFromTheSpec() {
            given(artistService.listArtists(0, 20)).willReturn(new ArtistPage(0, 20, 0L, 0, List.of()));

            assertThat(mvc.get().uri("/api/v1/artists")).hasStatusOk().hasHeader("Cache-Control", "no-cache");
            verify(artistService).listArtists(0, 20);
        }

        @Test
        void passesTheRequestedPageAndSize() {
            given(artistService.listArtists(2, 50)).willReturn(new ArtistPage(2, 50, 0L, 0, List.of()));

            assertThat(mvc.get().uri("/api/v1/artists?page=2&size=50")).hasStatusOk();
            verify(artistService).listArtists(2, 50);
        }

        @Test
        void rejectsAPageSizeAboveTheMaximum() {
            assertValidationError(mvc.get().uri("/api/v1/artists?size=101").exchange(), "size");
            verifyNoInteractions(artistService);
        }

    }

    @Nested
    class RenameArtist {

        @Test
        void returnsTheRenamedArtist() {
            given(artistService.renameArtist(7, "Ye", 3)).willReturn(artist(7, "Ye").version(4));

            assertThat(patch("/api/v1/artists/7", """
                    {"name": "Ye", "version": 3}"""))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.version").isEqualTo(4);
        }

        @Test
        void returnsConflictForAStaleVersion() {
            given(artistService.renameArtist(7, "Ye", 3)).willThrow(new ConflictException("stale"));

            assertThat(patch("/api/v1/artists/7", """
                    {"name": "Ye", "version": 3}"""))
                    .hasStatus(HttpStatus.CONFLICT)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

        @Test
        void rejectsAMissingVersion() {
            assertValidationError(patch("/api/v1/artists/7", """
                    {"name": "Ye"}"""), "version");
            verifyNoInteractions(artistService);
        }

    }

    @Nested
    class ListArtistTracks {

        @Test
        void returnsTheArtistTracksList() {
            given(trackService.listArtistTracks(7, 1, 10)).willReturn(new ArtistTrackPage(1, 10, 0L, 0, List.of()));

            assertThat(mvc.get().uri("/api/v1/artists/7/tracks?page=1&size=10"))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "no-cache");
            verify(trackService).listArtistTracks(7, 1, 10);
        }

    }

    @Nested
    class GetArtistOfTheDay {

        @Test
        void isCacheableUntilMidnightUtc() {
            given(artistService.getArtistOfTheDay()).willReturn(Optional.of(
                    new ArtistOfTheDay(LocalDate.of(2026, 11, 1), new ArtistSummary(7L, "Nova Lane"))));

            MvcTestResult result = mvc.get().uri("/api/v1/artists/of-the-day").exchange();

            assertThat(result).hasStatusOk().hasHeader("Cache-Control", "max-age=3600, public");
            assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-11-01");
            assertThat(result).bodyJson().extractingPath("$.artist.name").isEqualTo("Nova Lane");
        }

        @Test
        void returnsNoContentWhenThereAreNoArtists() {
            given(artistService.getArtistOfTheDay()).willReturn(Optional.empty());

            assertThat(mvc.get().uri("/api/v1/artists/of-the-day"))
                    .hasStatus(HttpStatus.NO_CONTENT)
                    .hasHeader("Cache-Control", "no-cache");
        }

    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult patch(String uri, String json) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static Artist artist(long id, String name) {
        return new Artist(id, name, List.of(), 0);
    }

    private static void assertValidationError(MvcTestResult result, String field) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors[*].field").asArray().contains(field);
    }

}
