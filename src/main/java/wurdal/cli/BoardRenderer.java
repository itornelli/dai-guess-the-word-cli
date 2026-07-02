package wurdal.cli;

import wurdal.structures.api.BoardRes;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public final class BoardRenderer {
    private static final int BOARD_ROWS = 6;
    private static final String CELL_BORDER = "*****";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_ORANGE = "\u001B[38;5;208m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String CELL_EMPTY = ANSI_ORANGE + "*   *" + ANSI_RESET;

    private BoardRenderer() {
    }

    public static void print(BoardRes board) {
        int wordLength = board.currentGuesses().length();
        String topBottomRow = buildRow(wordLength, CELL_BORDER);
        List<String> guessList = new ArrayList<>();
        if (board.currentGuesses() == null || board.currentGuesses().result() == null || board.currentGuesses().result().word() == null) {
            System.out.println("bad board response");
        }

        for (BoardRes.Guess guess : board.currentGuesses().guesses()) {
            String word = guess.letters().stream().map(l -> String.valueOf(l.letter())).collect(Collectors.joining());
            guessList.add(word);
        }

        String word = board.currentGuesses().result().word();
        for (int row = 0; row < BOARD_ROWS; row++) {
            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);
            if (row < guessList.size()) {
                String guess = guessList.get(row);
                String[] colorCodes = evaluateGuessColors(board.currentGuesses().result().word(), guess, wordLength);
                StringJoiner guessRow = new StringJoiner("  ");
                for (int col = 0; col < wordLength; col++) {
                    if (col < guess.length()) {
                        guessRow.add(buildColoredCell(guess.charAt(col), colorCodes[col]));
                    } else {
                        guessRow.add(CELL_EMPTY);
                    }
                }
                System.out.println(guessRow);
            } else {
                System.out.println(buildRow(wordLength, CELL_EMPTY));
            }
            System.out.println(ANSI_BLUE + topBottomRow + ANSI_RESET);
            if (row < BOARD_ROWS - 1) {
                System.out.println();
            }
        }

        String status = board.currentGuesses().result().status();
        if (status.equalsIgnoreCase("won")) {
            System.out.println("🎉 You won! 🎉");
        } else if (status.equalsIgnoreCase("lost")) {
            System.out.println("Too bad, the word was " + board.currentGuesses().result().word());
        }
    }

    private static String buildRow(int wordLength, String cellPattern) {
        StringJoiner joiner = new StringJoiner("  ");
        for (int i = 0; i < wordLength; i++) {
            joiner.add(cellPattern);
        }
        return joiner.toString();
    }

    private static String buildColoredCell(char letter, String ansiColor) {
        return ansiColor + "* " + letter + " *" + ANSI_RESET;
    }

    private static String[] evaluateGuessColors(String hiddenWord, String guess, int wordLength) {
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
}
