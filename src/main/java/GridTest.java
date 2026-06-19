import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GridTest {
    public static void main(String[] args) {
        List<String> words = loadWords();
        Set<String> validGuesses = loadGuessDictionary();

        if (words.isEmpty()) {
            System.err.println("No secret words found in words.txt.");
            return;
        }
        if (validGuesses.isEmpty()) {
            System.err.println("No guess words found in WordleDict.txt.");
            return;
        }

        String secretWord = words.get(new Random().nextInt(words.size()));
        String playerName = "[NAME]";
        int[] letterStatus = new int[26];
        
        System.out.println("\nEnter up to 6 five-letter guesses (or 'exit' to quit, 'letters' to view keyboard):");
        
        Scanner scanner = new Scanner(System.in);
        String[] guesses = new String[6];
        int guessCount = 0;
        boolean solved = false;
        boolean exited = false;
        
        // Print empty grid at start
        printGridAfterGuess(guesses, -1, secretWord);
        printAvailableLetters(letterStatus);
        
        for (int i = 0; i < 6; i++) {
            System.out.print("Guess " + (i + 1) + ": ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("EXIT")) {
                System.out.println("\nExiting game.");
                exited = true;
                break;
            } else if (input.equals("LETTERS")) {
                printAvailableLetters(letterStatus);
                i--;
                continue;
            } else if (input.equals("DONE")) {
                guesses[i] = "";
            } else if (input.length() == 5) {
                if (!validGuesses.contains(input)) {

                    System.out.println("Invalid! Guess must be a real word from Wordle Dictionary");
                    i--;
                    continue;
                }
                guesses[i] = input;
                guessCount++;
                updateLetterStatus(input, secretWord, letterStatus);
                printGridAfterGuess(guesses, i, secretWord);
                printAvailableLetters(letterStatus);
                if (input.equals(secretWord)) {
                    solved = true;
                    break;
                }
            } else {
                System.out.println("Invalid! Must be 5 letters. Try again.");
                i--;
                continue;
            }
        }

        if (!exited) {
            if (solved) {
                System.out.println("\n" + playerName + " solved it in " + guessCount + " guesses!");
            } else {
                System.out.println("\n" + playerName + " couldn't complete the challenge: " + secretWord);
            }
        }
        
        scanner.close();
    }
    
    private static List<String> loadWords() {
        List<String> words = new ArrayList<>();
        try {
            String filePath = "words.txt";
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                String word = line.trim().toUpperCase();
                if (word.length() == 5) {
                    words.add(word);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading words.txt: " + e.getMessage());
        }
        return words;
    }

    private static Set<String> loadGuessDictionary() {
        Set<String> dictionary = new HashSet<>();
        try {
            String filePath = "WordleDict.txt";
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                String word = line.trim().toUpperCase();
                if (word.length() == 5) {
                    dictionary.add(word);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading WordleDict.txt: " + e.getMessage());
        }
        return dictionary;
    }

    private static void updateLetterStatus(String guess, String secretWord, int[] letterStatus) {
        for (int col = 0; col < 5; col++) {
            char ch = guess.charAt(col);
            if (ch < 'A' || ch > 'Z') {
                continue;
            }

            int index = ch - 'A';
            int nextStatus;
            if (ch == secretWord.charAt(col)) {
                nextStatus = 3; // green
            } else if (secretWord.indexOf(ch) >= 0) {
                nextStatus = 2; // yellow
            } else {
                nextStatus = 1; // gray
            }

            if (nextStatus > letterStatus[index]) {
                letterStatus[index] = nextStatus;
            }
        }
    }

    private static void printAvailableLetters(int[] letterStatus) {
        String RESET = "\u001B[0m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";
        String GRAY = "\u001B[90m";

        String[] rows = { "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" };

        System.out.println("Available letters:");
        for (String row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                int status = letterStatus[ch - 'A'];
                String color = "";
                if (status == 3) {
                    color = GREEN;
                } else if (status == 2) {
                    color = YELLOW;
                } else if (status == 1) {
                    color = GRAY;
                }

                line.append(color).append(ch).append(RESET);
                if (i < row.length() - 1) {
                    line.append(' ');
                }
            }
            System.out.println(line);
        }
        System.out.println();
    }
    
    private static void printGridAfterGuess(String[] guesses, int currentRow, String secretWord) {
        System.out.println("\n✨ Your Grid ✨\n");
        
        String BLUE = "\u001B[34m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";
        String RED = "\u001B[31m";
        String RESET = "\u001B[0m";
        
        for (int row = 0; row < 6; row++) {
            String guess = guesses[row];
            
            // Top border (blue)
            System.out.println(BLUE + "*****  *****  *****  *****  *****" + RESET);
            
            // Middle row with letters and colors
            String line = "";
            for (int col = 0; col < 5; col++) {
                String letter = " ";
                String color = "";
                
                if (guess != null && guess.length() > col) {
                    letter = String.valueOf(guess.charAt(col));
                    
                    // Determine color based on Wordle rules
                    if (letter.equals(String.valueOf(secretWord.charAt(col)))) {
                        color = GREEN;  // Correct position
                    } else if (secretWord.contains(letter)) {
                        color = YELLOW; // Wrong position
                    } else {
                        color = RED;    // Not in word
                    }
                }
                
                line += color + "*" + RESET + " " + letter + " " + color + "*" + RESET;
                if (col < 4) line += "  ";
            }
            System.out.println(line);
            
            // Bottom border (blue)
            System.out.println(BLUE + "*****  *****  *****  *****  *****" + RESET);
            System.out.println();
        }
    }
}
