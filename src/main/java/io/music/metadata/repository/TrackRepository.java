package io.music.metadata.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import io.music.metadata.repository.entity.TrackEntity;

public interface TrackRepository extends JpaRepository<TrackEntity, Long> {

    boolean existsByIsrc(String isrc);

    @EntityGraph(attributePaths = { "genre", "credits", "credits.artist" })
    List<TrackEntity> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = { "genre", "credits", "credits.artist" })
    Optional<TrackEntity> findWithDetailsById(Long id);

}
