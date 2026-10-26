[![Upgraded with Claude](https://img.shields.io/badge/Upgraded%20with-Claude-D97757?logo=anthropic&logoColor=white)](https://claude.com/claude-code)

# CountMeUp

A small live-voting web application, originally written in 2017 as a technical challenge for the BBC London
Assessment Centre (the text of the challenge is in `Technical Challenge - BBC London Assessment Centre.doc`).

* **Admin** opens an event with a name, a description and at least two candidates (optionally with an initial number of votes).
* **Voters** pick a candidate. Every voter can cast at most **3 votes** per event.
* **Presenter** shows the running totals as a doughnut, column or pie chart plus a table, updated live over a WebSocket.

> **Upgraded after 9 years.** The code base had not been touched since 2017 (Play 2.5, Scala 2.11, sbt 0.13, Akka
> actors, jQuery 1.11, Bootstrap 3, an H2 1.4 jar committed to the repository). It has been upgraded and rewritten with
> [Claude Code](https://claude.com/claude-code); [What changed](#what-changed-in-the-2026-upgrade) explains how and why.

## Quick start

Requirements: JDK 17, 21 or 25 and [sbt](https://www.scala-sbt.org/) 1.12 (the version is pinned in `project/build.properties`).

```
sbt run     # http://localhost:9000  (the database schema is created automatically)
sbt test    # unit, HTTP and end-to-end (WebSocket) tests
```

| Page | URL |
|------|-----|
| Results (default) | http://localhost:9000/presenter |
| Vote | http://localhost:9000/voter |
| Create an event | http://localhost:9000/admin |

Start by creating an event in the admin page, then open the voter and presenter pages.

### Configuration

| Setting | Default | Notes |
|---------|---------|-------|
| `APPLICATION_SECRET` | dev-mode generated | **Required** (long random value) when running in production |
| `DB_URL` | `jdbc:h2:file:./data/countmeup` | Any JDBC URL for a driver on the classpath |
| `database.dispatcher` | 10 threads | Thread pool for blocking JDBC calls, sized like the Hikari pool |

## REST API

All bodies are JSON; errors look like `{"error": "..."}`.

| Method & path | Purpose | Success | Failures |
|---------------|---------|---------|----------|
| `GET /presenter/event`, `/voter/event`, `/admin/event` | Open event with its candidates | 200 | 404 no event open |
| `GET /presenter/result` | `[{"name": "A", "votes": 3}, ...]` | 200 | 404 no event open |
| `GET /presenter/websocket` | Pushes `{"event": {...} or null, "results": [...]}` every 5 s | 101 | |
| `POST /admin/event` | `{"name", "description", "candidates": [{"name", "initialVotes"?}]}` | 201 + event | 400 validation |
| `POST /voter/vote` | `{"voter", "candidateId", "eventId"}` | 201 | 400 invalid, 404 unknown candidate / no event, 409 stale event or vote limit reached |

## Architecture

```
controllers/   Admin, Voter, Presenter (+ ApiSupport: domain errors -> HTTP statuses)
model/         case classes, validation rules, VotingError, JSON formats
repository/    VotingRepository (trait) and JdbcVotingRepository, DatabaseExecutionContext
views/         Twirl templates (one layout + three pages)
public/        vanilla ES-module JavaScript, vendored Chart.js and Bootstrap
```

Controllers are thin: they parse and validate the request, call the repository and map the result. The repository is the only
code touching the database; its blocking JDBC calls run on a dedicated thread pool and return `Future`s.

Database tables (see `conf/evolutions/default/1.sql`): `EVENTS` (one is *current*), `CANDIDATES`, `VOTES` (every ballot,
used to enforce the limit and for auditing) and `CACHE` (denormalised running totals so reading results never scans
`VOTES`). Opening an event never deletes older ones; their totals can still be read by id.

## What changed in the 2026 upgrade

### Dependencies

| | 2017 | 2026 |
|---|---|---|
| Language | Scala 2.11.8 | Scala 3.3 LTS (3.3.9) |
| Framework | Play 2.5.4 | Play 3.0.12 (Pekko instead of Akka, `jakarta.inject`) |
| Build tool | sbt 0.13.8 | sbt 1.12.15 |
| sbt plugins | 7 (CoffeeScript, Less, JSHint, RJS, Digest, Mocha, Play) | Play + Digest only; the rest had nothing to process |
| Database | H2 1.4.194 (jar committed) | H2 2.5 from Maven |
| Tests | specs2 + JUnit runner + Selenium/HtmlUnit | ScalaTest + scalatestplus-play 7.0.2 |
| Front end | jQuery 1.11, Bootstrap 3, Alpaca, Handlebars, CanvasJS (over plain http) | Vanilla JS, Bootstrap 5.3.8, Chart.js 4.6.1, all vendored in `public/vendor` |

### Code

* **Actors removed.** The 2017 README admitted the actors were added, then regretted. Each request went through an actor
  only to call a blocking DAO. Controllers now call a repository returning `Future`s on a dedicated dispatcher. That also removes
  the `Await.result(..., Duration.Inf)` in the admin controller and the `DIModule` that read `application.conf` as a `Properties` file.
* **One repository instead of an inheritance trick.** `DbAccess` had `protected` methods re-exposed by three subclasses.
  It is now one `VotingRepository` trait with an implementation Guice binds via `@ImplementedBy`.
* **Resource safety.** JDBC statements and result sets are always closed (`Using`); the old code leaked them.
* **Atomic operations.** Creating an event and its candidates is one transaction (previously a parallel fan-out of candidate inserts).
  Casting a vote counts, inserts and updates the totals in one transaction under a lock, so concurrent votes from one
  voter can no longer slip past the limit of 3 (covered by a test with 20 concurrent votes).
* **Votes are acknowledged.** `POST /voter/vote` used to fire the vote at an actor and answer "Vote added" regardless of
  the outcome, and ignored `eventid`. It now reports the real result (404/409) and rejects votes for an event that is no longer open.
* **Validation moved to the model** (`NewEvent.validate`): blank names, fewer than 2 candidates, duplicates, negative votes.
* **Modern Scala 3 idioms:** indentation syntax, `given`/`using`, sealed error hierarchy, `Json.reads/writes` macros instead of
  functional-syntax combinators, `Option.when`, `Either` for validation.
* **Dead code removed:** `Application` controller, the Play welcome page, the `reducted` template, a script reference to a
  non-existent `hello.js`, the 183-line cover template CSS.
* **WebSocket** is now a Pekko Streams flow (no scheduler actor) that pushes the event *and* the totals, so the presenter follows a
  newly opened event without reloading; it uses `location.host` instead of a hard-coded `ws://localhost:9000` and reconnects.

### API changes (breaking)

* Read endpoints are `GET` (they were `POST`); the vote endpoint moved from `/voter/addvote` to `/voter/vote`, event creation from
  `/admin/addevent` to `POST /admin/event` and returns `201`.
* JSON fields are consistent: `description` (was `desc` in responses), `name`/`votes` (was `label`/`y`, a CanvasJS leftover),
  `candidateId`/`eventId`, `initialVotes`.
* Errors are `{"error": "..."}` with meaningful statuses (404, 409, 400) rather than always 400.

### Configuration & security

* `play.crypto.secret` is now `play.http.secret.key`, overridable with `APPLICATION_SECRET`.
* Evolutions are applied automatically; no more clicking "apply this script" on first start.
* The Hikari setting had a typo (`hickaricp`) and was silently ignored; fixed.
* A strict Content-Security-Policy is enabled (`script-src 'self'`): no inline scripts, no CDNs. UI strings are inserted with
  `textContent`, never as HTML.
* The schema got `VARCHAR` lengths, an index on `VOTES(EVENTID, VOTER)`, `CURRENT` renamed to `IS_CURRENT` (a reserved word in
  newer SQL dialects), and the `Downs` script now drops tables in a valid order.
* The H2 database moved from the repository root to `./data` (git-ignored). The committed `database.db.*` files and `h2*.jar/sh`
  were deleted: H2 2.x cannot read 1.4 files. Existing 2017 data is therefore not migrated.

### Tests

The old suite (specs2, 6 files) only covered the DAOs and a redirect, and its browser test needed a Selenium driver. The new suite has **35 tests**:

| Suite | Covers |
|-------|--------|
| `NewEventSpec` | every validation rule |
| `JsonFormatsSpec` | wire format, defaults, malformed input |
| `JdbcVotingRepositorySpec` | event lifecycle, initial votes, vote limit (per voter and per event), **concurrent** votes, stale/unknown candidate, transactional rollback, snapshot; `EmptyRepositorySpec` for the no-event case |
| `ApiSpec` | routing, status codes, JSON contracts, pages, assets, CSP header, error bodies |
| `PresenterWebSocketSpec` | real server + real WebSocket client receiving live updates after an event is created |

### Known limitations

* The vote limit lock is in-process: with several application nodes a database lock would be needed.
* There is no authentication: anyone can open `/admin`, and the voter id is self-declared (as in 2017). Put it behind a
  reverse proxy or add authentication before exposing it.
* Front-end JavaScript has no automated tests (it was checked manually in a browser: all three pages render and the presenter
  receives live updates under the strict CSP).

## License

Apache License 2.0, see `LICENSE`.
