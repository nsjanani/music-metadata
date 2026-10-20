package io.music.metadata.repository.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite primary key of {@code track_artist}: an artist appears at most once per track. */
@Embeddable
public class TrackArtistId implements Serializable {

    @Column(name = "track_id")
    private Long trackId;

    @Column(name = "artist_id")
    private Long artistId;

    protected TrackArtistId() {
    }

    TrackArtistId(Long trackId, Long artistId) {
        this.trackId = trackId;
        this.artistId = artistId;
    }

    public Long getTrackId() {
        return trackId;
    }

    public Long getArtistId() {
        return artistId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TrackArtistId that
                && Objects.equals(trackId, that.trackId)
                && Objects.equals(artistId, that.artistId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackId, artistId);
    }

}
