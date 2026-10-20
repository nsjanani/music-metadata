package io.music.metadata.configuration;

import io.music.metadata.it.MusicMetadataApiIT;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A UTC clock that tests can set, to move "today" forward without waiting for midnight. Shared by every test in
 * the application context.
 */
public class TestClock extends Clock {

    private volatile Instant now = Instant.now();

    public void setInstant(Instant instant) {
        this.now = instant;
    }

    public void reset() {
        this.now = Instant.now();
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("The application clock is always UTC.");
    }

}
