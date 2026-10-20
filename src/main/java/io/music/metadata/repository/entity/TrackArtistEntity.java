package io.music.metadata.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** One credit: an artist's role and position on a track (the many-to-many link, FR-2). */
@Entity
@Table(name = "track_artist")
public class TrackArtistEntity {

    @EmbeddedId
    private TrackArtistId id = new TrackArtistId();

    @MapsId("trackId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id")
    private TrackEntity track;

    @MapsId("artistId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private ArtistEntity artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditRole role;

    @Column(nullable = false)
    private short position;

    protected TrackArtistEntity() {
    }

    TrackArtistEntity(TrackEntity track, ArtistEntity artist, CreditRole role, short position) {
        this.track = track;
        this.artist = artist;
        this.role = role;
        this.position = position;
    }

    public TrackArtistId getId() {
        return id;
    }

    public TrackEntity getTrack() {
        return track;
    }

    public ArtistEntity getArtist() {
        return artist;
    }

    public CreditRole getRole() {
        return role;
    }

    public short getPosition() {
        return position;
    }

}
