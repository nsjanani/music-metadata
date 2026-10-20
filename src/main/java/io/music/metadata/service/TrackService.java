package io.music.metadata.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.api.model.CreateTrackRequest;
import io.music.metadata.api.model.Role;
import io.music.metadata.api.model.Track;
import io.music.metadata.api.model.TrackCreditRequest;
import io.music.metadata.exception.ConflictException;
import io.music.metadata.exception.InvalidFieldException;
import io.music.metadata.exception.NotFoundException;
import io.music.metadata.mapper.TrackMapper;
import io.music.metadata.repository.ArtistRepository;
import io.music.metadata.repository.GenreRepository;
import io.music.metadata.repository.TrackArtistRepository;
import io.music.metadata.repository.TrackRepository;
import io.music.metadata.repository.entity.ArtistEntity;
import io.music.metadata.repository.entity.GenreEntity;
import io.music.metadata.repository.entity.TrackEntity;


@Service
@Transactional(readOnly = true)
public class TrackService {

    private final TrackRepository trackRepository;

    private final TrackArtistRepository trackArtistRepository;

    private final ArtistRepository artistRepository;

    private final GenreRepository genreRepository;

    TrackService(TrackRepository trackRepository, TrackArtistRepository trackArtistRepository,
            ArtistRepository artistRepository, GenreRepository genreRepository) {
        this.trackRepository = trackRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * Adds a track credited to one or more artists. The order of the credits in the request is the
     * credit order.
     */
    @Transactional
    public Track createTrack(CreateTrackRequest request) {
        validateTrackRequest(request);
        List<TrackCreditRequest> credits = request.getArtists();

        // Extra checks for the backend, as even the public facing UI can be tampered
        GenreEntity genre = genreRepository.findById(request.getGenreId())
                .orElseThrow(() -> new InvalidFieldException("genreId", "unknown genre " + request.getGenreId()));
        Map<Long, ArtistEntity> artists = findCreditedArtists(credits);

        if (request.getIsrc() != null && trackRepository.existsByIsrc(request.getIsrc())) {
            throw new ConflictException("A track with ISRC " + request.getIsrc() + " already exists.");
        }

        TrackEntity track = new TrackEntity(request.getTitle().strip(), request.getDurationMs(), genre,
                request.getReleaseDate(), Boolean.TRUE.equals(request.getExplicit()), request.getIsrc());
        credits.forEach(credit -> track.addCredit(artists.get(credit.getArtistId()),
                TrackMapper.toCreditRole(credit.getRole())));
        return TrackMapper.toTrack(trackRepository.save(track));
    }

    public Track getTrack(long trackId) {
        return trackRepository.findWithDetailsById(trackId)
                .map(TrackMapper::toTrack)
                .orElseThrow(() -> new NotFoundException("Track", trackId));
    }

    /** A page of every track the artist is credited on, ordered by track id, with the artist's role on each. */
    public ArtistTrackPage listArtistTracks(long artistId, int page, int size) {
        if (!artistRepository.existsById(artistId)) {
            throw new NotFoundException("Artist", artistId);
        }
        // Two queries, to avoid both N+1 queries and paginating a collection fetch: a page of the artist's credits,
        // then those tracks with their genre and credits.
        Page<Long> trackIds = trackArtistRepository
                .findByIdArtistId(artistId, PageRequest.of(page, size, Sort.by("id.trackId")))
                .map(credit -> credit.getId().getTrackId());
        Map<Long, TrackEntity> tracksById = trackRepository.findByIdIn(trackIds.getContent()).stream()
                .collect(Collectors.toMap(TrackEntity::getId, Function.identity()));
        return TrackMapper.toArtistTrackPage(
                trackIds.map(trackId -> TrackMapper.toArtistTrack(tracksById.get(trackId), artistId)));
    }

    private static void validateTrackRequest(CreateTrackRequest request) {
        if (request.getTitle().isBlank()) {
            throw new InvalidFieldException("title", "must not be blank");
        }
        List<TrackCreditRequest> credits = request.getArtists();
        long distinctArtists = credits.stream().map(TrackCreditRequest::getArtistId).distinct().count();
        if (distinctArtists < credits.size()) {
            throw new InvalidFieldException("artists", "an artist can be credited only once per track");
        }
        if (credits.stream().noneMatch(credit -> credit.getRole() == Role.PRIMARY)) {
            throw new InvalidFieldException("artists", "must include at least one PRIMARY artist");
        }
    }

    private Map<Long, ArtistEntity> findCreditedArtists(List<TrackCreditRequest> credits) {
        List<Long> artistIds = credits.stream().map(TrackCreditRequest::getArtistId).toList();
        Map<Long, ArtistEntity> artists = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(ArtistEntity::getId, Function.identity()));
        List<Long> unknown = artistIds.stream().filter(id -> !artists.containsKey(id)).toList();
        if (!unknown.isEmpty()) {
            throw new InvalidFieldException("artists", "unknown artist ids " + unknown);
        }
        return artists;
    }

}
