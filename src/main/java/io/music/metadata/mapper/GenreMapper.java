package io.music.metadata.mapper;

import io.music.metadata.api.model.Genre;
import io.music.metadata.repository.entity.GenreEntity;

/**
 * Maps genre entities into generated API models.
 */
public final class GenreMapper {

    private GenreMapper() {
    }

    public static Genre toGenre(GenreEntity entity) {
        return new Genre(entity.getId(), entity.getName());
    }

}
