package io.music.metadata.service;

import static io.music.metadata.repository.entity.ArtistMother.artist;
import static io.music.metadata.repository.entity.ArtistMother.artistOfTheDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistAlias;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.repository.ArtistOfTheDayRepository;
import io.music.metadata.repository.ArtistRepository;
import io.music.metadata.repository.entity.ArtistEntity;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    /** Half an hour before midnight UTC: "today" must be this UTC date, whatever the machine's time zone. */
    private static final Instant NOW = Instant.parse("2026-11-01T23:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistOfTheDayRepository artistOfTheDayRepository;

    private ArtistService artistService;

    @BeforeEach
    void createService() {
        artistService = new ArtistService(artistRepository, artistOfTheDayRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    class CreateArtist {

        @Test
        void savesTheTrimmedNameAndReturnsTheSavedArtist() {
            given(artistRepository.save(any())).willAnswer(invocation -> {
                ArtistEntity saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 7L);
                return saved;
            });

            Artist artist = artistService.createArtist("  Nova Lane  ");

            ArgumentCaptor<ArtistEntity> entity = ArgumentCaptor.forClass(ArtistEntity.class);
            verify(artistRepository).save(entity.capture());
            assertThat(entity.getValue().getName()).isEqualTo("Nova Lane");
            assertThat(artist.getArtistId()).isEqualTo(7L);
            assertThat(artist.getName()).isEqualTo("Nova Lane");
            assertThat(artist.getAliases()).isEmpty();
        }

        @Test
        void rejectsANameThatIsBlankAfterTrimmingWithoutSaving() {
            assertThatThrownBy(() -> artistService.createArtist("   "))
                    .isInstanceOfSatisfying(InvalidFieldException.class,
                            ex -> assertThat(ex.getField()).isEqualTo("name"));
            verifyNoInteractions(artistRepository);
        }

    }

    @Nested
    class GetArtist {

        @Test
        void returnsTheArtistWithItsAliases() {
            ArtistEntity kairo = artist(3, "DJ Kairo");
            kairo.rename("Kairo");
            given(artistRepository.findWithAliasesById(3L)).willReturn(Optional.of(kairo));

            Artist artist = artistService.getArtist(3);

            assertThat(artist.getName()).isEqualTo("Kairo");
            assertThat(artist.getAliases()).containsExactly(new ArtistAlias("DJ Kairo"));
        }

        @Test
        void failsWithNotFoundForAnUnknownArtist() {
            given(artistRepository.findWithAliasesById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.getArtist(99)).isInstanceOf(NotFoundException.class);
        }

    }

    @Nested
    class ListArtists {

        @Test
        void requestsThePageOrderedByIdAndMapsIt() {
            given(artistRepository.findAll(any(Pageable.class))).willReturn(
                    new PageImpl<>(List.of(artist(3, "Kairo")), PageRequest.of(1, 1, Sort.by("id")), 8));

            ArtistPage page = artistService.listArtists(1, 1);

            ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
            verify(artistRepository).findAll(request.capture());
            assertThat(request.getValue().getPageNumber()).isEqualTo(1);
            assertThat(request.getValue().getPageSize()).isEqualTo(1);
            assertThat(request.getValue().getSort()).isEqualTo(Sort.by("id"));
            assertThat(page.getTotalElements()).isEqualTo(8L);
            assertThat(page.getContent()).extracting("name").containsExactly("Kairo");
        }

    }

    @Nested
    class RenameArtist {

        @Test
        void renamesKeepsThePreviousNameAsAnAliasAndFlushes() {
            ArtistEntity artist = artist(3, "Kanye West", 2);
            given(artistRepository.findWithAliasesById(3L)).willReturn(Optional.of(artist));

            Artist renamed = artistService.renameArtist(3, "  Ye  ", 2);

            assertThat(renamed.getName()).isEqualTo("Ye");
            assertThat(renamed.getAliases()).containsExactly(new ArtistAlias("Kanye West"));
            verify(artistRepository).flush();
        }

        @Test
        void doesNotKeepALetterCaseChangeAsAnAlias() {
            ArtistEntity artist = artist(3, "kairo", 0);
            given(artistRepository.findWithAliasesById(3L)).willReturn(Optional.of(artist));

            Artist renamed = artistService.renameArtist(3, "Kairo", 0);

            assertThat(renamed.getName()).isEqualTo("Kairo");
            assertThat(renamed.getAliases()).isEmpty();
        }

        @Test
        void rejectsAStaleVersionWithoutChangingTheArtist() {
            ArtistEntity artist = artist(3, "Kanye West", 2);
            given(artistRepository.findWithAliasesById(3L)).willReturn(Optional.of(artist));

            assertThatThrownBy(() -> artistService.renameArtist(3, "Ye", 1))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("version 1")
                    .hasMessageContaining("current version 2");
            assertThat(artist.getName()).isEqualTo("Kanye West");
            assertThat(artist.getAliases()).isEmpty();
            verify(artistRepository, never()).flush();
        }

        @Test
        void rejectsABlankNameWithoutChangingTheArtist() {
            ArtistEntity artist = artist(3, "Kanye West", 0);
            given(artistRepository.findWithAliasesById(3L)).willReturn(Optional.of(artist));

            assertThatThrownBy(() -> artistService.renameArtist(3, "   ", 0))
                    .isInstanceOf(InvalidFieldException.class);
            assertThat(artist.getName()).isEqualTo("Kanye West");
            verify(artistRepository, never()).flush();
        }

        @Test
        void failsWithNotFoundForAnUnknownArtist() {
            given(artistRepository.findWithAliasesById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.renameArtist(99, "Ye", 0)).isInstanceOf(NotFoundException.class);
            verify(artistRepository, never()).flush();
        }

    }

    @Nested
    class GetArtistOfTheDay {

        @Test
        void returnsTodaysRecordedArtistWithoutRecordingAgain() {
            given(artistOfTheDayRepository.findWithArtistByDay(TODAY))
                    .willReturn(Optional.of(artistOfTheDay(TODAY, artist(2, "The Midnight Owls"))));

            Optional<ArtistOfTheDay> pick = artistService.getArtistOfTheDay();

            assertThat(pick).hasValueSatisfying(artistOfTheDay -> {
                assertThat(artistOfTheDay.getDate()).isEqualTo(TODAY);
                assertThat(artistOfTheDay.getArtist().getName()).isEqualTo("The Midnight Owls");
            });
            verify(artistOfTheDayRepository, never()).recordNextArtist(any());
        }

        @Test
        void recordsTheNextArtistOnTheFirstRequestOfTheDayThenReadsIt() {
            given(artistOfTheDayRepository.findWithArtistByDay(TODAY))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(artistOfTheDay(TODAY, artist(3, "Kairo"))));

            Optional<ArtistOfTheDay> pick = artistService.getArtistOfTheDay();

            verify(artistOfTheDayRepository).recordNextArtist(TODAY);
            assertThat(pick).map(artistOfTheDay -> artistOfTheDay.getArtist().getName()).hasValue("Kairo");
        }

        @Test
        void isEmptyWhenThereAreNoArtistsToRecord() {
            given(artistOfTheDayRepository.findWithArtistByDay(TODAY)).willReturn(Optional.empty());

            assertThat(artistService.getArtistOfTheDay()).isEmpty();
            verify(artistOfTheDayRepository).recordNextArtist(TODAY);
        }

    }

}
