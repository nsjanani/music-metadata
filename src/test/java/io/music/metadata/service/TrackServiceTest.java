package io.music.metadata.service;

import static io.music.metadata.repository.entity.ArtistMother.artist;
import static io.music.metadata.repository.entity.GenreMother.genre;
import static io.music.metadata.repository.entity.TrackMother.track;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import io.music.metadata.api.model.ArtistTrack;
import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.api.model.CreateTrackRequest;
import io.music.metadata.api.model.Genre;
import io.music.metadata.api.model.Role;
import io.music.metadata.api.model.Track;
import io.music.metadata.api.model.TrackCredit;
import io.music.metadata.api.model.TrackCreditRequest;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.repository.ArtistRepository;
import io.music.metadata.repository.GenreRepository;
import io.music.metadata.repository.TrackArtistRepository;
import io.music.metadata.repository.TrackRepository;
import io.music.metadata.repository.entity.ArtistEntity;
import io.music.metadata.repository.entity.CreditRole;
import io.music.metadata.repository.entity.GenreEntity;
import io.music.metadata.repository.entity.TrackArtistMother;
import io.music.metadata.repository.entity.TrackEntity;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private TrackArtistRepository trackArtistRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private GenreRepository genreRepository;

    private TrackService trackService;

    private final GenreEntity pop = genre(11, "Pop");
    private final ArtistEntity lunaVega = artist(6, "Luna Vega");
    private final ArtistEntity novaLane = artist(1, "Nova Lane");

    @BeforeEach
    void createService() {
        trackService = new TrackService(trackRepository, trackArtistRepository, artistRepository, genreRepository);
    }

    @Nested
    class CreateTrack {

        @Test
        void savesTheTrackWithCreditsInRequestOrderAndReturnsIt() {
            given(genreRepository.findById(11L)).willReturn(Optional.of(pop));
            // The repository returns artists in id order; the credits must follow the request order instead.
            given(artistRepository.findAllById(anyIterable())).willReturn(List.of(novaLane, lunaVega));
            given(trackRepository.save(any())).willAnswer(invocation -> {
                TrackEntity saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 42L);
                return saved;
            });

            Track track = trackService.createTrack(request("  Northern Lights  ",
                    credit(6, Role.PRIMARY), credit(1, Role.FEATURED)));

            Track expected = new Track(42L, "Northern Lights", 242000, new Genre(11L, "Pop"), false, List.of(
                    new TrackCredit(6L, "Luna Vega", Role.PRIMARY),
                    new TrackCredit(1L, "Nova Lane", Role.FEATURED)));
            assertThat(track).isEqualTo(expected);
        }

        @Test
        void rejectsDuplicateIsrc() {
            given(genreRepository.findById(11L)).willReturn(Optional.of(pop));
            given(artistRepository.findAllById(anyIterable())).willReturn(List.of(lunaVega));
            given(trackRepository.existsByIsrc("QZABC2600001")).willReturn(true);

            assertThatThrownBy(() -> trackService.createTrack(
                    request("Northern Lights", credit(6, Role.PRIMARY)).isrc("QZABC2600001")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("QZABC2600001");
            verify(trackRepository, never()).save(any());
        }

        @Test
        void doesNotCheckTheIsrcWhenNoneIsGiven() {
            given(genreRepository.findById(11L)).willReturn(Optional.of(pop));
            given(artistRepository.findAllById(anyIterable())).willReturn(List.of(lunaVega));
            given(trackRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            trackService.createTrack(request("Northern Lights", credit(6, Role.PRIMARY)));

            verify(trackRepository, never()).existsByIsrc(any());
        }

        @Test
        void rejectsABlankTitle() {
            assertInvalidField(request("   ", credit(6, Role.PRIMARY)), "title");
            verifyNoInteractions(trackRepository, artistRepository, genreRepository);
        }

        @Test
        void rejectsTheSameArtistTwice() {
            assertInvalidField(request("Northern Lights", credit(6, Role.PRIMARY), credit(6, Role.FEATURED)),
                    "artists");
            verifyNoInteractions(trackRepository, artistRepository, genreRepository);
        }

        @Test
        void requiresAPrimaryArtist() {
            assertInvalidField(request("Northern Lights", credit(6, Role.FEATURED)), "artists");
            verifyNoInteractions(trackRepository, artistRepository, genreRepository);
        }

        @Test
        void rejectsAnUnknownGenre() {
            given(genreRepository.findById(11L)).willReturn(Optional.empty());

            assertInvalidField(request("Northern Lights", credit(6, Role.PRIMARY)), "genreId");
            verify(trackRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownArtistsAndNamesThem() {
            given(genreRepository.findById(11L)).willReturn(Optional.of(pop));
            given(artistRepository.findAllById(anyIterable())).willReturn(List.of(lunaVega));

            assertThatThrownBy(() -> trackService.createTrack(
                    request("Northern Lights", credit(6, Role.PRIMARY), credit(99, Role.FEATURED))))
                    .isInstanceOfSatisfying(InvalidFieldException.class, ex -> {
                        assertThat(ex.getField()).isEqualTo("artists");
                        assertThat(ex.getMessage()).contains("99");
                    });
            verify(trackRepository, never()).save(any());
        }

    }

    @Nested
    class GetTrack {

        @Test
        void returnsTheTrack() {
            TrackEntity twoLanterns = track(9, "Two Lanterns", 233000, pop, LocalDate.of(2025, 6, 12), false, null);
            twoLanterns.addCredit(lunaVega, CreditRole.PRIMARY);
            given(trackRepository.findWithDetailsById(9L)).willReturn(Optional.of(twoLanterns));

            assertThat(trackService.getTrack(9).getTitle()).isEqualTo("Two Lanterns");
        }

        @Test
        void failsWithNotFoundForAnUnknownTrack() {
            given(trackRepository.findWithDetailsById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.getTrack(99)).isInstanceOf(NotFoundException.class);
        }

    }

    @Nested
    class ListArtistTracks {

        @Test
        void keepsThePageOrderEvenWhenTheTracksAreLoadedInAnotherOrder() {
            TrackEntity first = track(10, "Velvet Static", 205000, pop, null, true, null);
            first.addCredit(novaLane, CreditRole.PRIMARY);
            first.addCredit(lunaVega, CreditRole.FEATURED);
            TrackEntity second = track(12, "Northern Lights", 242000, pop, null, false, null);
            second.addCredit(lunaVega, CreditRole.PRIMARY);
            given(artistRepository.existsById(6L)).willReturn(true);
            given(trackArtistRepository.findByIdArtistId(eq(6L), any(Pageable.class))).willReturn(new PageImpl<>(
                    List.of(TrackArtistMother.credit(first, lunaVega, CreditRole.FEATURED),
                            TrackArtistMother.credit(second, lunaVega, CreditRole.PRIMARY)),
                    PageRequest.of(0, 2), 3));
            given(trackRepository.findByIdIn(List.of(10L, 12L))).willReturn(List.of(second, first));

            ArtistTrackPage page = trackService.listArtistTracks(6, 0, 2);

            assertThat(page.getContent()).extracting(ArtistTrack::getTrackId).containsExactly(10L, 12L);
            assertThat(page.getContent()).extracting(ArtistTrack::getRole)
                    .containsExactly(Role.FEATURED, Role.PRIMARY);
            assertThat(page.getTotalElements()).isEqualTo(3L);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        void requestsTheGivenPageOrderedByTrackId() {
            given(artistRepository.existsById(6L)).willReturn(true);
            given(trackArtistRepository.findByIdArtistId(eq(6L), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(3, 25), 0));

            trackService.listArtistTracks(6, 3, 25);

            ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
            verify(trackArtistRepository).findByIdArtistId(eq(6L), request.capture());
            assertThat(request.getValue().getPageNumber()).isEqualTo(3);
            assertThat(request.getValue().getPageSize()).isEqualTo(25);
            assertThat(request.getValue().getSort()).isEqualTo(Sort.by("id.trackId"));
        }

        @Test
        void failsWithNotFoundForAnUnknownArtistWithoutQueryingTracks() {
            given(artistRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> trackService.listArtistTracks(99, 0, 20)).isInstanceOf(NotFoundException.class);
            verifyNoInteractions(trackRepository, trackArtistRepository);
        }

    }

    private void assertInvalidField(CreateTrackRequest request, String field) {
        assertThatThrownBy(() -> trackService.createTrack(request))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo(field));
    }

    private static CreateTrackRequest request(String title, TrackCreditRequest... credits) {
        return new CreateTrackRequest(title, 242000, 11L, List.of(credits)).explicit(null);
    }

    private static TrackCreditRequest credit(long artistId, Role role) {
        return new TrackCreditRequest(artistId, role);
    }

}
