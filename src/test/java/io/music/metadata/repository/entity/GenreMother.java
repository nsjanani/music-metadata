package io.music.metadata.repository.entity;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Object Mother for {@link GenreEntity}: genres as if loaded from the database.
 */
public final class GenreMother {

    private GenreMother() {
    }

    public static GenreEntity genre(long id, String name) {
        GenreEntity genre = BeanUtils.instantiateClass(GenreEntity.class);
        ReflectionTestUtils.setField(genre, "id", id);
        ReflectionTestUtils.setField(genre, "name", name);
        return genre;
    }

}
