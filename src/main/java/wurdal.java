import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import javax.management.openmbean.ArrayType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;


public class wurdal {

    private static final int DEFAULT_WORD_LENGTH = 5;
    private static final int BOARD_ROWS = 6;
    private static final String CELL_BORDER = "*****";
    private static final String CELL_EMPTY = "*   *";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    public String currentInput = "";
    public Map<String, String> playerHiddenWords = new HashMap<>();
    public Map<String, List<String>> playerGuesses = new HashMap<>();
    public Set<String> currentSeenWords = new HashSet<String>() {};
    public Set<String> currentGames = new HashSet<String>(){};
    public List<String> wordDictionary = new ArrayList<String>(){};
    public List<String> guessableWords = new ArrayList<String>(){};
    public List<LeaderboardEntry> leaderboard = new ArrayList<LeaderboardEntry>(){};
    public CommandLineParser parser = new CommandLineParser(){};
    public Random random = new Random();

    public Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    public wurdal() {
        loadPlayersFromFile();
        loadGamesFromFile();
        loadWordDictionary();
        loadGuessableWords();
    }

    private void loadWordDictionary() {
        try {
            if (Files.exists(Paths.get("word_bank/wordle_dict.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("word_bank/wordle_dict.txt"));
                for (String line : lines) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        wordDictionary.add(word);
                    }
                }
                if (wordDictionary.isEmpty()) {
                    System.err.println("Word dictionary is empty or missing at word_bank/wordle_dict.txt");
                }
            }else{
                Files.createDirectories(Paths.get("word_bank"));
                Files.createFile(Paths.get("word_bank/wordle_dict.txt"));
            }
        } catch (IOException e) {
            System.err.println("Error loading words.txt: " + e.getMessage());
        }
    }

    private void loadGuessableWords() {
        try {
            if (Files.exists(Paths.get("word_bank/valid_words.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("word_bank/valid_words.txt"));
                for (String line : lines) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        guessableWords.add(word);
                    }
                }
                if (guessableWords.isEmpty()) {
                    System.err.println("Guessable word bank is empty or missing at word_bank/valid_words.txt");
                }
            }else{
                Files.createDirectories(Paths.get("word_bank"));
                Files.createFile(Paths.get("word_bank/valid_words.txt"));
            }
        } catch (IOException e) {
            System.err.println("Error loading valid_words.txt: " + e.getMessage());
        }
    }

    private String chooseRandomWord() {
        if (guessableWords.isEmpty()) {
            throw new IllegalStateException("No words available in dictionary");
        }

        if (currentSeenWords.size() >= guessableWords.size()) {
            currentSeenWords.clear();
        }

        String chosenWord;
        do {
            chosenWord = guessableWords.get(random.nextInt(guessableWords.size()));
        } while (currentSeenWords.contains(chosenWord) && currentSeenWords.size() < guessableWords.size());

        currentSeenWords.add(chosenWord);
        return chosenWord;
    }

    private void loadPlayersFromFile() {
        try {
            if (Files.exists(Paths.get("game_state/players.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("game_state/players.txt"));
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        String[] parts = trimmedLine.split(",");
                        String playerName = parts[0].trim();
                        ArrayList<Integer> games = new ArrayList<Integer>();

                        for (int i = 1; i < parts.length; i++) {
                            String gameValue = parts[i].trim();
                            if (!gameValue.isEmpty()) {
                                try {
                                    games.add(Integer.parseInt(gameValue));
                                } catch (NumberFormatException e) {
                                    System.err.println("Skipping invalid game value for player " + playerName + ": " + gameValue);
                                }
                            }
                        }

                        if (!playerName.isEmpty()) {
                            leaderboard.add(new LeaderboardEntry(playerName, games));
                        }
                    }
                }
                System.out.println("Loaded " + leaderboard.size() + " registered players.");
            } else {
                Files.createDirectories(Paths.get("game_state"));
                Files.createFile(Paths.get("game_state/players.txt"));
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
            }else{
                Files.createDirectories(Paths.get("game_state"));
                Files.createFile(Paths.get("game_state/games.txt"));
            }
        } catch (IOException e) {
            System.err.println("Error loading games.txt: " + e.getMessage());
        }
    }

    private void savePlayersToFile() {
        try {
            List<String> playerNames = new ArrayList<String>();
            for (LeaderboardEntry entry : leaderboard) {
                StringJoiner joiner = new StringJoiner(",");
                joiner.add(entry.name());
                for (Integer guessesForGame : entry.games()) {
                    joiner.add(String.valueOf(guessesForGame));
                }
                playerNames.add(joiner.toString());
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

    private int getLeaderboardIndexByPlayerName(String playerName) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).name().equals(playerName)) {
                return i;
            }
        }
        return -1;
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

    private String buildColoredCell(char letter, String ansiColor) {
        return ansiColor + "* " + letter + " *" + ANSI_RESET;
    }

