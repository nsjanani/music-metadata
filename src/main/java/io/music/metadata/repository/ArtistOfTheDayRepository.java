package io.music.metadata.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.music.metadata.repository.entity.ArtistOfTheDayEntity;

public interface ArtistOfTheDayRepository extends JpaRepository<ArtistOfTheDayEntity, LocalDate> {

    @EntityGraph(attributePaths = "artist")
    Optional<ArtistOfTheDayEntity> findWithArtistByDay(LocalDate day);

    /**
     * Records the artist for {@code day} in one atomic statement: the artist after the most recently featured one,
     * in {@code artist.id} order, wrapping to the first artist after the last.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO artist_of_the_day (day, artist_id)
            SELECT :day,
                   COALESCE(
                       (SELECT MIN(a.id) FROM artist a
                         WHERE a.id > (SELECT p.artist_id FROM artist_of_the_day p
                                        WHERE p.day < :day
                                        ORDER BY p.day DESC LIMIT 1)),
                       (SELECT MIN(a.id) FROM artist a))
            FROM DUAL
            WHERE EXISTS (SELECT 1 FROM artist)
            ON DUPLICATE KEY UPDATE day = day""")
    int recordNextArtist(@Param("day") LocalDate day);

}
