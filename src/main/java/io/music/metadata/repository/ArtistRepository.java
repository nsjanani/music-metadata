package io.music.metadata.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import io.music.metadata.repository.entity.ArtistEntity;

public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {

    @EntityGraph(attributePaths = "aliases")
    Optional<ArtistEntity> findWithAliasesById(Long id);

}
