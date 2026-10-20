package io.music.metadata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.music.metadata.api.model.Genre;
import io.music.metadata.service.GenreService;

@WebMvcTest(GenreController.class)
class GenreControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private GenreService genreService;

    @Test
    void returnsTheGenres() {
        given(genreService.listGenres()).willReturn(List.of(new Genre(1L, "Blues"), new Genre(2L, "Classical")));

        MvcTestResult result = mvc.get().uri("/api/v1/genres").exchange();

        assertThat(result)
                .hasStatusOk()
                .hasHeader("Cache-Control", "no-cache")
                .bodyJson().isLenientlyEqualTo("""
                [{"genreId": 1, "name": "Blues"}, {"genreId": 2, "name": "Classical"}]""");
    }

    @Test
    void returnsAnEmptyList() {
        given(genreService.listGenres()).willReturn(List.of());

        assertThat(mvc.get().uri("/api/v1/genres"))
                .hasStatusOk()
                .bodyJson().extractingPath("$").asArray().isEmpty();
    }

}
