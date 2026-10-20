package io.music.metadata.repository.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "track")
public class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private GenreEntity genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false)
    private boolean explicit;

    @Column(length = 12)
    private String isrc;

    /** Credits in credit order */
    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<TrackArtistEntity> credits = new ArrayList<>();

    protected TrackEntity() {
    }

    public TrackEntity(String title, int durationMs, GenreEntity genre, LocalDate releaseDate, boolean explicit,
            String isrc) {
        this.title = title;
        this.durationMs = durationMs;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.explicit = explicit;
        this.isrc = isrc;
    }

    /** Adds the next credit; its position follows the credits already added. */
    public void addCredit(ArtistEntity artist, CreditRole role) {
        credits.add(new TrackArtistEntity(this, artist, role, (short) (credits.size() + 1)));
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public GenreEntity getGenre() {
        return genre;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public String getIsrc() {
        return isrc;
    }

    public List<TrackArtistEntity> getCredits() {
        return credits;
    }

}