    private String[] evaluateGuessColors(String hiddenWord, String guess) {
        String normalizedHiddenWord = hiddenWord.toLowerCase();
        String normalizedGuess = guess.toLowerCase();
        String[] colorCodes = new String[DEFAULT_WORD_LENGTH];
        int[] letterCounts = new int[26];

        for (int i = 0; i < normalizedHiddenWord.length(); i++) {
            char hiddenLetter = normalizedHiddenWord.charAt(i);
            if (hiddenLetter >= 'a' && hiddenLetter <= 'z') {
                letterCounts[hiddenLetter - 'a']++;
            }
        }

        for (int i = 0; i < DEFAULT_WORD_LENGTH; i++) {
            if (i >= normalizedGuess.length()) {
                colorCodes[i] = ANSI_RESET;
                continue;
            }

            char guessLetter = normalizedGuess.charAt(i);
            if (i < normalizedHiddenWord.length() && guessLetter == normalizedHiddenWord.charAt(i)) {
                colorCodes[i] = ANSI_GREEN;
                if (guessLetter >= 'a' && guessLetter <= 'z') {
                    letterCounts[guessLetter - 'a']--;
                }
            } else {
                colorCodes[i] = ANSI_RESET;
            }
        }

        for (int i = 0; i < DEFAULT_WORD_LENGTH; i++) {
            if (i >= normalizedGuess.length()) {
                continue;
            }

            if (ANSI_GREEN.equals(colorCodes[i])) {
                continue;
            }

            char guessLetter = normalizedGuess.charAt(i);
            if (guessLetter >= 'a' && guessLetter <= 'z' && letterCounts[guessLetter - 'a'] > 0) {
                colorCodes[i] = ANSI_YELLOW;
                letterCounts[guessLetter - 'a']--;
            }
        }

        return colorCodes;
    }

    private void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses) {
        System.out.println("📋 Game Board");
        System.out.println();

        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        List<String> guessList = new ArrayList<>(guesses);
        
        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(topBottomRow);
            
            if (row < guessList.size()) {
                String guess = guessList.get(row);
                String[] colorCodes = evaluateGuessColors(hiddenWord, guess);
                StringJoiner guessRow = new StringJoiner("  ");
                for (int col = 0; col < wordLength; col++) {
                    if (col < guess.length()) {
                        guessRow.add(buildColoredCell(guess.charAt(col), colorCodes[col]));
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

        private void guessHandler(String input, String playerName, String fullCommand){
            // Validate the playerName
            validPlayerHandler(playerName, String.join(" ", fullCommand));
            // Check if the player has a game active
            if (!playerGuesses.containsKey(playerName)) {
                System.err.println("No active game for player: " + playerName);
                System.exit(1);
            }
            // Check if the guess is the word length
            if (input.length() != DEFAULT_WORD_LENGTH || !wordDictionary.contains(input)){
                // Check if input is empty
                System.err.println("Invalid guess [%s]".formatted(input));
                System.exit(1);
            }
            // Check if the player has already guessed the word
            if (playerGuesses.get(playerName).contains(input)){
                // Check if input is empty
                System.err.println("Player has already guessed [%s]".formatted(input));
                System.exit(1);
            }
            // Regardless if they got it or not save the guess
            playerGuesses.get(playerName).add(input);

            // Print the board
            printBoardWithGuesses(DEFAULT_WORD_LENGTH, playerHiddenWords.get(playerName), playerGuesses.get(playerName));
            // Check if the player has correctly guessed the word / or has reached max attempts
            var correctlyGuessed = input.equals(playerHiddenWords.get(playerName));
            var outOfGuesses = playerGuesses.get(playerName).stream().count() >= BOARD_ROWS;
            if (correctlyGuessed || outOfGuesses){
                var currNumOfGuesses = playerGuesses.get(playerName).stream().count();
                if (correctlyGuessed){
                    System.out.println("player [%s] guessed the word in [%d] guesses".formatted(playerName,currNumOfGuesses));
                }
                else{
                    System.out.println("player [%s] DID NOT guess the word [%s]".formatted(playerName,playerHiddenWords.get(playerName)));
                }
                int leaderboardIdx = getLeaderboardIndexByPlayerName(playerName);
                ArrayList<Integer> currGames = leaderboard.get(leaderboardIdx).games();
                // Add New additional game
                Integer newGame = (int) (input.equals(playerHiddenWords.get(playerName)) ? currNumOfGuesses : -1); 
                currGames.add(newGame);
                leaderboard.set(leaderboardIdx,new LeaderboardEntry(playerName,currGames));
                savePlayersToFile();
            }
        }

        private void validPlayerHandler(String input, String fullCommand){
            // Invalid Player Name handling
            if (input.isEmpty() || pattern.matcher(input).find()) {
                System.err.println("Invalid player name");
                System.exit(1);
            }
        }

        private void handleRegister(String[] normInput) {
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
            validPlayerHandler(playerName, String.join(" ", normInput));
            // check if player is registered
            boolean playerExists = leaderboard.stream().anyMatch(entry -> entry.name().equals(playerName));
            if (!playerExists) {
               System.err.println("Player not registered: " + playerName);
                    System.exit(1);
            }

            playerHiddenWords.put(playerName, chooseRandomWord());
            playerGuesses.put(playerName, new ArrayList<>());
            printNewGameBoard(DEFAULT_WORD_LENGTH);
            saveGamesToFile();
        }

        private void handleGuess(String[] normInput) {
            if (normInput.length < 3) {
               System.err.println("Invalid Arguments: GUESS <player-name> <word>");
               System.exit(1);
            }
            String playerName = normInput[1].strip();
            String guessWord = normInput[2].strip();
            guessHandler(guessWord, playerName, String.join(" ", normInput));
            saveGamesToFile();
        }

        private void handleLeaderboard(String[] normInput) {
            boolean byGuesses = normInput.length > 1 && normInput[1].equals("--by-guesses");
            System.out.println("Sorted by: %s".formatted(byGuesses ? "by-guesses" : "by-num-of-games"));
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
