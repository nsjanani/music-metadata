package io.music.metadata.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.music.metadata.api.TracksApi;
import io.music.metadata.api.model.CreateTrackRequest;
import io.music.metadata.api.model.Track;
import io.music.metadata.service.TrackService;

/**
 * HTTP mapping and validation come from the generated {@link TracksApi}; this class only delegates.
 */
@RestController
class TrackController implements TracksApi {

    private final TrackService trackService;

    TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @Override
    public ResponseEntity<Track> createTrack(CreateTrackRequest request) {
        Track track = trackService.createTrack(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{trackId}")
                .buildAndExpand(track.getTrackId())
                .toUri();
        return ResponseEntity.created(location).body(track);
    }

    @Override
    public ResponseEntity<Track> getTrack(Long trackId) {
        return ResponseEntity.ok(trackService.getTrack(trackId));
    }

}
