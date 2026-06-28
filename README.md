# Wurdal

Wurdal now runs as:
- a **Spring Boot API server** backed by **Postgres**
- a **CLI client** (`wurdal ...`) that logs in once and then calls game endpoints

## Commands

```bash
wurdal register <name>
wurdal login <name>
wurdal logout
wurdal board
wurdal guess <word>
```

`register` and `login` prompt for a password (hidden when a real terminal console is available).

## Exact Postgres Setup Commands

```bash
brew install postgresql@18
brew services start postgresql@18
psql postgres -c "CREATE ROLE wurdal WITH LOGIN PASSWORD 'wurdal';"
psql postgres -c "CREATE DATABASE wurdal OWNER wurdal;"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE wurdal TO wurdal;"
```

## Run the API Server

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wurdal
export SPRING_DATASOURCE_USERNAME=wurdal
export SPRING_DATASOURCE_PASSWORD=wurdal
./gradlew runServer
```

Flyway migrations auto-create:
- `users`
- `sessions`
- `games`
- `guesses`

## Run the CLI

In another terminal:

```bash
./gradlew installDist
./build/install/wurdal/bin/wurdal register Alice
./build/install/wurdal/bin/wurdal board
./build/install/wurdal/bin/wurdal guess crane
./build/install/wurdal/bin/wurdal logout
./build/install/wurdal/bin/wurdal login ALICE
./build/install/wurdal/bin/wurdal board
```

## API Notes

- Username matching is case-insensitive (`ALICE` logs into `Alice`).
- Passwords are stored as bcrypt hashes.
- CLI stores the current session ID in `~/.wurdal/session-id`.
- Signed-out board/guess calls return: `Please login to continue`.
