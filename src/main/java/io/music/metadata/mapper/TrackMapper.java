package io.music.metadata.mapper;

import java.util.List;

import org.springframework.data.domain.Page;

import io.music.metadata.api.model.ArtistTrack;
import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.api.model.Role;
import io.music.metadata.api.model.Track;
import io.music.metadata.api.model.TrackCredit;
import io.music.metadata.repository.entity.CreditRole;
import io.music.metadata.repository.entity.TrackArtistEntity;
import io.music.metadata.repository.entity.TrackEntity;

/**
 * Maps track entities into generated API models.
 */
public final class TrackMapper {

    private TrackMapper() {
    }

    public static Track toTrack(TrackEntity entity) {
        return new Track(
                entity.getId(),
                entity.getTitle(),
                entity.getDurationMs(),
                GenreMapper.toGenre(entity.getGenre()),
                entity.isExplicit(),
                toCredits(entity))
                .releaseDate(entity.getReleaseDate())
                .isrc(entity.getIsrc());
    }

    /** A track as seen from one artist: includes that artist's role on the track. */
    public static ArtistTrack toArtistTrack(TrackEntity entity, long artistId) {
        return new ArtistTrack(
                entity.getId(),
                entity.getTitle(),
                entity.getDurationMs(),
                GenreMapper.toGenre(entity.getGenre()),
                entity.isExplicit(),
                toCredits(entity),
                roleOf(entity, artistId))
                .releaseDate(entity.getReleaseDate())
                .isrc(entity.getIsrc());
    }

    public static ArtistTrackPage toArtistTrackPage(Page<ArtistTrack> page) {
        return new ArtistTrackPage(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent());
    }

    public static Role toRole(CreditRole role) {
        return Role.valueOf(role.name());
    }

    public static CreditRole toCreditRole(Role role) {
        return CreditRole.valueOf(role.name());
    }

    private static List<TrackCredit> toCredits(TrackEntity entity) {
        return entity.getCredits().stream()
                .map(credit -> new TrackCredit(credit.getArtist().getId(), credit.getArtist().getName(),
                        toRole(credit.getRole())))
                .toList();
    }

    private static Role roleOf(TrackEntity entity, long artistId) {
        return entity.getCredits().stream()
                .filter(credit -> credit.getArtist().getId() == artistId)
                .map(TrackArtistEntity::getRole)
                .map(TrackMapper::toRole)
                .findFirst()
                .orElseThrow();
    }

}
