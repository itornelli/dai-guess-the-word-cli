# Contributing to Wurdal

Welcome! We're excited that you want to contribute to the Wordle CLI game. This guide will help you get started.

## Table of Contents

- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [How to Contribute](#how-to-contribute)
- [Adding New Features](#adding-new-features)
- [Code Style](#code-style)

---

## Development Setup

### Prerequisites

- Java 17 or later
- Git

### Getting Started

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd team-5-guess-the-word-cli
   ```

2. **Build the project:**
   ```bash
   ./gradlew build
   ```

3. **Run tests:**
   ```bash
   ./gradlew test
   ```

4. **Run the application:**
   ```bash
   ./gradlew run --args="REGISTER alice"
   ./gradlew run --args="GUESS alice crane"
   ./gradlew run --args="LEADERBOARD"
   ```

### Creating a Branch

Always create a feature branch for your work:

```bash
git checkout -b feat/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

Use descriptive names:
- `feat/` for new features
- `fix/` for bug fixes
- `refactor/` for code improvements
- `test/` for test additions
- `docs/` for documentation updates

---

## Project Structure

```
src/main/java/
├── wurdal/
│   ├── App.java                          # Main entry point (CLI launcher)
│   ├── game/
│   │   ├── GameEngine.java              # Core game state and logic
│   │   └── GuessEvaluator.java          # (future) Guess color evaluation
│   ├── command/
│   │   ├── CommandLineParser.java       # CLI command routing
│   │   └── Command.java                 # (future) Command interface
│   ├── leaderboard/
│   │   └── LeaderboardEntry.java        # Player stats record
│   └── persistence/
│       ├── PersistenceLayer.java        # Storage interface (abstraction)
│       └── FileBasedPersistence.java    # File-based implementation
│
src/test/java/
├── wurdal/
│   ├── game/
│   │   └── GameEngineTest.java
│   └── persistence/
│       └── PersistenceLayerTest.java
```

### Key Classes

- **`App.java`** — Application entry point. Sets up dependency injection.
- **`GameEngine.java`** — Core game logic. Manages state, players, leaderboard, rendering.
- **`CommandLineParser.java`** — Parses and routes CLI commands (REGISTER, NEW_GAME, GUESS, LEADERBOARD).
- **`PersistenceLayer.java`** — Interface for storage (file, database, etc.).
- **`FileBasedPersistence.java`** — CSV-based file storage implementation.
- **`LeaderboardEntry.java`** — Player record with game history.

---

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Tests in a Specific Package

```bash
./gradlew test --tests "wurdal.game.*"
```

### Run a Single Test Class

```bash
./gradlew test --tests "wurdal.game.GameEngineTest"
```

### Run a Specific Test Method

```bash
./gradlew test --tests "wurdal.game.GameEngineTest.testGameEngineInitialization"
```

### View Test Results

HTML report: `build/reports/tests/test/index.html`

---

## How to Contribute

### Good First Issues

- Add more unit tests for `GameEngine`
- Add integration tests for CLI commands
- Improve error messages
- Refactor `CommandLineParser` into separate handler classes
- Add logging using SLF4J

### Steps to Submit a Contribution

1. **Create a branch** (see [Creating a Branch](#creating-a-branch))

2. **Make your changes** with clear commit messages:
   ```bash
   git commit -m "feat: add player validation tests"
   ```

3. **Push your branch:**
   ```bash
   git push origin feat/your-feature-name
   ```

4. **Create a Pull Request** describing:
   - What problem it solves
   - How it works
   - Any tests added
   - Any breaking changes

5. **Address review feedback** and iterate

---

## Adding New Features

### Example: Adding a New Persistence Backend

1. **Create a new implementation of `PersistenceLayer`:**
   ```java
   package wurdal.persistence;
   
   public class JsonFilePersistence implements PersistenceLayer {
       @Override
       public List<LeaderboardEntry> loadPlayers() {
           // Load from JSON
       }
       
       // ... implement other methods
   }
   ```

2. **Create tests:**
   ```java
   package wurdal.persistence;
   
   public class JsonFilePersistenceTest {
       @Test
       public void testLoadPlayersFromJson() {
           // Test logic
       }
   }
   ```

3. **Update `App.java` to use the new implementation:**
   ```java
   PersistenceLayer persistence = new JsonFilePersistence();
   ```

### Example: Adding a New Game Command

The `CommandLineParser` currently handles all commands. To add a new command:

1. **Add the command to the enum:**
   ```java
   public enum actions { REGISTER, NEW_GAME, GUESS, LEADERBOARD, MY_NEW_COMMAND }
   ```

2. **Add a handler method:**
   ```java
   private void handleMyNewCommand(GameEngine game, String[] normInput) {
       // Command logic
   }
   ```

3. **Add a case in the switch statement:**
   ```java
   case MY_NEW_COMMAND:
       handleMyNewCommand(game, normInput);
       break;
   ```

4. **Add tests** for the new command

---

## Code Style

### Naming Conventions

- **Classes:** `PascalCase` (e.g., `GameEngine`, `LeaderboardEntry`)
- **Methods/Variables:** `camelCase` (e.g., `printLeaderboard()`, `playerGuesses`)
- **Constants:** `UPPER_SNAKE_CASE` (e.g., `DEFAULT_WORD_LENGTH`)
- **Packages:** `lowercase.no.hyphens` (e.g., `wurdal.game`)

### Formatting

- **Indentation:** 4 spaces (no tabs)
- **Line length:** Aim for <120 characters
- **Braces:** Opening brace on same line (Java convention)

### Javadoc

Document public classes and public methods:

```java
/**
 * Parses and executes CLI commands for the Wordle game.
 * 
 * Supports: REGISTER, NEW_GAME, GUESS, LEADERBOARD
 */
public class CommandLineParser {
    
    /**
     * Parse and execute a command.
     * 
     * @param game the GameEngine instance
     * @param playerInput the raw command string
     */
    public void Parse(GameEngine game, String playerInput) {
        // ...
    }
}
```

### Testing

- Write tests for new features (aim for >80% coverage)
- Use descriptive test names: `testGuessHandlerValidatesPlayerName()`
- Mock external dependencies (use mock PersistenceLayer)
- One assertion per test when possible, or group related assertions

---

## Questions?

See [DEVELOPMENT.md](./DEVELOPMENT.md) for architecture and design decisions.

For questions, check the README or open a discussion issue.

Thank you for contributing! 🎉
