package wurdal.game;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class GameEngine {

    public static final int BOARD_ROWS = 6;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    private final List<String> wordDictionary;
    private final Map<String, Set<String>> playerSeenWords = new HashMap<>();
    private final Random random = new Random();

    public GameEngine() {
        this.wordDictionary = loadWordsFromClasspath();
    }

    private List<String> loadWordsFromClasspath() {
        List<String> words = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("words.txt")) {
            if (is == null) {
                throw new IllegalStateException("words.txt not found in classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load words.txt", e);
        }
        return words;
    }

    public String chooseRandomWord(String playerName) {
        if (wordDictionary.isEmpty()) {
            throw new IllegalStateException("No words available in dictionary");
        }

        Set<String> seenWords = playerSeenWords.computeIfAbsent(playerName, k -> new HashSet<>());

        if (seenWords.size() >= wordDictionary.size()) {
            throw new IllegalStateException("No words available for player");
        }

        String chosen;
        do {
            chosen = wordDictionary.get(random.nextInt(wordDictionary.size()));
        } while (seenWords.contains(chosen));

        seenWords.add(chosen);
        return chosen;
    }

    public String[] evaluateGuessColors(String hiddenWord, String guess, int wordLength) {
        String normalizedHiddenWord = hiddenWord.toLowerCase();
        String normalizedGuess = guess.toLowerCase();
        String[] colorCodes = new String[wordLength];
        int[] letterCounts = new int[26];

        for (int i = 0; i < normalizedHiddenWord.length(); i++) {
            char c = normalizedHiddenWord.charAt(i);
            if (c >= 'a' && c <= 'z') {
                letterCounts[c - 'a']++;
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
            if (i >= normalizedGuess.length() || ANSI_GREEN.equals(colorCodes[i])) {
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
}
