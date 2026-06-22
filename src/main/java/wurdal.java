import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;


public class wurdal {

    static final int DEFAULT_WORD_LENGTH = 5;
    static final int BOARD_ROWS = 6;
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
    public final CommandLineParser parser;
    public Random random = new Random();

    public Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    public wurdal(CommandLineParser parser) {
        this.parser = parser;
        loadPlayersFromFile();
        loadGamesFromFile();
        loadWordDictionary();
        loadGuessableWords();
    }

    public wurdal() {
        this(new CommandLineParser());
    }

    // Pull game state from files
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

    String chooseRandomWord() {
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

    // Persistence Functions
    void savePlayersToFile() {
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

    void saveGamesToFile() {
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

    // Terminal Functions
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

    void printNewGameBoard(int wordLength) {
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

    void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses) {
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
    public static void main(String[] args) {
        wurdal game = new wurdal(new CommandLineParser());

        if (args.length <= 0) {
            System.err.println("usage: wurdal <command>");
        }        
        String commandLine = String.join(" ", args);
        game.parser.Parse(game, commandLine);
        // Everything succeeded return 0
        System.exit(0);

    }
}
