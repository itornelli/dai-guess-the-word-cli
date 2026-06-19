# Description

Java Wordle Dupe

# Quickstart

## Prerequisites

- Java 17+
- The Gradle wrapper is included — no separate Gradle installation required.

## Build

```bash
./gradlew build
```

## Run (via Gradle)

Run a single command directly:

```bash
./gradlew run --args="<command> <args>"
```

Examples:

```bash
./gradlew run --args="REGISTER alice"
./gradlew run --args="GUESS alice crane"
./gradlew run --args="LEADERBOARD"
./gradlew run --args="LEADERBOARD --by-guesses"
```

## Run (as a native executable)

Build and install the executable once:

```bash
./gradlew installDist
```

This generates `build/install/wurdal/bin/wurdal`. Run it directly without Gradle:

```bash
./wurdal REGISTER alice
./wurdal GUESS alice crane
./wurdal LEADERBOARD
./wurdal LEADERBOARD --by-guesses
```

To make `wurdal` available system-wide from any directory:

```bash
sudo ln -s "$PWD/build/install/wurdal/bin/wurdal" /usr/local/bin/wurdal
```

Then simply run:

```bash
wurdal REGISTER alice
```

> Re-run `./gradlew installDist` after any code changes to update the executable.



# Rules

Command Contract
Every implementation must support the same commands, arguments, output format, error format, validation rules, and exit codes.

## Commands

| Command | Purpose |
|---------|---------|
| `register <player-name>` | Create a player. |
| `new-game` | Choose a new secret word and clear previous guesses. |
| `guess <player-name> <word>` | Submit a guess and display the Wordle board. |
| `leaderboard [--by-games]` | Show players by accuracy or games completed. |

## Exit Codes

| Exit Code | Meaning |
|-----------|---------|
| 0 | Command completed successfully. |
| 1 | Command failed because of invalid input or game-rule validation. |
| 2 | Command failed because of incorrect command usage. |
| 3 | Command failed because of an unexpected application error. |
Output Rules
Successful commands write to standard output.

Failed commands write to standard error.

Output should be stable, predictable, and easy to verify.

Do not include timestamps, random IDs, framework banners, stack traces, or environment-specific paths in normal command output.

Register
Create a player.


wurdal register alex
Successful output:


Player registered: alex
Exit code:


0
Register Validation
A player name is valid when it:

Is not blank.
Contains only letters, numbers, hyphens, and underscores.
Is unique.
If the player name is invalid:


Error: invalid player name
Exit code:


1
If the player already exists:


Error: player already exists
Exit code:


1
If the command is missing the player name:


Usage: wurdal register <player-name>
Exit code:


2
New Game
Choose a new secret word and clear previous guesses.


wurdal new-game
The CLI picks a random word from the word list. Players do not see the chosen word.

The CLI prints an empty board. Each cell is drawn as a box made of asterisks. The number of columns matches the length of the secret word. The board always has six rows.

Successful output (example for a five-letter word):


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
Exit code:


0
New Game Validation
If all words in the word list have already been seen by all registered players:


Error: no words available
Exit code:


1
Guess
Submit a guess for the current secret word.


wurdal guess <player-name> <word>
Example:


wurdal guess alex crane
After each guess the CLI prints the full board using the same asterisk-box format as new-game.

Each guessed letter is shown centered inside its box. The box is colored to show the result:

Green — letter is in the correct position
Yellow — letter is in the word but in the wrong position
Gray — letter is not in the word
Rows that have not yet been guessed remain as empty boxes.

Example board after two guesses (crane, stole) on a five-letter word, with the secret word stone:


*****  *****  *****  *****  *****
* c *  * r *  * a *  * n *  * e *
*****  *****  *****  *****  *****

*****  *****  *****  *****  *****
* s *  * t *  * o *  * l *  * e *
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
The color of each box is set using terminal color codes.

When the player's guess matches the secret word exactly, the board is printed and then:


alex solved it in 2 guesses!
When the guess is not correct, the board is printed with no additional message.

Exit code for both outcomes:


0
Guess Validation
A guess is valid when it:

Is exactly the same length as the secret word.
Contains only letters.
If no game has been started yet (no secret word exists):


Error: no active game
Exit code:


1
If the player does not exist:


Error: player not found
Exit code:


1
If the guess word is invalid:


Error: invalid guess
Exit code:


1
If the command is missing arguments:


Usage: wurdal guess <player-name> <word>
Exit code:


2
Leaderboard
Show the top players.


wurdal leaderboard [--by-games]
By default, players are ranked by fewest average guesses: the player who solved games in the fewest guesses on average ranks highest.

With --by-games, players are ranked by number of completed games: most games solved first.

Each row shows the player name, games solved, and average guesses per solve.

Default output (ranked by average guesses):


Leaderboard

1. alex - 3 games - avg 2.3 guesses
2. jordan - 1 game - avg 3.0 guesses
3. sam - 2 games - avg 4.5 guesses
--by-games output (ranked by games solved):


Leaderboard

1. alex - 3 games - avg 2.3 guesses
2. sam - 2 games - avg 4.5 guesses
3. jordan - 1 game - avg 3.0 guesses
Exit code:


0
Leaderboard Rules
Default sort (fewest average guesses):

Lowest average guess count first.
Most games solved when averages are tied.
Player name alphabetically when both are tied.
--by-games sort:

Most games solved first.
Lowest average guess count when games are tied.
Player name alphabetically when both are tied.
Average guess count is rounded to one decimal place.

Only players who have solved at least one game appear on the leaderboard.

If no players have solved a game:


Leaderboard

No games completed yet.
Exit code:


0
If the command receives an unrecognized argument:


Usage: wurdal leaderboard [--by-games]
Exit code:


2
Game Rules
The application has one current secret word chosen by new-game.

Each letter in a guess is evaluated independently and its box is colored:

Green — letter is in the correct position.
Yellow — letter appears in the word but in the wrong position.
Gray — letter does not appear in the word.
A player solves the game when their guess matches the secret word exactly. The number of guesses it took is recorded for that player.

A new game can be started at any time with new-game. This resets the board and picks a new secret word.

No player should see the same secret word twice. When a player has already played a given word, that word is excluded from future games for that player. If all words have been used by all registered players, words may repeat.

Guess comparison is case-insensitive.

Player names are case-sensitive. alex and Alex are different players.

All teams must use the same word list.

Persistence
The CLI must preserve game state between commands.

At minimum, it must remember:

Registered players
Current secret word (resets with each new-game)
All guesses made during the current game (to rebuild the board)
Per-player game history: number of games solved and total guesses used across those solves
Per-player list of words already seen (to avoid repeats)
The storage mechanism may vary by implementation. A local JSON file is a common choice.

Suggested Feature Ownership
Each team may divide the work however it wants.

One practical split is:

Register
Responsible for:

Creating players
Validating player names
Preventing duplicate players
Persisting new players
New Game
Responsible for:

Selecting a random secret word from the word list
Persisting the secret word
Resetting the board (clearing previous guesses)
Guess Logic
Responsible for:

Accepting guesses
Validating players and guess format
Evaluating each letter against the secret word and choosing color
Persisting the guess
Awarding a point when the player solves the game
Guess Formatting
Printing the full board after each guess
Adding colors to the board
Leaderboard
Responsible for:

Reading persisted players and their game history
Computing average guess count per player
Sorting by accuracy (default) or games completed (--by-games)
Applying tiebreaker rules
Printing the leaderboard
Done
The project is done when:

The repository is created.
All collaborators have access.
All commands follow the contract.
Game state persists between commands.
The board displays correct color markers after each guess.
The full game flow works from the command line.
Wordlist