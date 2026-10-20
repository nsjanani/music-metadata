package io.music.metadata.controller;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.music.metadata.api.ArtistsApi;
import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.api.model.ArtistTrackPage;
import io.music.metadata.api.model.CreateArtistRequest;
import io.music.metadata.api.model.RenameArtistRequest;
import io.music.metadata.service.ArtistService;
import io.music.metadata.service.TrackService;

/**
 * HTTP mapping and validation come from the generated {@link ArtistsApi}; this class only delegates.
 */
@RestController
class ArtistController implements ArtistsApi {

    private final ArtistService artistService;
    private final TrackService trackService;

    private final Clock clock;

    ArtistController(ArtistService artistService, TrackService trackService, Clock clock) {
        this.artistService = artistService;
        this.trackService = trackService;
        this.clock = clock;
    }

    @Override
    public ResponseEntity<Artist> createArtist(CreateArtistRequest request) {
        Artist artist = artistService.createArtist(request.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{artistId}")
                .buildAndExpand(artist.getArtistId())
                .toUri();
        return ResponseEntity.created(location).body(artist);
    }

    @Override
    public ResponseEntity<Artist> getArtist(Long artistId) {
        return ResponseEntity.ok(artistService.getArtist(artistId));
    }

    @Override
    public ResponseEntity<ArtistPage> listArtists(Integer page, Integer size) {
        return ResponseEntity.ok(artistService.listArtists(page, size));
    }

    /**
     * @param artistId The artist&#39;s id, as returned by &#x60;GET /artists&#x60;. (required)
     * @param request  (required) - has the version field which UI has to send for optimistic locking and prevent lost update problem
     * @return
     */
    @Override
    public ResponseEntity<Artist> renameArtist(Long artistId, RenameArtistRequest request) {
        return ResponseEntity.ok(artistService.renameArtist(artistId, request.getName(), request.getVersion()));
    }

    @Override
    public ResponseEntity<ArtistTrackPage> listArtistTracks(Long artistId, Integer page, Integer size) {
        return ResponseEntity.ok(trackService.listArtistTracks(artistId, page, size));
    }

    /**
     * Response cached for the entire UTC day, so browsers and CDNs may cache it until then: most requests never
     * reach the service.
     */
    @Override
    public ResponseEntity<ArtistOfTheDay> getArtistOfTheDay() {
        return artistService.getArtistOfTheDay()
                .map(artist -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(untilNextMidnightUtc()).cachePublic())
                        .body(artist))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private Duration untilNextMidnightUtc() {
        Instant now = clock.instant();
        Instant nextMidnight = LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return Duration.between(now, nextMidnight);
    }

}
