package wurdal.persistence;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import wurdal.leaderboard.LeaderboardEntry;

/**
 * File-based persistence implementation using CSV format.
 * 
 * Stores:
 * - Players and game history: game_state/players.txt (CSV: name,game1,game2,...)
 * - Current games: game_state/games.txt (CSV: name:hiddenWord:guess1,guess2,...)
 * - Secret words: word_bank/wordle_dict.txt (one word per line)
 * - Valid guesses: word_bank/valid_words.txt (one word per line)
 * 
 * Future implementations can replace this with JSON, database, or other formats.
 */
public class FileBasedPersistence implements PersistenceLayer {
    
    private static final String PLAYERS_FILE = "game_state/players.txt";
    private static final String GAMES_FILE = "game_state/games.txt";
    private static final String WORD_DICT_FILE = "word_bank/wordle_dict.txt";
    private static final String VALID_WORDS_FILE = "word_bank/valid_words.txt";
    
    private static final String GAME_STATE_DIR = "game_state";
    private static final String WORD_BANK_DIR = "word_bank";

    @Override
    public List<LeaderboardEntry> loadPlayers() {
        List<LeaderboardEntry> players = new ArrayList<>();
        try {
            if (Files.exists(Paths.get(PLAYERS_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(PLAYERS_FILE));
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        String[] parts = trimmedLine.split(",");
                        String playerName = parts[0].trim();
                        ArrayList<Integer> games = new ArrayList<>();

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
                            players.add(new LeaderboardEntry(playerName, games));
                        }
                    }
                }
                System.out.println("Loaded " + players.size() + " registered players.");
            } else {
                createFile(GAME_STATE_DIR, PLAYERS_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error loading players.txt: " + e.getMessage());
        }
        return players;
    }

    @Override
    public void savePlayers(List<LeaderboardEntry> players) {
        try {
            List<String> lines = new ArrayList<>();
            for (LeaderboardEntry entry : players) {
                StringJoiner joiner = new StringJoiner(",");
                joiner.add(entry.name());
                for (Integer guessesForGame : entry.games()) {
                    joiner.add(String.valueOf(guessesForGame));
                }
                lines.add(joiner.toString());
            }
            Files.write(Paths.get(PLAYERS_FILE), lines);
        } catch (IOException e) {
            System.err.println("Error saving players.txt: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> loadGameState() {
        Map<String, Object> gameState = new HashMap<>();
        Map<String, String> hiddenWords = new HashMap<>();
        Map<String, List<String>> guesses = new HashMap<>();

        try {
            if (Files.exists(Paths.get(GAMES_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(GAMES_FILE));
                for (String line : lines) {
                    String[] parts = line.split(":", -1);
                    if (parts.length >= 3) {
                        String playerName = parts[0].trim();
                        String hiddenWord = parts[1].trim();
                        String guessesStr = parts[2].trim();

                        hiddenWords.put(playerName, hiddenWord);

                        List<String> guessList = new ArrayList<>();
                        if (!guessesStr.isEmpty()) {
                            String[] guessArray = guessesStr.split(",");
                            for (String guess : guessArray) {
                                String trimmedGuess = guess.trim();
                                if (!trimmedGuess.isEmpty()) {
                                    guessList.add(trimmedGuess);
                                }
                            }
                        }
                        guesses.put(playerName, guessList);
                    }
                }
                System.out.println("Loaded game state for " + guesses.size() + " players.");
            } else {
                createFile(GAME_STATE_DIR, GAMES_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error loading games.txt: " + e.getMessage());
        }

        gameState.put("hiddenWords", hiddenWords);
        gameState.put("guesses", guesses);
        return gameState;
    }

    @Override
    public void saveGameState(Map<String, String> hiddenWords, Map<String, List<String>> guesses) {
        try {
            List<String> lines = new ArrayList<>();
            for (String playerName : hiddenWords.keySet()) {
                String hiddenWord = hiddenWords.get(playerName);
                List<String> playerGuesses = guesses.getOrDefault(playerName, new ArrayList<>());
                String guessesStr = String.join(",", playerGuesses);
                lines.add(playerName + ":" + hiddenWord + ":" + guessesStr);
            }
            if (!lines.isEmpty()) {
                Files.write(Paths.get(GAMES_FILE), lines);
            }
        } catch (IOException e) {
            System.err.println("Error saving games.txt: " + e.getMessage());
        }
    }

    @Override
    public List<String> loadWordDictionary() {
        return loadWordFile(WORD_DICT_FILE, "word dictionary");
    }

    @Override
    public List<String> loadGuessableWords() {
        return loadWordFile(VALID_WORDS_FILE, "guessable words");
    }

    private List<String> loadWordFile(String filePath, String fileDescription) {
        List<String> words = new ArrayList<>();
        try {
            if (Files.exists(Paths.get(filePath))) {
                List<String> lines = Files.readAllLines(Paths.get(filePath));
                for (String line : lines) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                }
                if (words.isEmpty()) {
                    System.err.println(fileDescription + " is empty at " + filePath);
                }
            } else {
                String dir = filePath.contains("word_bank") ? WORD_BANK_DIR : GAME_STATE_DIR;
                createFile(dir, filePath);
            }
        } catch (IOException e) {
            System.err.println("Error loading " + filePath + ": " + e.getMessage());
        }
        return words;
    }

    private void createFile(String directory, String filePath) {
        try {
            Files.createDirectories(Paths.get(directory));
            Files.createFile(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Error creating " + filePath + ": " + e.getMessage());
        }
    }
}
