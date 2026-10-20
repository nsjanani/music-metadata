package io.music.metadata.mapper;

import org.springframework.data.domain.Page;

import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistAlias;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.api.model.ArtistSummary;
import io.music.metadata.repository.entity.ArtistAliasEntity;
import io.music.metadata.repository.entity.ArtistEntity;
import io.music.metadata.repository.entity.ArtistOfTheDayEntity;

/**
 * Maps artist entities into generated API models.
 */
public final class ArtistMapper {

    private ArtistMapper() {
    }

    public static Artist toArtist(ArtistEntity entity) {
        return new Artist(
                entity.getId(),
                entity.getName(),
                entity.getAliases().stream().map(ArtistMapper::toAlias).toList(),
                entity.getVersion());
    }

    public static ArtistAlias toAlias(ArtistAliasEntity entity) {
        return new ArtistAlias(entity.getAlias());
    }

    public static ArtistSummary toSummary(ArtistEntity entity) {
        return new ArtistSummary(entity.getId(), entity.getName());
    }

    public static ArtistPage toPage(Page<ArtistEntity> page) {
        return new ArtistPage(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent().stream().map(ArtistMapper::toSummary).toList());
    }

    public static ArtistOfTheDay toArtistOfTheDay(ArtistOfTheDayEntity entity) {
        return new ArtistOfTheDay(entity.getDay(), toSummary(entity.getArtist()));
    }

}
