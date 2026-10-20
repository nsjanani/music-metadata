package io.music.metadata.service;

import static io.music.metadata.repository.entity.GenreMother.genre;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import io.music.metadata.api.model.Genre;
import io.music.metadata.repository.GenreRepository;


@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Test
    void listsGenresOrderedByName() {
        given(genreRepository.findAll(Sort.by("name"))).willReturn(List.of(genre(1, "Blues"), genre(11, "Pop")));

        List<Genre> genres = new GenreService(genreRepository).listGenres();

        assertThat(genres).containsExactly(new Genre(1L, "Blues"), new Genre(11L, "Pop"));
    }

}
