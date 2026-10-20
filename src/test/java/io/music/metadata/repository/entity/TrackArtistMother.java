package io.music.metadata.repository.entity;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Object Mother for {@link TrackArtistEntity}: a credit as if loaded from the database.
 */
public final class TrackArtistMother {

    private TrackArtistMother() {
    }

    public static TrackArtistEntity credit(TrackEntity track, ArtistEntity artist, CreditRole role) {
        TrackArtistEntity credit = new TrackArtistEntity(track, artist, role, (short) 1);
        ReflectionTestUtils.setField(credit, "id", new TrackArtistId(track.getId(), artist.getId()));
        return credit;
    }

}
