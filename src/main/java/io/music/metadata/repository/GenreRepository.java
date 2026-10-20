package io.music.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.music.metadata.repository.entity.GenreEntity;

public interface GenreRepository extends JpaRepository<GenreEntity, Long> {

}
