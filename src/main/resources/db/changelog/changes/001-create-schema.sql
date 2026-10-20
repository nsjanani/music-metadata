--liquibase formatted sql
--changeset music-metadata:001-create-artist
CREATE TABLE artist (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(500) NOT NULL,
    version    INT          NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_artist PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;

--changeset music-metadata:001-create-artist-alias
CREATE TABLE artist_alias (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    artist_id BIGINT       NOT NULL,
    alias     VARCHAR(500) NOT NULL,
    CONSTRAINT pk_artist_alias PRIMARY KEY (id),
    CONSTRAINT fk_artist_alias_artist FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT uk_artist_alias_artist_alias UNIQUE (artist_id, alias)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;

--changeset music-metadata:001-create-genre
CREATE TABLE genre (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_genre PRIMARY KEY (id),
    CONSTRAINT uk_genre_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;

--changeset music-metadata:001-create-track
CREATE TABLE track (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(500) NOT NULL,
    duration_ms  INT          NOT NULL,
    genre_id     BIGINT       NOT NULL,
    release_date DATE         NULL,
    explicit     BOOLEAN      NOT NULL DEFAULT FALSE,
    isrc         CHAR(12)     NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_track PRIMARY KEY (id),
    CONSTRAINT fk_track_genre FOREIGN KEY (genre_id) REFERENCES genre (id),
    CONSTRAINT uk_track_isrc UNIQUE (isrc),
    CONSTRAINT ck_track_duration_positive CHECK (duration_ms > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;

--changeset music-metadata:001-create-track-artist
CREATE TABLE track_artist (
    track_id  BIGINT      NOT NULL,
    artist_id BIGINT      NOT NULL,
    role      VARCHAR(20) NOT NULL,
    position  SMALLINT    NOT NULL,
    CONSTRAINT pk_track_artist PRIMARY KEY (track_id, artist_id),
    CONSTRAINT fk_track_artist_track FOREIGN KEY (track_id) REFERENCES track (id),
    CONSTRAINT fk_track_artist_artist FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT uk_track_artist_position UNIQUE (track_id, position),
    CONSTRAINT ck_track_artist_role CHECK (role IN ('PRIMARY', 'FEATURED')),
    CONSTRAINT ck_track_artist_position_positive CHECK (position > 0),
    INDEX ix_track_artist_artist_track (artist_id, track_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;

--changeset music-metadata:001-create-artist-of-the-day
CREATE TABLE artist_of_the_day (
    day       DATE   NOT NULL,
    artist_id BIGINT NOT NULL,
    CONSTRAINT pk_artist_of_the_day PRIMARY KEY (day),
    CONSTRAINT fk_artist_of_the_day_artist FOREIGN KEY (artist_id) REFERENCES artist (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_as_ci;
