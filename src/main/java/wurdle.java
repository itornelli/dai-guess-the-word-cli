import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;

public class wurdle {

    private static final int DEFAULT_WORD_LENGTH = 5;
    private static final int BOARD_ROWS = 6;
    private static final String CELL_BORDER = "*****";
    private static final String CELL_EMPTY = "*   *";
    
    public String currentInput = "";
    public Set<String> currentSeenWords = new HashSet<String>() {};


    public static void main(String[] args) {

        String currInput = "";
        try (Scanner scanner = new Scanner(System.in)) {
            while(!currInput.equals("exit") && scanner.hasNextLine()){
                currInput = scanner.nextLine().trim();

                if (currInput.equals("new-game")) {
                    printNewGameBoard(DEFAULT_WORD_LENGTH);
                    continue;
                }

                if (!currInput.equals("exit")) {
                    System.out.println("User entered: %s".formatted(currInput));
                }
            }
        }

        System.out.println("Welcome to wurdal!");
    }

    private static void printNewGameBoard(int wordLength) {
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

    private static String buildRow(int wordLength, String cellPattern) {
        StringJoiner joiner = new StringJoiner("  ");
        for (int i = 0; i < wordLength; i++) {
            joiner.add(cellPattern);
        }
        return joiner.toString();
    }
}
