package io.music.metadata.mapper;

import static io.music.metadata.repository.entity.GenreMother.genre;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.music.metadata.api.model.Genre;

class GenreMapperTest {

    @Test
    void toGenreMapsIdAndName() {
        assertThat(GenreMapper.toGenre(genre(6, "Hip-Hop"))).isEqualTo(new Genre(6L, "Hip-Hop"));
    }

}
