package wurdal.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import wurdal.command.CommandLineParser;
import wurdal.leaderboard.LeaderboardEntry;
import wurdal.persistence.PersistenceLayer;

/**
 * Core game engine managing game state, players, and board rendering.
 * 
 * Responsibilities:
 * - Manage current game state (hidden words, player guesses)
 * - Manage player leaderboard
 * - Render the game board with colors
 * - Persist state via injected PersistenceLayer
 * 
 * This class uses dependency injection for the persistence layer,
 * allowing swapping between file-based, JSON, database, etc.
 */
@Component
public class GameEngine {

    public static final int BOARD_ROWS = 6;
    public static final String CELL_BORDER = "*****";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_ORANGE = "\u001B[38;5;208m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String CELL_EMPTY = ANSI_ORANGE + "*   *" + ANSI_RESET;

    // Game state
    public Map<String, String> playerHiddenWords = new HashMap<>();
    public Map<String, List<String>> playerGuesses = new HashMap<>();
    public Map<String, Set<String>> playerSeenWords = new HashMap<>();
    public List<String> wordDictionary = new ArrayList<>();
    public List<String> guessableWords = new ArrayList<>();
    public List<LeaderboardEntry> leaderboard = new ArrayList<>();

    // Dependencies
    public final CommandLineParser parser;
    private final PersistenceLayer persistence;
    public Random random = new Random();
    public Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    /**
     * Constructor with dependency injection.
     * 
     * @param parser command parser
     * @param persistence storage layer implementation
     */
    public GameEngine(CommandLineParser parser, PersistenceLayer persistence) {
        this.parser = parser;
        this.persistence = persistence;
        loadGameData();
    }

    /**
     * Constructor with default file-based persistence.
     * 
     * @param parser command parser
     */
    public GameEngine(CommandLineParser parser) {
        this(parser, new wurdal.persistence.FileBasedPersistence());
    }

    /**
     * Constructor with default parser and file-based persistence.
     */
    public GameEngine() {
        this(new CommandLineParser());
    }

    private void loadGameData() {
        leaderboard = persistence.loadPlayers();
        
        Map<String, Object> gameState = persistence.loadGameState();
        @SuppressWarnings("unchecked")
        Map<String, String> hiddenWords = (Map<String, String>) gameState.get("hiddenWords");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> guesses = (Map<String, List<String>>) gameState.get("guesses");
        
        playerHiddenWords = hiddenWords != null ? hiddenWords : new HashMap<>();
        playerGuesses = guesses != null ? guesses : new HashMap<>();
        
        wordDictionary = persistence.loadWordDictionary();
        guessableWords = persistence.loadGuessableWords();
    }

    public void savePlayersToFile() {
        persistence.savePlayers(leaderboard);
    }

    public void saveGamesToFile() {
        persistence.saveGameState(playerHiddenWords, playerGuesses);
    }

    public boolean hasSeenAllWords(String playerName) {
        if (guessableWords.isEmpty()) {
            return true;
        }

        Set<String> currentSeenWords = playerSeenWords.getOrDefault(playerName, Set.of());
        return currentSeenWords.size() >= guessableWords.size();
    }

    public String chooseRandomWord(String playerName) {
        if (guessableWords.isEmpty()) {
            throw new IllegalStateException("No words available in dictionary");
        }

        Set<String> currentSeenWords = playerSeenWords.computeIfAbsent(playerName, k -> new HashSet<>());

        if (hasSeenAllWords(playerName)) {
            throw new IllegalStateException("No words available for player");
        }
        
        // Keep guessing words until the random next word is not in the currentSeenWords for the player
        String chosenWord;
        do {
            chosenWord = guessableWords.get(random.nextInt(guessableWords.size()));
        } while (currentSeenWords.contains(chosenWord) && currentSeenWords.size() < guessableWords.size());

        currentSeenWords.add(chosenWord);
        return chosenWord;
    }

    // Terminal/UI Functions
    public void printLeaderboard(Boolean byGames) {
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

    public void printNewGameBoard(int wordLength) {
        System.out.println("✨ New game started ✨");
        System.out.println();

        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        String middleRow = buildRow(wordLength, CELL_EMPTY);

        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);
            System.out.println(ANSI_ORANGE + middleRow + ANSI_RESET);
            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);
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

    public String[] evaluateGuessColors(String hiddenWord, String guess, int wordLength) {
        String normalizedHiddenWord = hiddenWord.toLowerCase();
        String normalizedGuess = guess.toLowerCase();
        String[] colorCodes = new String[wordLength];
        int[] letterCounts = new int[26];

        for (int i = 0; i < normalizedHiddenWord.length(); i++) {
            char hiddenLetter = normalizedHiddenWord.charAt(i);
            if (hiddenLetter >= 'a' && hiddenLetter <= 'z') {
                letterCounts[hiddenLetter - 'a']++;
            }
        }

        for (int i = 0; i < wordLength; i++) {
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

        for (int i = 0; i < wordLength; i++) {
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

    public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses) {
        System.out.println("📋 Game Board");
        System.out.println();

        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        List<String> guessList = new ArrayList<>(guesses);

        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);

            if (row < guessList.size()) {
                String guess = guessList.get(row);
                String[] colorCodes = evaluateGuessColors(hiddenWord, guess, guess.length());
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

            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);
            if (row < BOARD_ROWS - 1) {
                System.out.println();
            }
        }
    }
}
