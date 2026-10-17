# Music Metadata

A service that stores and serves metadata about artists and tracks for a music streaming platform, with a web UI on top.

## Features

- **Add a track** to an artist's catalogue (title, genre, length, …)
- **Edit an artist's name**, keeping previous names as aliases
- **Fetch an artist's tracks**
- **Artist of the Day**: a fair, cyclical rotation through the whole artist catalogue

## Repository layout

```
music-metadata/
├── backend/    # music-metadata-api: JVM service exposing the REST API
└── frontend/   # music-metadata-web: web UI, talks to the backend only through the API contract
```

`backend/` and `frontend/` share no code. Their only coupling is the API contract, so each can be built, deployed and scaled on its own.

## Getting started

_To be added once the backend and frontend are in place._
