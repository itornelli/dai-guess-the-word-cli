# Wurdal — System Design & Developer Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [The Server](#the-server)
   - [Entry Point](#entry-point)
   - [GameController](#gamecontroller)
   - [GameEngine](#gameengine)
   - [Database & Entities](#database--entities)
   - [Liquibase Migrations](#liquibase-migrations)
5. [The CLI Client](#the-cli-client)
   - [WurdalCli](#wurdalcli)
   - [ApiClient](#apiclient)
   - [SessionStore](#sessionstore)
   - [BoardRenderer](#boardrenderer)
6. [API Contract](#api-contract)
7. [Authentication](#authentication)
8. [Data Flow: End-to-End Examples](#data-flow-end-to-end-examples)
9. [DTOs (structures/api)](#dtos-structuresapi)
10. [Design Decisions & Why](#design-decisions--why)
11. [Environment & Configuration](#environment--configuration)

---

## Overview

Wurdal is a multiplayer Wordle-style word-guessing game built as a **client-server application**. Players register once, then use a CLI tool to play games against a central server that persists all state in PostgreSQL.

The repo contains **both** the server and the CLI client in one codebase, sharing the same DTO (data transfer object) types. This is intentional — since both sides are Java, they can share the same `structures/api` package rather than duplicating type definitions.

---

## Architecture

```
┌─────────────────────┐         HTTP/JSON         ┌──────────────────────────┐
│     CLI Client       │ ────────────────────────► │     Spring Boot Server   │
│                     │                           │                          │
│  WurdalCli          │ ◄──────────────────────── │  GameController          │
│  ApiClient          │                           │  GameEngine              │
│  BoardRenderer      │                           │  PlayerRepository        │
│  SessionStore       │                           │  GameRepository          │
└─────────────────────┘                           └────────────┬─────────────┘
         │                                                     │
   ~/.wurdal/session-id                               PostgreSQL (port 5432)
   (stores player id)                                 database: wordle
```

**Key principle:** The server is the single source of truth. The CLI holds only one piece of state: the player's `id` (used as an auth token). Everything else — current game, guesses, leaderboard — lives in the database.

---

## Project Structure

```
src/main/java/wurdal/
├── App.java                      ← Entry point (server or CLI)
│
├── cli/                          ← CLI client code
│   ├── WurdalCli.java            ← Command router
│   ├── ApiClient.java            ← HTTP calls to the server
│   ├── BoardRenderer.java        ← Terminal board drawing
│   └── SessionStore.java         ← Persists player id to disk
│
├── controllers/
│   └── GameController.java       ← All REST endpoints
│
├── game/
│   └── GameEngine.java           ← Word picking + guess evaluation logic
│
├── repository/
│   ├── PlayerRepository.java     ← JPA: players table
│   └── GameRepository.java       ← JPA: games table
│
└── structures/
    ├── Player.java               ← JPA entity
    ├── Game.java                 ← JPA entity
    └── api/                      ← Shared DTOs (server + CLI)
        ├── Board.java            ← Polymorphic interface for board responses
        ├── BoardRes.java         ← Board response payload
        ├── AuthResponse.java     ← Login response
        ├── RegisterRes.java      ← Register response
        ├── RegisterReq.java      ← Register request body
        ├── CredentialsRequest.java ← Login request body
        ├── GuessReq.java         ← Guess request body
        ├── Links.java            ← Hypermedia links
        ├── LeaderBoard.java      ← Leaderboard response
        └── ErrorResponse.java    ← CLI-side error representation

src/main/resources/
├── words.txt                     ← Word bank (130 words, loaded at startup)
├── application.properties        ← DB config, Liquibase config
└── db/changelog/                 ← Liquibase migration files
```

---

## The Server

### Entry Point

**`App.java`** is a single binary that decides at startup whether to run as a **server** or a **CLI client** based on the command-line arguments:

```java
if (args.length > 0 && !args[0].startsWith("--")) {
    // Has non-Spring args → run as CLI
    WurdalCli cli = new WurdalCli();
    System.exit(cli.run(args));
} else {
    // No args or Spring flags → start server
    SpringApplication.run(App.class, args);
}
```

**Why one binary?** It simplifies distribution. You ship one jar/script, and users run `wurdal register Alice` for CLI usage or run it with no args to start a server. The trade-off is that the server-side Spring context (JPA, Liquibase, etc.) is bundled even in CLI mode — but since the CLI exits before Spring initializes, this is harmless.

---

### GameController

**`controllers/GameController.java`** is the sole REST controller. It's implemented as a Java `record` with constructor injection (Spring supports this):

```java
@RestController
public record GameController(PlayerRepository playerRepo, GameRepository gameRepo, GameEngine gameEngine) { ... }
```

**Why a record?** Records enforce immutability of dependencies and eliminate boilerplate getters. Spring's constructor injection works perfectly with them.

**Endpoints summary:**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Root links (hypermedia discovery) |
| `POST` | `/players` | Register new player |
| `POST` | `/sessions` | Login (get player id) |
| `GET` | `/players/{id}/board` | View current game board |
| `POST` | `/players/{id}/guess` | Submit a guess |
| `GET` | `/leaderboard` | All player stats |

**Auth check** is handled inline via a helper method `checkAuth(playerId, authorization)` — see [Authentication](#authentication).

**Board building** is centralized in `buildBoardRes(Player, Game)` to avoid code duplication between the `board` and `guess` endpoints. Both endpoints ultimately need to return the same board structure.

**Game lifecycle in `guess`:**
1. Validate the bearer token matches the path `{id}`
2. Find the player's most recent game
3. Validate guess length matches hidden word length
4. Add guess to game, check win/loss conditions
5. If the game is over (won or lost): save it and immediately create the next game
6. Return the board state

**Why create the next game immediately on win/loss?** So the player never has to call a separate "new game" endpoint — the next `board` call just shows the fresh empty board.

---

### GameEngine

**`game/GameEngine.java`** is a Spring `@Component` responsible for two things:

1. **`chooseRandomWord(playerName)`** — picks a word from the dictionary that the player hasn't seen before. Tracks seen words per player in an in-memory `Map<String, Set<String>>`.

2. **`evaluateGuessColors(hiddenWord, guess, wordLength)`** — returns a `String[]` of ANSI color codes indicating which letters are correct position (`GREEN`), wrong position (`YELLOW`), or not in word (`RESET`).

**Words are loaded from classpath** (`words.txt` in `src/main/resources/`) at startup, not from the filesystem. This means the word bank is bundled into the jar — no external file dependency at runtime.

**Why in-memory seen-word tracking?** The alternative would be a DB column or table tracking seen words per player. For this scale, in-memory is simpler. The trade-off: server restart resets the seen-word history, meaning a player could theoretically see the same word again after a server restart. Acceptable for a learning/demo app.

**Color evaluation algorithm:**
1. First pass: mark exact matches (`GREEN`), decrement letter counts
2. Second pass: for remaining positions, mark wrong-position matches (`YELLOW`) if the letter still has remaining count — this correctly handles duplicate letters

---

### Database & Entities

Two JPA entities map directly to PostgreSQL tables:

**`Player` (`players` table)**

| Column | Type | Notes |
|--------|------|-------|
| `id` | serial PK | Auto-generated, also used as auth token |
| `name` | varchar(255) | Unique, case-sensitive at DB level |
| `games_won` | int | Running total |
| `games_lost` | int | Running total |
| `average_guesses` | decimal | Rolling average, updated on win |
| `is_in_game` | bool | Set by DB trigger on game insert |
| `game_id` | int | FK to current game, set by DB trigger |

**`Game` (`games` table)**

| Column | Type | Notes |
|--------|------|-------|
| `id` | serial PK | Auto-generated |
| `hidden_word` | varchar(255) | The secret word |
| `current_guesses` | varchar(255)[] | PostgreSQL native array of guesses |
| `player_id` | int | FK to players |
| `status` | int | 1=in-progress, 0=won, 2=lost |

**Why store guesses as a PostgreSQL array?** The alternative is a separate `guesses` table with a row per guess. A native array is simpler for this use case — guesses are always read and written together, always ordered, and never queried individually. The trade-off is less normalization.

**Why `@JdbcTypeCode(SqlTypes.ARRAY)` on `currentGuesses`?** Hibernate 6 requires an explicit type hint to map a `List<String>` to a PostgreSQL array column. Without it, Hibernate doesn't know to use the array JDBC type.

**The `status` integer encoding:**
- `1` = in-progress (default)
- `0` = won
- `2` = lost

Note: `0` for "won" might seem odd. It was the original team's convention and kept to avoid a data migration.

---

### Liquibase Migrations

All schema changes live in `src/main/resources/db/changelog/` and are registered in `db.changelog-master.yaml`. Migrations run automatically on server startup via `spring.liquibase.change-log`.

**Naming convention:** `NNNa-description.sql` where `NNN` is a zero-padded sequence number.

**Notable migrations:**

- **`007a` / `018a` — DB trigger `trg_update_player_in_game`**: When a new game is inserted, a PostgreSQL trigger automatically sets `players.is_in_game = true` and `players.game_id = <new_game_id>`. This keeps player state in sync without requiring the application to make two separate writes.

- **`014a` — Added UUID token** / **`020a` — Dropped UUID token**: The original auth design used a UUID token generated per player. This was replaced with the simpler approach of using the player's integer `id` as the bearer token, so the UUID column was dropped.

- **`016a` / `019a` — Delete all entries**: These are data wipe migrations run during development to reset the database during schema changes. They stay in the changelog because Liquibase tracks applied changesets by checksum — removing them would break the checksum chain.

**Why `ddl-auto=none`?** Hibernate can auto-generate DDL (`create`, `update`, etc.) but this is disabled in favor of explicit Liquibase migrations. This gives full control over schema changes, is safe for production, and keeps a traceable history of every schema change.

---

## The CLI Client

### WurdalCli

**`cli/WurdalCli.java`** routes CLI arguments to the appropriate handler. Each command maps to a private method:

```
wurdal register <name>   → handleRegister()
wurdal login <name>      → handleLogin()
wurdal logout            → handleLogout()
wurdal board             → handleBoard()
wurdal guess <word>      → handleGuess()
wurdal leaderboard       → handleLeaderboard()
```

Each handler returns an exit code (`0` = success, `1`/`2`/`3` = various errors) which `App.java` passes to `System.exit()`.

**Error handling:** `ApiClient.ApiException` is caught at the top-level `run()` method so no individual handler needs to deal with HTTP errors. A 401/403 response also clears the session (auto-logout on auth failure).

---

### ApiClient

**`cli/ApiClient.java`** wraps `RestTemplate` calls to the server. Each method corresponds to one API endpoint.

**`WURDAL_SERVER_URL` environment variable** lets you point the CLI at any server instance. Defaults to `http://localhost:8080`.

**Error parsing** handles two response formats:
1. `{"error": {"description": "..."}}` — the server's standard error format
2. `{"message": "...", "registerCommand": "..."}` — legacy flat format

This dual-format handling ensures that any future/past error format variations don't crash the CLI.

**Why `RestTemplate` instead of `HttpClient`?** `RestTemplate` integrates with Spring's `HttpMessageConverter` infrastructure (including the Jackson `ObjectMapper` config), making JSON deserialization seamless. `HttpClient` is lower-level and would require manual JSON parsing.

---

### SessionStore

**`cli/SessionStore.java`** persists the player's `id` to `~/.wurdal/session-id` on disk (a plain text file containing just the integer id).

**Why a file instead of environment variable or keychain?** A file persists across terminal sessions without requiring the user to set anything up. It's the simplest cross-platform solution. The `~/.wurdal/` directory is user-specific, so multiple users on the same machine don't interfere.

**Why store the integer `id` and not the player name?** The API requires the player `id` both in the URL path (`/players/{id}/board`) and as the bearer token. Storing the `id` directly means no extra lookup step on every command.

---

### BoardRenderer

**`cli/BoardRenderer.java`** draws the Wordle board to the terminal using ANSI color codes.

The board is always rendered as `BOARD_ROWS` (6) rows regardless of how many guesses have been made. Empty rows show empty cells. The color logic mirrors the server's `evaluateGuessColors` — but operates on the response's pre-evaluated `match` strings (`"full"`, `"partial"`, `"none"`) rather than re-computing colors.

**Why duplicate the color evaluation in the renderer instead of using `GameEngine` directly?** The CLI doesn't have access to the hidden word until the game is over — the server controls that. The server sends back the evaluated letter results so the client just renders what it receives.

---

## API Contract

### POST /players — Register

```
Request:  {"name": "Alice"}
Response: 201
{
  "id": 1,
  "name": "Alice",
  "_links": {
    "board": {"href": "/players/1/board"},
    "guess": {"href": "/players/1/guess"}
  }
}
```

### POST /sessions — Login

```
Request:  {"name": "tom"}   (case-insensitive)
Response: 200
{"id": 1, "name": "Tom"}
```

### GET /players/{id}/board — View Board

```
Headers:  Authorization: Bearer {id}
Response: 200
{
  "user": {"id": 1, "name": "Alice"},
  "current": {
    "length": 5,
    "guesses": [
      {"letters": [
        {"letter": "c", "match": "full"},
        {"letter": "r", "match": "none"},
        {"letter": "a", "match": "partial"},
        {"letter": "n", "match": "none"},
        {"letter": "e", "match": "none"}
      ]}
    ],
    "result": {"status": "in-progress", "word": "crane"}
  }
}
```

### POST /players/{id}/guess — Submit Guess

```
Headers:  Authorization: Bearer {id}
Request:  {"guess": "crane"}
Response: 200  (same board structure as above)

Error (wrong length): 422
{"error": {"description": "Guess must be exactly 5 letters"}}
```

### GET /leaderboard

```
Response: 200
{
  "players": [
    {"name": "Alice", "wins": 4, "losses": 1, "averageGuesses": 3.5},
    {"name": "Tom",   "wins": 2, "losses": 3, "averageGuesses": 4.1}
  ]
}
```

### GET / — Hypermedia Links

```
Response: 200
{
  "_links": {
    "register":    {"href": "/players"},
    "login":       {"href": "/sessions"},
    "leaderboard": {"href": "/leaderboard"}
  }
}
```

---

## Authentication

Authentication is intentionally minimal. The player's integer `id` serves as both the identifier and the bearer token:

1. On `register` or `login`, the server returns `{"id": N, "name": "..."}`.
2. The CLI stores `N` in `~/.wurdal/session-id`.
3. Every subsequent request includes `Authorization: Bearer N` in the header.
4. The server validates by checking that the bearer token matches the `{id}` in the URL path.

**Why not a UUID or JWT?** The user stories explicitly defined this simple scheme — the player id IS the token. For a learning/demo app this is acceptable. The security model is: anyone who knows your player id can act as you — fine for a game with no sensitive data.

**Why `id` in both the path AND the header?** The path makes URLs self-describing and RESTful. The Authorization header is the standard mechanism for conveying identity. The server cross-checks both to prevent one player from hitting another player's endpoints.

---

## Data Flow: End-to-End Examples

### Register and first board view

```
CLI                          Server                        PostgreSQL
 │                              │                               │
 │  POST /players               │                               │
 │  {"name": "Alice"}  ────────►│                               │
 │                              │  INSERT INTO players          │
 │                              │  (name='Alice') ─────────────►│
 │                              │◄───────────────── id=7        │
 │                              │  chooseRandomWord("Alice")    │
 │                              │  INSERT INTO games            │
 │                              │  (hidden_word='crane',        │
 │                              │   player_id=7) ──────────────►│
 │                              │  [trigger fires:              │
 │                              │   players.is_in_game=true     │
 │                              │   players.game_id=<game_id>]  │
 │◄─────────────────────────────│                               │
 │  201 {id:7, name:"Alice",    │                               │
 │       _links:{board,guess}}  │                               │
 │                              │                               │
 │  store "7" in ~/.wurdal/session-id
 │                              │                               │
 │  GET /players/7/board        │                               │
 │  Authorization: Bearer 7 ──►│                               │
 │                              │  SELECT * FROM games          │
 │                              │  WHERE player_id=7            │
 │                              │  ORDER BY id DESC LIMIT 1 ───►│
 │◄─────────────────────────────│◄────────── game row           │
 │  200 {user:{id:7,...},       │                               │
 │       current:{length:5,     │                               │
 │       guesses:[], ...}}      │                               │
```

### Submitting a guess

```
CLI                          Server                        PostgreSQL
 │                              │                               │
 │  POST /players/7/guess       │                               │
 │  Authorization: Bearer 7     │                               │
 │  {"guess": "stone"} ────────►│                               │
 │                              │  token == path id? ✓          │
 │                              │  SELECT game WHERE            │
 │                              │  player_id=7 ORDER BY id ────►│
 │                              │◄──────────── game             │
 │                              │  evaluateGuessColors(         │
 │                              │    "crane","stone",5)         │
 │                              │  → [RESET,RESET,RESET,        │
 │                              │     YELLOW,RESET]             │
 │                              │  game.addGuess("stone")       │
 │                              │  game.status = 1 (in-progress)│
 │                              │  UPDATE games SET ... ────────►│
 │◄─────────────────────────────│                               │
 │  200 {board with colored     │                               │
 │       letter results}        │                               │
```

---

## DTOs (structures/api)

All DTOs are **Java records** — immutable value objects with auto-generated constructors, `equals`, `hashCode`, and `toString`. They're used by both the server (serializing responses) and the CLI (deserializing responses).

| File | Direction | Purpose |
|------|-----------|---------|
| `RegisterReq` | CLI → Server | `{"name": "Alice"}` |
| `CredentialsRequest` | CLI → Server | `{"name": "Tom"}` |
| `GuessReq` | CLI → Server | `{"guess": "crane"}` |
| `RegisterRes` | Server → CLI | `{id, name, _links}` |
| `AuthResponse` | Server → CLI | `{id, name}` |
| `BoardRes` | Server → CLI | Full board state |
| `Board` | — | Polymorphic interface for board (only `BoardRes` currently) |
| `Links` | Server → CLI | Hypermedia links object |
| `LeaderBoard` | Server → CLI | `{players: [PlayerStats...]}` |
| `ErrorResponse` | CLI internal | Parsed error from server for display |

**Why `Board` as an interface with `@JsonTypeInfo`?** The board endpoint can return different response shapes (success vs error). Jackson's polymorphic deserialization (`@JsonTypeInfo` + `@JsonSubTypes`) lets the CLI deserialize `Board` without knowing in advance which subtype it will get. The `"type"` discriminator field in the JSON tells Jackson which concrete class to instantiate.

**`_links` with `@JsonProperty`:** Java records use camelCase field names, but the hypermedia convention is `_links` (with underscore prefix). `@JsonProperty("_links")` on the `links` field in `RegisterRes` bridges this naming gap without changing the Java field name.

---

## Design Decisions & Why

### Single binary for server + CLI
Discussed above — simplifies distribution. The `App.java` dispatcher is the seam.

### No password / simple auth
The user stories defined the auth model. It's intentionally simple for a learning project. Adding bcrypt + password would require a password column, a `/sessions` endpoint that verifies it, and session/JWT token management.

### Case-insensitive login
Login does an exact DB lookup first, then falls back to a case-insensitive in-memory search across all players. This means `wurdal login ALICE` works even though Alice registered as `Alice`. The trade-off is the fallback requires loading all players — acceptable at small scale.

### Leaderboard uses a DTO, not the Player entity directly
Originally the leaderboard serialized `Player` objects directly, which leaked internal fields (and previously leaked the UUID token). Using a `LeaderBoard.PlayerStats` DTO gives explicit control over what's exposed.

### `emergencySetCurrentGuesses` in `Game`
This method name (`emergencySetCurrentGuesses`) is a code smell from when the guesses array could come back as `null` from the DB after a migration. It's a defensive fallback. Worth renaming or removing once the data is confirmed clean.

### DB trigger for `is_in_game` / `game_id`
Rather than requiring the application to issue two writes (insert game + update player), a PostgreSQL trigger handles the player update automatically. This keeps the application code simpler and ensures the invariant is enforced at the DB level regardless of how a game is inserted.

---

## Environment & Configuration

### Running the server

```bash
# Requires PostgreSQL running at localhost:5432 with a database named "wordle"
./gradlew bootRun
```

Or with a custom DB URL:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://myhost:5432/wordle ./gradlew bootRun
```

### Running the CLI

```bash
# After ./gradlew installDist:
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli register Alice
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli login alice
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli board
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli guess crane
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli leaderboard
./build/install/dai-guess-the-word-cli/bin/dai-guess-the-word-cli logout
```

### Pointing CLI at a different server

```bash
export WURDAL_SERVER_URL=http://myserver.example.com:8080
./build/install/.../dai-guess-the-word-cli board
```

### application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wordle
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.maximum-pool-size=5
spring.jpa.hibernate.ddl-auto=none          # Hibernate never touches schema
spring.jpa.properties.hibernate.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```
