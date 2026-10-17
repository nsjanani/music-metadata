# API Endpoints

All paths are relative to `/api/v1`.

| Feature | Method and path | Success | Requirement |
|---|---|---|---|
| Create artist | `POST /artists` | `201 Created` + `Location` header | FR-4 |
| Get artist | `GET /artists/{artistId}` | `200 OK` | FR-1 (shows name and aliases) |
| List artists | `GET /artists?page=&size=` | `200 OK` | Lets the UI pick artists for a track. A plain list, not search. |
| Rename artist | `PATCH /artists/{artistId}` | `200 OK` | FR-1 |
| Add track | `POST /tracks` | `201 Created` + `Location` header | FR-2, FR-3 |
| Fetch artist tracks | `GET /artists/{artistId}/tracks?page=&size=` | `200 OK` | FR-2 |
| Artist of the Day | `GET /artist-of-the-day` | `200 OK`, or `204 No Content` if there are no artists | FR-6 |
| List genres | `GET /genres` | `200 OK` | FR-3 (genre dropdown) |
