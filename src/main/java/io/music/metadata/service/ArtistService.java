package io.music.metadata.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.mapper.ArtistMapper;
import io.music.metadata.repository.ArtistOfTheDayRepository;
import io.music.metadata.repository.ArtistRepository;
import io.music.metadata.repository.entity.ArtistEntity;
import io.music.metadata.repository.entity.ArtistOfTheDayEntity;


@Service
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistOfTheDayRepository artistOfTheDayRepository;

    private final Clock clock;

    ArtistService(ArtistRepository artistRepository, ArtistOfTheDayRepository artistOfTheDayRepository, Clock clock) {
        this.artistRepository = artistRepository;
        this.artistOfTheDayRepository = artistOfTheDayRepository;
        this.clock = clock;
    }

    @Transactional
    public Artist createArtist(String name) {
        ArtistEntity artist = artistRepository.save(new ArtistEntity(normalizeName(name)));
        return ArtistMapper.toArtist(artist);
    }

    public Artist getArtist(long artistId) {
        return ArtistMapper.toArtist(findWithAliases(artistId));
    }

    public ArtistPage listArtists(int page, int size) {
        return ArtistMapper.toPage(artistRepository.findAll(PageRequest.of(page, size, Sort.by("id"))));
    }

    /**
     * Renames an artist, keeping the previous name as an alias.
     * {@code expectedVersion} is the version the client last read: if the artist has changed since, the rename is
     * rejected instead of silently overwriting the other change.
     */
    @Transactional
    public Artist renameArtist(long artistId, String newName, int expectedVersion) {
        ArtistEntity artist = findWithAliases(artistId);
        if (artist.getVersion() != expectedVersion) {
            throw new ConflictException("Artist " + artistId + " has changed since version " + expectedVersion
                    + " (current version " + artist.getVersion() + "). Reload it and try again.");
        }
        artist.rename(normalizeName(newName));
        artistRepository.flush();
        return ArtistMapper.toArtist(artist);
    }

    /**
     * Artist of the Day, or empty if there are no artists.
     * Lazily triggered by the first request of each UTC day.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<ArtistOfTheDay> getArtistOfTheDay() {
        LocalDate today = LocalDate.now(clock);
        Optional<ArtistOfTheDayEntity> pick = artistOfTheDayRepository.findWithArtistByDay(today);
        if (pick.isEmpty()) {
            artistOfTheDayRepository.recordNextArtist(today);
            pick = artistOfTheDayRepository.findWithArtistByDay(today);
        }
        return pick.map(ArtistMapper::toArtistOfTheDay);
    }

    private ArtistEntity findWithAliases(long artistId) {
        return artistRepository.findWithAliasesById(artistId)
                .orElseThrow(() -> new NotFoundException("Artist", artistId));
    }

    private static String normalizeName(String name) {
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidFieldException("name", "must not be blank");
        }
        return trimmed;
    }

}
