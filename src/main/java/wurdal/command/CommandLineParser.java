package wurdal.command;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import wurdal.game.GameEngine;
import wurdal.leaderboard.LeaderboardEntry;

/**
 * Parses and executes CLI commands for the Wordle game.
 * Supports: REGISTER, NEW_GAME, GUESS, LEADERBOARD
 */
public class CommandLineParser {

    public enum actions { REGISTER, NEW_GAME, GUESS, LEADERBOARD }

    private static final String API_BASE = "http://localhost:8080";
    private static final String SESSION_FILE = "game_state/session.txt";

    public void Parse(GameEngine game, String playerInput) {
        // Normalize the player input
        var normInput = playerInput.strip().split(" ");
        // Extract the command
        var command = normInput[0].toUpperCase().replace("-", "_");
        // Check if input is empty
        emptyHandler(command, playerInput);
        // Check if command exists
        commandExistsHandler(command, playerInput);

        switch (actions.valueOf(command)) {
            case REGISTER:
                handleRegister(game, normInput);
                break;
            case NEW_GAME:
                handleNewGame(game, normInput);
                break;
            case GUESS:
                handleGuess(game, normInput);
                break;
            case LEADERBOARD:
                handleLeaderboard(game, normInput);
                break;

            default:
                System.err.println("Unknown command: " + command);
        }
    }

