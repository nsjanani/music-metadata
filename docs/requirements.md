# Requirements and Assumptions

This is a living document. Each requirement is refined into concrete behaviour, and every assumption is stated explicitly.

## Interface

**Assumptions:**
- The task is to build the Music Metadata Service. 
- In a streaming platform, the homepage and apps are the service’s clients: for example, the homepage shows Artist of the Day by calling GET /api/v1/artists/of-the-day. 
- A separate web front end isn’t part of this solution, but every user story below can be tried in Swagger UI.
- Who can read and write
  - **Anyone can read and edit.** The service does not implement authentication or authorisation.
  - In production, the service sits behind an API gateway and identity provider that authenticate callers and authorize specific roles to perform specific actions (RBAC).

## Functional requirements

### FR-1: Edit artist name (aliases)

An artist has exactly one **current name** and zero or more **aliases**.

| Behaviour | Rule                                                                                                                                                                                  |
|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Identity | An artist is identified by an immutable `artist_id`, never by its name.                                                                                                               |
| Rename | Editing the name **adds the previous name to the artist's aliases**, then sets the new name. Both happen in one transaction.                                                          |
| Rename back to an alias | If the new name is already one of the artist's aliases, it is removed from the aliases, and the previous name is added in its place. The current name never also appears as an alias. |
| Duplicates | Alias comparison is case-insensitive and trims surrounding whitespace, so an artist never holds "Ye" and "ye" as separate aliases.                                                    |
| Uniqueness | Names and aliases are **not** globally unique. Different artists may share a name or alias.                                                                                           |
| Display | Tracks always show the artist's **current** name by using reference `artist_id`.                                      |

### FR-2: Track ↔ artist relationship

A track is credited to **one or more** artists, and an artist can be credited on many tracks (many-to-many)

| Behaviour | Rule |
|---|---|
| Roles | Each credit has a role: `PRIMARY` (main billed artist) or `FEATURED` (guest appearance). |
| Primary artist | Every track has **at least one** `PRIMARY` artist. |
| Credit order | Credits keep their order, so a track displays as "Lady Gaga & Bradley Cooper" and not the reverse. |
| No duplicates | An artist appears at most once in a track's credits. |
| Fetch artist tracks | Returns every track the artist is credited on, with the artist's role on each. |

### FR-3: Track attributes

| Field | Required | Rule |
|---|---|---|
| `title` | Yes | 1–500 characters, trimmed. |
| `durationMs` | Yes | Positive integer, in milliseconds. Formatted (for example "3:45") only for display. |
| `genreId` | Yes | Exactly one genre, chosen from a fixed reference list of genres (each with an id and a unique name). |
| `releaseDate` | No | Calendar date. |
| `explicit` | Yes | Boolean, defaults to `false`. |
| `isrc` | No | 12-character ISRC format. Unique across all tracks when present, to prevent duplicates. |

A track is identified by its own `track_id`. ISRC is not used as the identity, because it can be missing, mistyped or reissued.

### FR-4: Create artist

Artist need to exist first for adding a track to an artist (FR-2) and editing an artist's name (FR-1).

| Behaviour | Rule                                                                                                                    |
|---|-------------------------------------------------------------------------------------------------------------------------|
| Create | An artist is created with a name only (same validation as a rename). It starts with no aliases and no tracks.           |
| Identity | The system assigns the `artist_id`. Names are not unique, so creating a second artist with an existing name is allowed. |
| Seed data | A small seed dataset of artists and tracks is loaded on first start, so the endpoints have some data straight away.     |

### FR-5: Artist of the Day

Every day, one artist is featured. Artists take turns like a queue, so everyone gets a turn before anyone gets a second one.

| Behaviour | Rule |
|---|---|
| Scope | One Artist of the Day for **everyone**, worldwide. |
| Day boundary | Days run from **00:00 to 24:00 UTC**. |
| Order | Artists take turns in the order they were added (increasing `artist_id`). After the last artist, the rotation starts again from the first. |
| New artists | A newly added artist joins the **end** of the current rotation. Nobody already waiting is skipped or moved. |
| Stability | Each day's artist is recorded when it's chosen, so it never changes during that day, whatever happens to the catalogue. |
| Eligibility | Every artist is eligible, including artists with no tracks yet ("the entire catalogue"). |
| Empty catalogue | If there are no artists, there is no Artist of the Day. |

**Growth note:** the rotation serves one artist per day. If artists were added faster than one per day, indefinitely, the rotation would keep growing and never get back to the first artist. At this service's scale (editors adding artists occasionally) that doesn't happen.

**Scale note:** at real streaming-platform scale (around 11 million artists), one full cycle would take about 30,000 years, so most artists would never be featured. In production the rotation would run over an eligible or curated pool (for example verified artists with at least one track), with editorial overrides. This service implements the fair policy as stated in the task document.

## Out of scope

Not built, by decision, to keep it simple and stay within the time box:

- Deleting artists or tracks
- Adding or removing aliases directly (aliases come only from renames)
- Search, including search by alias
- Albums and releases
- Authentication and authorisation
- Playback, user accounts, playlists and recommendations
- A separate web front end 

## Non-functional requirements

These are targets for a **production deployment**.

| # | Quality | Target | Design consequence                                                                                                          |
|---|---|---|-----------------------------------------------------------------------------------------------------------------------------|
| NFR-1 | Traffic shape | **Read-heavy.** Millions of listeners read (artist tracks, Artist of the Day); a small number of catalogue editors write. | Optimise the read path (caching, read replicas); keep the write path simple.                                                |
| NFR-2 | Latency | Reads: **p99 < 200 ms**. Writes: **p99 < 1 s**. Measured at the service. | Indexed queries for every read path; cacheable responses.                                                                   |
| NFR-3 | Availability | Reads: **99.9%** (about 43 min of downtime a month). Writes: **99%** (about 7 h a month). | Reads need redundancy: 2+ app instances behind a load balancer, and a database with automatic failover. Writes can tolerate short outages. |
| NFR-4 | Consistency | Writes: **strong.** Multi-row changes (for example a rename plus its alias) are atomic. Reads: **eventual.** A change may take a few seconds to show everywhere. Artist of the Day is the same for every user on a given day. | Transactions for writes; caches and replicas allowed for reads.                                                             |
| NFR-5 | Durability | An acknowledged write is never lost. | A persistent relational database with backups; no in-memory storage of catalogue data.                                      |
| NFR-6 | Scalability | The service scales horizontally by adding instances. | Application instances are stateless; all data lives in the database.                                            |
| NFR-7 | Operability | The service can be monitored and debugged in production. | Health checks, structured logs and metrics.                                                                                 |

