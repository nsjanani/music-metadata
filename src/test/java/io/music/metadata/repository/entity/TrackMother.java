package io.music.metadata.repository.entity;

import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Object Mother for {@link TrackEntity}: tracks as if loaded from the database.
 */
public final class TrackMother {

    private TrackMother() {
    }

    public static TrackEntity track(long id, String title, int durationMs, GenreEntity genre, LocalDate releaseDate,
            boolean explicit, String isrc) {
        TrackEntity track = new TrackEntity(title, durationMs, genre, releaseDate, explicit, isrc);
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }

}
