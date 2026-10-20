package io.music.metadata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;

import io.music.metadata.api.model.TrackCreditRequest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.music.metadata.api.model.CreateTrackRequest;
import io.music.metadata.api.model.Genre;
import io.music.metadata.api.model.Role;
import io.music.metadata.api.model.Track;
import io.music.metadata.api.model.TrackCredit;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.service.TrackService;

@WebMvcTest(TrackController.class)
class TrackControllerTest {

    private static final String SHALLOW = """
            {"title": "Shallow", "durationMs": 215000, "genreId": 3, "releaseDate": "2018-09-27",
             "explicit": true, "isrc": "QZABC2600001",
             "artists": [{"artistId": 1, "role": "PRIMARY"}, {"artistId": 2, "role": "FEATURED"}]}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TrackService trackService;

    @Nested
    class CreateTrack {

        @Test
        void returnsTheCreatedTrack() {
            given(trackService.createTrack(any())).willReturn(shallow());

            MvcTestResult result = post(SHALLOW);

            assertThat(result).hasStatus(HttpStatus.CREATED)
                    .hasHeader("Location", "http://localhost/api/v1/tracks/42");
            assertThat(result).bodyJson().extractingPath("$.genre.name").isEqualTo("Pop");
            assertThat(result).bodyJson().extractingPath("$.artists[*].name").asArray()
                    .containsExactly("Lady Gaga", "Bradley Cooper");
        }

        @Test
        void passesEveryFieldAndTheCreditOrderToTheService() {
            given(trackService.createTrack(any())).willReturn(shallow());

            post(SHALLOW);

            CreateTrackRequest expected = new CreateTrackRequest("Shallow", 215000, 3L, List.of(
                    new TrackCreditRequest(1L, Role.PRIMARY),
                    new TrackCreditRequest(2L, Role.FEATURED)))
                    .releaseDate(LocalDate.of(2018, 9, 27))
                    .explicit(true)
                    .isrc("QZABC2600001");
            verify(trackService).createTrack(expected);
        }

        @Test
        void defaultsExplicitToFalse() {
            given(trackService.createTrack(any())).willReturn(shallow());

            post("""
                    {"title": "Shallow", "durationMs": 215000, "genreId": 3,
                     "artists": [{"artistId": 1, "role": "PRIMARY"}]}""");

            ArgumentCaptor<CreateTrackRequest> request = ArgumentCaptor.forClass(CreateTrackRequest.class);
            verify(trackService).createTrack(request.capture());
            assertThat(request.getValue().getExplicit()).isFalse();
        }

        @Test
        void rejectsAMalformedIsrc() {
            assertValidationError(post(SHALLOW.replace("QZABC2600001", "not-an-isrc")), "isrc");
            verifyNoInteractions(trackService);
        }

        @Test
        void rejectsANonPositiveDuration() {
            assertValidationError(post(SHALLOW.replace("215000", "0")), "durationMs");
            verifyNoInteractions(trackService);
        }

        @Test
        void rejectsAnEmptyCreditList() {
            assertValidationError(post("""
                    {"title": "Shallow", "durationMs": 215000, "genreId": 3, "artists": []}"""), "artists");
            verifyNoInteractions(trackService);
        }

        @Test
        void returnsBadRequestWhenTheServiceRejectsTheCredits() {
            given(trackService.createTrack(any()))
                    .willThrow(new InvalidFieldException("artists", "must include at least one PRIMARY artist"));

            assertValidationError(post(SHALLOW), "artists");
        }

        @Test
        void returnsConflictForADuplicateIsrc() {
            given(trackService.createTrack(any())).willThrow(new ConflictException("duplicate ISRC"));

            assertThat(post(SHALLOW))
                    .hasStatus(HttpStatus.CONFLICT)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

    }

    @Nested
    class GetTrack {

        @Test
        void returnsTheTrack() {
            given(trackService.getTrack(42)).willReturn(shallow());

            assertThat(mvc.get().uri("/api/v1/tracks/42"))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "no-cache")
                    .bodyJson().extractingPath("$.title").isEqualTo("Shallow");
        }

        @Test
        void returnsNotFoundAsProblemDetails() {
            given(trackService.getTrack(42)).willThrow(new NotFoundException("Track", 42));

            assertThat(mvc.get().uri("/api/v1/tracks/42"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/v1/tracks").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static Track shallow() {
        return new Track(42L, "Shallow", 215000, new Genre(3L, "Pop"), false, List.of(
                new TrackCredit(1L, "Lady Gaga", Role.PRIMARY),
                new TrackCredit(2L, "Bradley Cooper", Role.PRIMARY)));
    }

    private static void assertValidationError(MvcTestResult result, String field) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors[*].field").asArray().contains(field);
    }

}