    // Helper Functions
    private int getLeaderboardIndexByPlayerName(GameEngine game, String playerName) {
        for (int i = 0; i < game.leaderboard.size(); i++) {
            if (game.leaderboard.get(i).name().equals(playerName.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    private void commandExistsHandler(String command, String fullCommand) {
        // Short circuit if command not in actions
        boolean commandExists = Arrays.stream(actions.values()).anyMatch(action -> action.name().equals(command));
        if (!commandExists) {
            System.err.println("Unknown command: " + command);
            System.exit(1);
        }
    }

    private void emptyHandler(String input, String fullCommand) {
        if (input.isEmpty()) {
            System.err.println("Invalid command: " + fullCommand);
            System.exit(1);
        }
    }

    private void guessHandler(GameEngine game, String input, String playerName, String fullCommand) {
        // Validate the playerName
        validPlayerHandler(game, playerName, String.join(" ", fullCommand));
        // Check if the player has a game active
        if (!game.playerGuesses.containsKey(playerName)) {
            System.err.println("No active game for player: " + playerName);
            System.exit(1);
        }
        // Check if the guess is the word length
        if (input.length() != GameEngine.DEFAULT_WORD_LENGTH || !game.wordDictionary.contains(input)) {
            System.err.println("Invalid guess [%s]".formatted(input));
            System.exit(1);
        }
        // Check if the player has already guessed the word
        if (game.playerGuesses.get(playerName).contains(input)) {
            System.err.println("Player has already guessed [%s]".formatted(input));
            System.exit(1);
        }
        // Regardless if they got it or not save the guess
        game.playerGuesses.get(playerName).add(input);

        game.printBoardWithGuesses(GameEngine.DEFAULT_WORD_LENGTH, game.playerHiddenWords.get(playerName), game.playerGuesses.get(playerName));
        // Check if the player has correctly guessed the word / or has reached max attempts
        var correctlyGuessed = input.equals(game.playerHiddenWords.get(playerName));
        var outOfGuesses = game.playerGuesses.get(playerName).stream().count() >= GameEngine.BOARD_ROWS;
        if (correctlyGuessed || outOfGuesses) {
            var currNumOfGuesses = game.playerGuesses.get(playerName).stream().count();
            if (correctlyGuessed) {
                System.out.println("player [%s] guessed the word in [%d] guesses".formatted(playerName, currNumOfGuesses));
            } else {
                System.out.println("player [%s] DID NOT guess the word [%s]".formatted(playerName, game.playerHiddenWords.get(playerName)));
            }
            int leaderboardIdx = getLeaderboardIndexByPlayerName(game, playerName);
            ArrayList<Integer> currGames = game.leaderboard.get(leaderboardIdx).games();
            Integer newGame = (int) (input.equals(game.playerHiddenWords.get(playerName)) ? currNumOfGuesses : -1);
            currGames.add(newGame);
            game.leaderboard.set(leaderboardIdx, new LeaderboardEntry(playerName, currGames));
            game.savePlayersToFile();
        }
    }

    private void validPlayerHandler(GameEngine game, String input, String fullCommand) {
        if (input.isEmpty() || game.pattern.matcher(input).find()) {
            System.err.println("Invalid player name");
            System.exit(1);
        }
    }

    private void handleRegister(GameEngine game, String[] normInput) {
        if (normInput.length < 2) {
            // Check if there was a blank name (e.g. "register   ")
            // In that case normInput only has ["register"] after split
            System.err.println("Name cannot be empty.");
            System.exit(1);
        }

        String playerName = normInput[1].strip();

        if (playerName.isBlank()) {
            System.err.println("Name cannot be empty.");
            System.exit(1);
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonBody = "{\"name\": \"%s\"}".formatted(playerName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/players"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                // Extract id from response (simple parse: {"id":1,"name":"alice"})
                String body = response.body();
                String idStr = body.split("\"id\":")[1].split("[,}]")[0].trim();

                // Store session
                Files.createDirectories(Paths.get("game_state"));
                Files.writeString(Paths.get(SESSION_FILE), idStr);

                // Fetch the board
                HttpRequest boardRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/players/" + idStr + "/board"))
                        .GET()
                        .build();

                HttpResponse<String> boardResponse = client.send(boardRequest, HttpResponse.BodyHandlers.ofString());

                System.out.println("May the odds be in your favor %s!".formatted(playerName));
                game.printNewGameBoard(GameEngine.DEFAULT_WORD_LENGTH);

            } else if (response.statusCode() == 409) {
                System.err.println("That name is already taken. Please choose another.");
                System.exit(1);
            } else if (response.statusCode() == 400) {
                System.err.println("Name cannot be empty.");
                System.exit(1);
            } else {
                System.err.println("Unexpected error: " + response.body());
                System.exit(1);
            }

        } catch (java.net.ConnectException e) {
            System.err.println("Looks like the wurdal servers are taking a loss... try again later!");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Looks like the wurdal servers are taking a loss... try again later!");
            System.exit(1);
        }
    }

    private void handleNewGame(GameEngine game, String[] normInput) {
        if (normInput.length < 2) {
            System.err.println("Invalid Arguments: NEW_GAME <player-name>");
            System.exit(2);
        }

        String playerName = normInput[1].strip().toLowerCase();
        validPlayerHandler(game, playerName, String.join(" ", normInput));
        boolean playerExists = game.leaderboard.stream().anyMatch(entry -> entry.name().equals(playerName.toLowerCase()));
        if (!playerExists) {
            System.err.println("Error: player not found");
            System.exit(1);
        }

        if (game.playerHiddenWords.getOrDefault("playerName",null) != null){
            System.err.println("Error: game in progress");
            System.exit(1);
        }

        if (game.hasSeenAllWords(playerName)) {
            System.err.println("Error: no words available");
            System.exit(1);
        }

        // if (game.player)

        game.playerHiddenWords.put(playerName, game.chooseRandomWord(playerName));
        game.playerGuesses.put(playerName, new ArrayList<>());
        game.printNewGameBoard(GameEngine.DEFAULT_WORD_LENGTH);
        game.saveGamesToFile();
    }

    private void handleGuess(GameEngine game, String[] normInput) {
        if (normInput.length < 3) {
            System.err.println("Invalid Arguments: GUESS <player-name> <word>");
            System.exit(1);
        }
        String playerName = normInput[1].strip().toLowerCase();
        String guessWord = normInput[2].strip();
        guessHandler(game, guessWord, playerName, String.join(" ", normInput));
        game.saveGamesToFile();
    }

    private void handleLeaderboard(GameEngine game, String[] normInput) {
        boolean byGuesses = normInput.length > 1 && normInput[1].equals("--by-guesses");
        System.out.println("Sorted by: %s".formatted(byGuesses ? "by-guesses" : "by-num-of-games"));
        game.printLeaderboard(byGuesses);
    }
}
