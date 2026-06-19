import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;


public class wurdal {

    private static final int DEFAULT_WORD_LENGTH = 5;
    private static final int BOARD_ROWS = 6;
    private static final String CELL_BORDER = "*****";
    private static final String CELL_EMPTY = "*   *";

    public String currentInput = "";
    public Map<String, String> playerHiddenWords = new HashMap<>();
    public Map<String, List<String>> playerGuesses = new HashMap<>();
    public Set<String> currentSeenWords = new HashSet<String>() {};
    public Set<String> currentGames = new HashSet<String>(){};
    public List<LeaderboardEntry> leaderboard = new ArrayList<LeaderboardEntry>(){};
    public CommandLineParser parser = new CommandLineParser(){};

    public Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    public wurdal() {
        loadPlayersFromFile();
        loadGamesFromFile();
    }

    private void loadPlayersFromFile() {
        try {
            if (Files.exists(Paths.get("game_state/players.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("game_state/players.txt"));
                for (String line : lines) {
                    String playerName = line.trim();
                    if (!playerName.isEmpty()) {
                        leaderboard.add(new LeaderboardEntry(playerName, new ArrayList<Integer>()));
                    }
                }
                System.out.println("Loaded " + leaderboard.size() + " registered players.");
            }
        } catch (IOException e) {
            System.err.println("Error loading players.txt: " + e.getMessage());
        }
    }

    private void loadGamesFromFile() {
        try {
            if (Files.exists(Paths.get("game_state/games.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("game_state/games.txt"));
                for (String line : lines) {
                    String[] parts = line.split(":", -1);
                    if (parts.length >= 3) {
                        String playerName = parts[0].trim();
                        String hiddenWord = parts[1].trim();
                        String guessesStr = parts[2].trim();
                        
                        playerHiddenWords.put(playerName, hiddenWord);
                        
                        List<String> guesses = new ArrayList<>();
                        if (!guessesStr.isEmpty()) {
                            String[] guessArray = guessesStr.split(",");
                            for (String guess : guessArray) {
                                String trimmedGuess = guess.trim();
                                if (!trimmedGuess.isEmpty()) {
                                    guesses.add(trimmedGuess);
                                }
                            }
                        }
                        playerGuesses.put(playerName, guesses);
                    }
                }
                System.out.println("Loaded game state for " + playerGuesses.size() + " players.");
            }
        } catch (IOException e) {
            System.err.println("Error loading games.txt: " + e.getMessage());
        }
    }

    private void savePlayersToFile() {
        try {
            List<String> playerNames = new ArrayList<>();
            for (LeaderboardEntry entry : leaderboard) {
                playerNames.add(entry.name());
            }
            Files.write(Paths.get("game_state/players.txt"), playerNames);
        } catch (IOException e) {
            System.err.println("Error saving players.txt: " + e.getMessage());
        }
    }

    private void saveGamesToFile() {
        try {
            List<String> gameLines = new ArrayList<>();
            for (String playerName : playerHiddenWords.keySet()) {
                String hiddenWord = playerHiddenWords.get(playerName);
                List<String> guesses = playerGuesses.getOrDefault(playerName, new ArrayList<>());
                String guessesStr = String.join(",", guesses);
                gameLines.add(playerName + ":" + hiddenWord + ":" + guessesStr);
            }
            if (!gameLines.isEmpty()) {
                Files.write(Paths.get("game_state/games.txt"), gameLines);
            }
        } catch (IOException e) {
            System.err.println("Error saving games.txt: " + e.getMessage());
        }
    }

    public void printLeaderboard(Boolean byGames){
        if (leaderboard.isEmpty()) {
            System.out.println("Leaderboard is empty");
            return;
        }

        List<LeaderboardEntry> sortedLeaderboard = new ArrayList<>(leaderboard);
        
        if (byGames) {
            sortedLeaderboard.sort((a, b) -> Integer.compare(b.games().size(), a.games().size()));
        } else {
            sortedLeaderboard.sort((a, b) -> Double.compare(a.getAvgGuesses(), b.getAvgGuesses()));
        }

        int rank = 1;
        for (LeaderboardEntry entry : sortedLeaderboard) {
            int numGames = entry.games().size();
            double avgGuesses = entry.getAvgGuesses();
            String gameWord = numGames == 1 ? "game" : "games";
            System.out.println("%d. %s - %d %s - avg %.1f guesses".formatted(
                rank, entry.name(), numGames, gameWord, avgGuesses));
            rank++;
        }
    }

    private void printNewGameBoard(int wordLength) {
        System.out.println("✨ New game started ✨");
        System.out.println();

        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        String middleRow = buildRow(wordLength, CELL_EMPTY);

        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(topBottomRow);
            System.out.println(middleRow);
            System.out.println(topBottomRow);
            if (row < BOARD_ROWS - 1) {
                System.out.println();
            }
        }
    }

    private String buildRow(int wordLength, String cellPattern) {
        StringJoiner joiner = new StringJoiner("  ");
        for (int i = 0; i < wordLength; i++) {
            joiner.add(cellPattern);
        }
        return joiner.toString();
    }

    private String buildCellWithLetter(char letter) {
        return "* " + letter + " *";
    }

    private void printBoardWithGuesses(int wordLength, List<String> guesses) {
        System.out.println("📋 Game Board");
        System.out.println();

        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        List<String> guessList = new ArrayList<>(guesses);
        
        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(topBottomRow);
            
            if (row < guessList.size()) {
                String guess = guessList.get(row);
                StringJoiner guessRow = new StringJoiner("  ");
                for (int col = 0; col < wordLength; col++) {
                    if (col < guess.length()) {
                        guessRow.add(buildCellWithLetter(guess.charAt(col)));
                    } else {
                        guessRow.add(CELL_EMPTY);
                    }
                }
                System.out.println(guessRow.toString());
            } else {
                System.out.println(buildRow(wordLength, CELL_EMPTY));
            }
            
            System.out.println(topBottomRow);
            if (row < BOARD_ROWS - 1) {
                System.out.println();
            }
        }
    }
    public class CommandLineParser {
    
        public enum actions { REGISTER, NEW_GAME, GUESS, LEADERBOARD };

        public void Parse(String playerInput){
            // Normalize the player input
            var normInput = playerInput.strip().split(" ");
            // Extract the command
            var command = normInput[0].toUpperCase().replace("-","_");
            // Check if input is empty
            emptyHandler(command, playerInput);
            // Check if command exists
            commandExistsHandler(command, playerInput);

            switch (actions.valueOf(command)) {
                case REGISTER:
                    handleRegister(normInput);
                    break;
                case NEW_GAME:
                    handleNewGame(normInput);
                    break;
                case GUESS:
                    handleGuess(normInput);
                    break;
                case LEADERBOARD:
                    handleLeaderboard(normInput);
                    break;
            
                default:
                   System.err.println("Unknown command: " + command);
            }

        }

        private void commandExistsHandler(String command, String fullCommand){
            // Short Circuit if command not in actions
            boolean commandExists = Arrays.stream(actions.values()).anyMatch(action -> action.name().equals(command));
            if (!commandExists) {
               System.err.println("Unknown command: " + command);
               System.exit(1);
            }
        }
        private void emptyHandler(String input, String fullCommand){
            if (input.isEmpty()){
                // Check if input is empty
               System.err.println("Invalid command: " + fullCommand);
               System.exit(1);
            }
        }

        private void guessHandler(String input, String fullCommand){
            if (input.length() != 5){
                // Check if input is empty
                System.err.println("Invalid guess: " + fullCommand);
                System.exit(1);
            }
        }

        private void validPlayerHandler(String input, String fullCommand){
            // Invalid Player Name handling
            if (input.isEmpty() || pattern.matcher(input).matches()) {
                System.err.println("Invalid player name");
                System.exit(1);
            }
        }

        private void handleRegister(String[] normInput) {
            System.out.println("Called Register");
            if (normInput.length < 2) {
               System.err.println("usage: wurdal REGISTER <player-name>");
               System.exit(2);
            }
            // Validate the playername
            String playerName = normInput[1].strip();
            validPlayerHandler(playerName, String.join(" ", normInput));
            // Player already registered
            if (leaderboard.stream().anyMatch((entry)-> entry.name().equals(playerName))){
                System.err.println("Player already registered");
                System.exit(1);
            }
            leaderboard.add(new LeaderboardEntry(playerName, new ArrayList<Integer>(){}));
            savePlayersToFile();
        }

        private void handleNewGame(String[] normInput) {
            if (normInput.length < 2) {
               System.err.println("Invalid Arguments: NEW_GAME <player-name>");
               System.exit(1);
            }

            String playerName = normInput[1].strip();
            // validate player name
            validPlayerHandler(playerName, String.join(" ", normInput));
            // check if player is registered
            boolean playerExists = leaderboard.stream().anyMatch(entry -> entry.name().equals(playerName));
            if (!playerExists) {
               System.err.println("Player not registered: " + playerName);
            }

            playerHiddenWords.put(playerName, "");
            playerGuesses.put(playerName, new ArrayList<>());
            printNewGameBoard(DEFAULT_WORD_LENGTH);
            saveGamesToFile();
        }

        private void handleGuess(String[] normInput) {
            System.out.println("Called Guess");
            if (normInput.length < 3) {
               System.err.println("Invalid Arguments: GUESS <player-name> <word>");
               System.exit(1);
            }
            String playerName = normInput[1].strip();
            String guessWord = normInput[2].strip();

            emptyHandler(playerName, );
            guessWord()


            if (!playerGuesses.containsKey(playerName)) {
               System.err.println("No active game for player: " + playerName);
            }

            currentInput = guessWord;
            playerGuesses.get(playerName).add(guessWord);
            saveGamesToFile();
            printBoardWithGuesses(DEFAULT_WORD_LENGTH, playerGuesses.get(playerName));
        }

        private void handleLeaderboard(String[] normInput) {
            System.out.println("Called Leaderboard");
            boolean byGuesses = normInput.length > 1 && normInput[1].equals("--by-guesses");
            printLeaderboard(byGuesses);
        }
    }
    public static void main(String[] args) {
        wurdal game = new wurdal();

        if (args.length <= 0) {
            System.err.println("usage: wurdal <command>");
        }        
        String commandLine = String.join(" ", args);
        game.parser.Parse(commandLine);
        // Everything succeeded return 0
        System.exit(0);

    }
}
