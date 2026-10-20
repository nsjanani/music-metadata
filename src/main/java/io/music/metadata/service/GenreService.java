package io.music.metadata.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.music.metadata.api.model.Genre;
import io.music.metadata.mapper.GenreMapper;
import io.music.metadata.repository.GenreRepository;


@Service
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;

    GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<Genre> listGenres() {
        return genreRepository.findAll(Sort.by("name")).stream().map(GenreMapper::toGenre).toList();
    }

}
