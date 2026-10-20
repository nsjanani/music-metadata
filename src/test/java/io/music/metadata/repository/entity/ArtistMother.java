package io.music.metadata.repository.entity;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/**
 * Object Mother for {@link ArtistEntity}: artists as if loaded from the database.
 */
public final class ArtistMother {

    private ArtistMother() {
    }


    public static ArtistEntity artist(long id, String name) {
        ArtistEntity artist = new ArtistEntity(name);
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }

    public static ArtistEntity artist(long id, String name, int version) {
        ArtistEntity artist = artist(id, name);
        ReflectionTestUtils.setField(artist, "version", version);
        return artist;
    }

    public static ArtistOfTheDayEntity artistOfTheDay(LocalDate day, ArtistEntity artist) {
        ArtistOfTheDayEntity pick = BeanUtils.instantiateClass(ArtistOfTheDayEntity.class);
        ReflectionTestUtils.setField(pick, "day", day);
        ReflectionTestUtils.setField(pick, "artist", artist);
        return pick;
    }

}
