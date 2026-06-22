# Wurdal — Wordle CLI

A command-line Wordle game where players register, play, and compete on a leaderboard. Guess 5-letter words in up to 6 attempts. Each guess reveals letter positions through color-coded feedback.

## Table of Contents

- [Quick Start](#quick-start)
- [How to Play](#how-to-play)
- [Command Reference](#command-reference)
- [Game Rules](#game-rules)
- [Implementation Specification](#implementation-specification)

---

## Quick Start

### Prerequisites

- Java 17 or later
- Gradle wrapper included (no separate installation needed)

### Build

```bash
./gradlew build
```

### Run

#### Option 1: Via Gradle

```bash
./gradlew run --args="REGISTER alice"
./gradlew run --args="GUESS alice crane"
./gradlew run --args="LEADERBOARD"
./gradlew run --args="LEADERBOARD --by-guesses"
```

#### Option 2: As a Native Executable

Build and install once:

```bash
./gradlew installDist
```

Run the executable directly:

```bash
./build/install/wurdal/bin/wurdal REGISTER alice
./build/install/wurdal/bin/wurdal GUESS alice crane
./build/install/wurdal/bin/wurdal LEADERBOARD
```

**Tip:** Add to your PATH for global access:

```bash
sudo ln -s "$PWD/build/install/wurdal/bin/wurdal" /usr/local/bin/wurdal
```

Then simply run:

```bash
wurdal REGISTER alice
```

> After code changes, re-run `./gradlew installDist` to update the executable.

---

## How to Play

### Game Flow

1. **Register** a player account
2. **Start a new game** (a random 5-letter word is chosen)
3. **Make guesses** (up to 6 attempts)
4. **Check the leaderboard** to see who's winning

### Feedback on Each Guess

Each letter in your guess is color-coded:

- 🟩 **Green** — Letter is in the correct position
- 🟨 **Yellow** — Letter is in the word but wrong position
- ⬜ **Gray** — Letter is not in the word

You win by guessing the word before your 6 attempts run out. A new game clears your previous guesses and picks a new word.

---

## Command Reference

### Commands at a Glance

| Command | Purpose | Example |
|---------|---------|---------|
| `REGISTER <name>` | Create a player account | `REGISTER alice` |
| `NEW_GAME` | Start a new game (pick a random word) | `NEW_GAME` |
| `GUESS <name> <word>` | Make a guess and display the board | `GUESS alice crane` |
| `LEADERBOARD [--by-guesses]` | Show rankings (default: by accuracy) | `LEADERBOARD --by-guesses` |

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Invalid input or rule violation |
| 2 | Incorrect command usage |
| 3 | Unexpected application error |

---

### REGISTER — Create a Player

Create a new player account.

```bash
wurdal REGISTER alice
```

**Valid player names:**
- Only letters, numbers, hyphens, and underscores
- Not blank
- Unique (no duplicates)

**Errors:**

| Condition | Error | Exit Code |
|-----------|-------|-----------|
| Name already registered | `Player already registered` | 1 |
| Invalid characters or blank | `Invalid player name` | 1 |
| Missing name argument | `usage: wurdal REGISTER <player-name>` | 2 |

---

### NEW_GAME — Start a New Game

Pick a random secret word and display an empty board.

```bash
wurdal NEW_GAME
```

**Output (example for a 5-letter word):**

```
✨ New game started ✨

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
*   *  *   *  *   *  *   *  *   *
*****  *****  *****  *****  *****
```

**Notes:**
- No player sees the same word twice (unless all words have been exhausted)
- Each NEW_GAME resets your guess history for that word
- Exit code: 0

---

### GUESS — Make a Guess

Submit a guess and view the board with feedback.

```bash
wurdal GUESS alice crane
```

**Valid guesses:**
- Exactly 5 letters
- Only letters (a–z, case-insensitive)
- Must be in the game's dictionary

**Output:** Board showing all guesses with color feedback

**When you win:**

```
player [alice] guessed the word in [2] guesses
```

**Errors:**

| Condition | Error | Exit Code |
|-----------|-------|-----------|
| No active game | `No active game for player: alice` | 1 |
| Player not registered | `Player not found` | 1 |
| Invalid guess format | `Invalid guess [xyz]` | 1 |
| Already guessed this word | `Player has already guessed [crane]` | 1 |
| Missing arguments | `Usage: wurdal GUESS <player-name> <word>` | 2 |

---

### LEADERBOARD — View Rankings

Display the top players.

```bash
wurdal LEADERBOARD
```

**Default ranking (by accuracy):**

```
Sorted by: by-num-of-games

1. alice - 3 games - avg 2.3 guesses
2. jordan - 1 game - avg 3.0 guesses
3. sam - 2 games - avg 4.5 guesses
```

**Ranking by games completed:**

```bash
wurdal LEADERBOARD --by-guesses
```

```
Sorted by: by-guesses

1. alice - 3 games - avg 2.3 guesses
2. sam - 2 games - avg 4.5 guesses
3. jordan - 1 game - avg 3.0 guesses
```

**Ranking rules:**

- **Default (accuracy):** Fewest average guesses → most games → alphabetical
- **--by-guesses:** Most games → fewest average guesses → alphabetical
- Only players with at least one solved game appear
- Average is shown to one decimal place

---

## Game Rules

### Core Mechanics

- **One current word** per game (reset by NEW_GAME)
- **6 attempts per game**
- **5-letter words** only
- **Case-insensitive** guesses (all converted to lowercase internally)
- **Case-sensitive** player names (alice ≠ Alice)

### Scoring

- Recorded only on a correct guess
- Number of guesses it took is stored (1–6)
- Losses (-1) are tracked separately

### Word Selection

- Random word from the dictionary
- No repeats per player (until word pool exhausted)
- When all words have been seen by all players, repetition begins

### Guess Evaluation

Each position is evaluated independently:

1. Mark all correct-position matches as **green**
2. For remaining letters, mark in-word-but-wrong-position as **yellow**
3. Remaining letters are **gray** (not in word)

**Example (secret word: STONE):**

```
Guess: CRANE
- C: gray (not in word)
- R: gray (not in word)
- A: gray (not in word)
- N: yellow (in word, wrong position → position 4)
- E: green (correct position → position 5)

Guess: STOLE
- S: green (position 1)
- T: green (position 2)
- O: green (position 3)
- L: yellow (in word, wrong position)
- E: green (position 5)
```

---

## Implementation Specification

### Command Contract

Every command must:
- Produce consistent, predictable output
- Use the exact error messages specified
- Return the correct exit codes
- Validate inputs rigorously

### Output Rules

- ✅ **Successful commands** → standard output
- ❌ **Failed commands** → standard error
- **No timestamps, random IDs, framework output, stack traces, or environment paths** in normal output

### Persistence

The application must preserve state between commands:

- Registered players
- Current game secret word (resets per NEW_GAME)
- All guesses in current game (to reconstruct the board)
- Per-player game history (games solved, total guesses, words already seen)

### Output Stability

- Board format must be identical every time
- Color codes (ANSI) are used for feedback
- Error messages must match exactly (case, punctuation, etc.)

---

## Word Bank

The game uses two word lists:

- `word_bank/wordle_dict.txt` — Words available as secret words (picked for games)
- `word_bank/valid_words.txt` — Words accepted as valid guesses

All teams must use the same word lists to ensure consistency.

---

## For Developers

Want to contribute or extend the codebase? Start here:

### Project Structure

The code is organized into focused packages for easy navigation and extension:

```
src/main/java/wurdal/
├── App.java                          # Application entry point
├── game/
│   └── GameEngine.java              # Core game logic and state
├── command/
│   └── CommandLineParser.java       # CLI command routing
├── leaderboard/
│   └── LeaderboardEntry.java        # Player stats record
└── persistence/
    ├── PersistenceLayer.java        # Storage interface (abstraction)
    └── FileBasedPersistence.java    # File-based implementation
```

### Getting Started

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a command
./gradlew run --args="REGISTER alice"
```

### Documentation

- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — How to contribute, code style, adding features

### Key Features for Developers

- **Dependency Injection** — Loosely-coupled, testable code
- **PersistenceLayer Interface** — Swap storage backends (files → database) without touching game logic
- **Unit Tests** — Example tests included; test files use mocked persistence
- **Clear Separation of Concerns** — GameEngine, CommandParser, and Persistence are independent
