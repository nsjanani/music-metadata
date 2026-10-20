package io.music.metadata.mapper;

import static io.music.metadata.repository.entity.ArtistMother.artist;
import static io.music.metadata.repository.entity.GenreMother.genre;
import static io.music.metadata.repository.entity.TrackMother.track;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.music.metadata.api.model.ArtistTrack;
import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.api.model.Genre;
import io.music.metadata.api.model.Role;
import io.music.metadata.api.model.Track;
import io.music.metadata.api.model.TrackCredit;
import io.music.metadata.repository.entity.ArtistEntity;
import io.music.metadata.repository.entity.CreditRole;
import io.music.metadata.repository.entity.TrackEntity;

class TrackMapperTest {

    private static final LocalDate RELEASE_DATE = LocalDate.of(2018, 9, 27);

    private ArtistEntity ladyGaga;
    private ArtistEntity bradleyCooper;
    private TrackEntity shallow;

    @BeforeEach
    void createDuet() {
        ladyGaga = artist(1, "Lady Gaga");
        bradleyCooper = artist(2, "Bradley Cooper");
        shallow = track(42, "Shallow", 215000, genre(3, "Pop"), RELEASE_DATE, true, "QZABC2600001");
        shallow.addCredit(ladyGaga, CreditRole.PRIMARY);
        shallow.addCredit(bradleyCooper, CreditRole.FEATURED);
    }

    @Test
    void toTrackMapsEveryField() {
        Track track = TrackMapper.toTrack(shallow);
        Track expected = new Track(42L, "Shallow", 215000, new Genre(3L, "Pop"), true, List.of())
                .releaseDate(RELEASE_DATE)
                .isrc("QZABC2600001");

        assertThat(TrackMapper.toTrack(shallow))
                .usingRecursiveComparison()
                .ignoringFields("artists")
                .isEqualTo(expected);
    }

    @Test
    void toTrackListsCreditsInCreditOrderWithRoles() {
        assertThat(TrackMapper.toTrack(shallow).getArtists()).containsExactly(
                new TrackCredit(1L, "Lady Gaga", Role.PRIMARY),
                new TrackCredit(2L, "Bradley Cooper", Role.FEATURED));
    }

    @Test
    void toTrackShowsTheArtistsCurrentName() {
        ladyGaga.rename("Stefani Germanotta");

        assertThat(TrackMapper.toTrack(shallow).getArtists().getFirst().getName()).isEqualTo("Stefani Germanotta");
    }

    @Test
    void toTrackLeavesOptionalFieldsEmpty() {
        TrackEntity entity = track(43, "Untitled", 1000, genre(3, "Pop"), null, false, null);
        entity.addCredit(ladyGaga, CreditRole.PRIMARY);

        Track track = TrackMapper.toTrack(entity);

        assertThat(track.getReleaseDate()).isNull();
        assertThat(track.getIsrc()).isNull();
        assertThat(track.getExplicit()).isFalse();
    }

    @Test
    void toArtistTrackIncludesTheRequestedArtistsRole() {
        ArtistTrack asFeatured = TrackMapper.toArtistTrack(shallow, 2);

        assertThat(asFeatured.getRole()).isEqualTo(Role.FEATURED);
        assertThat(asFeatured.getTrackId()).isEqualTo(42L);
        assertThat(asFeatured.getArtists()).extracting(TrackCredit::getName)
                .containsExactly("Lady Gaga", "Bradley Cooper");
        assertThat(TrackMapper.toArtistTrack(shallow, 1).getRole()).isEqualTo(Role.PRIMARY);
    }

    @Test
    void toArtistTrackFailsForAnArtistNotCreditedOnTheTrack() {
        assertThatThrownBy(() -> TrackMapper.toArtistTrack(shallow, 99))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void toArtistTrackPageMapsPageMetadataAndContent() {
        ArtistTrack track = TrackMapper.toArtistTrack(shallow, 1);
        PageImpl<ArtistTrack> page = new PageImpl<>(List.of(track), PageRequest.of(2, 10), 21);

        ArtistTrackPage trackPage = TrackMapper.toArtistTrackPage(page);

        assertThat(trackPage.getPage()).isEqualTo(2);
        assertThat(trackPage.getSize()).isEqualTo(10);
        assertThat(trackPage.getTotalElements()).isEqualTo(21L);
        assertThat(trackPage.getTotalPages()).isEqualTo(3);
        assertThat(trackPage.getContent()).containsExactly(track);
    }

    /** Guards that the stored role and the API role stay in sync: a value added to one but not the other fails. */
    @ParameterizedTest
    @EnumSource(CreditRole.class)
    void everyStoredRoleHasAnApiRole(CreditRole role) {
        assertThat(TrackMapper.toCreditRole(TrackMapper.toRole(role))).isEqualTo(role);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyApiRoleHasAStoredRole(Role role) {
        assertThat(TrackMapper.toRole(TrackMapper.toCreditRole(role))).isEqualTo(role);
    }

}
