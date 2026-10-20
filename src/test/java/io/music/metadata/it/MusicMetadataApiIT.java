package io.music.metadata.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.music.metadata.configuration.TestClock;
import io.music.metadata.configuration.TestClockConfiguration;
import io.music.metadata.configuration.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

/**
 * Base class for integration tests: the full application against a real MySQL (Testcontainers), called through
 * HTTP with {@link MockMvcTester}. All subclasses share one application context and one database container.
 * <p>
 * Tests always run with only the "test" profile, which adds no configuration of its own: they test the base
 * (production) configuration, whatever profile an IDE or shell may have set.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, TestClockConfiguration.class })
public abstract class MusicMetadataApiIT {

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestClock clock;

    /** Every test starts from an empty catalogue (reference data such as genres is kept) and the real time. */
    @BeforeEach
    void resetState() {
        clock.reset();
        jdbcTemplate.update("DELETE FROM artist_of_the_day");
        jdbcTemplate.update("DELETE FROM track_artist");
        jdbcTemplate.update("DELETE FROM track");
        jdbcTemplate.update("DELETE FROM artist_alias");
        jdbcTemplate.update("DELETE FROM artist");
    }

    protected long createArtist(String name) {
        MvcTestResult result = post("/api/v1/artists", "{\"name\": \"" + name + "\"}");
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return idOf(result, "$.artistId");
    }

    protected MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    protected MvcTestResult patch(String uri, String json) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    /** Asserts a 400 Problem Details response that lists {@code field} among the invalid fields. */
    protected static void assertValidationError(MvcTestResult result, String field) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors[*].field").asArray().contains(field);
    }

    protected static long idOf(MvcTestResult result, String path) {
        try {
            Number id = JsonPath.read(result.getResponse().getContentAsString(), path);
            return id.longValue();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Could not read " + path + " from the response", ex);
        }
    }

}
