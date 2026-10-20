--liquibase formatted sql
--changeset music-metadata:002-insert-genres
INSERT INTO genre (name) VALUES
    ('Blues'),
    ('Classical'),
    ('Country'),
    ('Electronic'),
    ('Folk'),
    ('Hip-Hop'),
    ('Indie'),
    ('Jazz'),
    ('Latin'),
    ('Metal'),
    ('Pop'),
    ('R&B'),
    ('Reggae'),
    ('Rock'),
    ('Soul');
