package io.music.metadata.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.music.metadata.repository.entity.TrackArtistEntity;
import io.music.metadata.repository.entity.TrackArtistId;

/** Credits: the rows of {@code track_artist} linking tracks and artists. */
public interface TrackArtistRepository extends JpaRepository<TrackArtistEntity, TrackArtistId> {

    /**
     * A page of the artist's credits, served by the (artist_id, track_id) index. The track and artist are loaded
     * lazily, so this reads only the credit rows. Sort by {@code id.trackId} for a stable order.
     */
    Page<TrackArtistEntity> findByIdArtistId(long artistId, Pageable pageable);

}
