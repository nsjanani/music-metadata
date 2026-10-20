package io.music.metadata.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class GenresApiIT extends MusicMetadataApiIT {

    @Nested
    class ListGenres {

        @Test
        void returnsAllGenresOrderedByName() {
            MvcTestResult result = mvc.get().uri("/api/v1/genres").exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(15);
            assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("Blues");
            assertThat(result).bodyJson().extractingPath("$[14].name").isEqualTo("Soul");
        }

    }

}
