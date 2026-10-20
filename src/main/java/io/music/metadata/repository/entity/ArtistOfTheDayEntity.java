package io.music.metadata.repository.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One row per UTC day: the artist featured that day (FR-6). Rows are only written by
 * {@code ArtistOfTheDayRepository#recordNextArtist}, never through this entity.
 */
@Entity
@Table(name = "artist_of_the_day")
public class ArtistOfTheDayEntity {

    @Id
    private LocalDate day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private ArtistEntity artist;

    protected ArtistOfTheDayEntity() {
    }

    public LocalDate getDay() {
        return day;
    }

    public ArtistEntity getArtist() {
        return artist;
    }

}
