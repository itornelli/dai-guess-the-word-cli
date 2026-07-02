package wurdal.command;

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
        // Check if the guess is the correct length for the current hidden word
        String hiddenWord = game.playerHiddenWords.get(playerName);
        if (input.length() != hiddenWord.length() || !game.wordDictionary.contains(input)) {
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

        game.printBoardWithGuesses(game.playerHiddenWords.get(playerName).length(), game.playerHiddenWords.get(playerName), game.playerGuesses.get(playerName));
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
            System.err.println("usage: wurdal REGISTER <player-name>");
            System.exit(2);
        }

        String playerName = normInput[1].strip().toLowerCase();
        validPlayerHandler(game, playerName, String.join(" ", normInput));
        if (game.leaderboard.stream().anyMatch((entry) -> entry.name().equals(playerName.toLowerCase()))) {
            System.err.println("Error: player already exists.");
            System.exit(1);
        }

        if (!playerName.matches("[A-Za-z0-9_-]+"))
            System.out.println("Error: Invalid player name.");
            

        System.out.println("Player registered: %s".formatted(playerName));

        game.leaderboard.add(new LeaderboardEntry(playerName, new ArrayList<Integer>() {}));
        game.savePlayersToFile();
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
        game.printNewGameBoard(game.playerHiddenWords.get(playerName).length());
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
