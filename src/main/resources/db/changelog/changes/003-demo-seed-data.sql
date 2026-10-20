--liquibase formatted sql

-- Demo data
-- Only applied with the "demo" Liquibase context; never in production.

--changeset music-metadata:003-demo-artists
INSERT INTO artist (name) VALUES
    ('Nova Lane'),
    ('The Midnight Owls'),
    ('Kairo'),
    ('Ama Sol'),
    ('Echo Harbor'),
    ('Luna Vega'),
    ('Static Bloom'),
    ('Marlowe Grey');

--changeset music-metadata:003-demo-artist-aliases
INSERT INTO artist_alias (artist_id, alias)
SELECT id, 'DJ Kairo' FROM artist WHERE name = 'Kairo';

--changeset music-metadata:003-demo-tracks
INSERT INTO track (title, duration_ms, genre_id, release_date, explicit)
SELECT t.title, t.duration_ms, g.id, t.release_date, t.explicit
FROM (
    SELECT 'Paper Satellites' AS title, 214000 AS duration_ms, 'Pop'        AS genre, DATE '2024-03-15' AS release_date, FALSE AS explicit
    UNION ALL SELECT 'Glass Horizon',     198000, 'Pop',        DATE '2024-03-15', FALSE
    UNION ALL SELECT 'Neon Moths',        245000, 'Indie',      DATE '2023-09-01', FALSE
    UNION ALL SELECT 'Last Train North',  262000, 'Rock',       DATE '2022-11-11', FALSE
    UNION ALL SELECT 'Low Frequency',     187000, 'Electronic', DATE '2025-01-20', TRUE
    UNION ALL SELECT 'Afterglow Theory',  301000, 'Electronic', NULL,              FALSE
    UNION ALL SELECT 'Golden Hour Talk',  229000, 'R&B',        DATE '2024-07-04', FALSE
    UNION ALL SELECT 'Tidewater',         276000, 'Folk',       DATE '2021-05-30', FALSE
    UNION ALL SELECT 'Two Lanterns',      233000, 'Pop',        DATE '2025-06-12', FALSE
    UNION ALL SELECT 'Velvet Static',     205000, 'Hip-Hop',    DATE '2025-02-28', TRUE
) AS t
JOIN genre g ON g.name = t.genre;

--changeset music-metadata:003-demo-track-artists
INSERT INTO track_artist (track_id, artist_id, role, position)
SELECT tr.id, a.id, c.role, c.position
FROM (
    SELECT 'Paper Satellites' AS title, 'Nova Lane' AS artist, 'PRIMARY' AS role, 1 AS position
    UNION ALL SELECT 'Glass Horizon',    'Nova Lane',         'PRIMARY',  1
    UNION ALL SELECT 'Neon Moths',       'The Midnight Owls', 'PRIMARY',  1
    UNION ALL SELECT 'Last Train North', 'The Midnight Owls', 'PRIMARY',  1
    UNION ALL SELECT 'Low Frequency',    'Kairo',             'PRIMARY',  1
    UNION ALL SELECT 'Afterglow Theory', 'Kairo',             'PRIMARY',  1
    UNION ALL SELECT 'Afterglow Theory', 'Luna Vega',         'FEATURED', 2
    UNION ALL SELECT 'Golden Hour Talk', 'Ama Sol',           'PRIMARY',  1
    UNION ALL SELECT 'Tidewater',        'Echo Harbor',       'PRIMARY',  1
    UNION ALL SELECT 'Two Lanterns',     'Luna Vega',         'PRIMARY',  1
    UNION ALL SELECT 'Two Lanterns',     'Nova Lane',         'PRIMARY',  2
    UNION ALL SELECT 'Velvet Static',    'Static Bloom',      'PRIMARY',  1
    UNION ALL SELECT 'Velvet Static',    'Kairo',             'FEATURED', 2
) AS c
JOIN track tr ON tr.title = c.title
JOIN artist a ON a.name = c.artist;
