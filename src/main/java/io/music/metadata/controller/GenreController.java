package io.music.metadata.controller;

import io.music.metadata.api.GenresApi;
import io.music.metadata.api.model.Genre;
import io.music.metadata.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP mapping and validation come from the generated {@link GenresApi}; this class only delegates.
 */
@RestController
class GenreController implements GenresApi {

    private final GenreService genreService;

    GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @Override
    public ResponseEntity<List<Genre>> listGenres() {
        return ResponseEntity.ok(genreService.listGenres());
    }

}
