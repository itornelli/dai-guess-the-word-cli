public class colorManager {

    // ANSI color codes
    public static final String RESET  = "\u001B[0m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED    = "\u001B[31m";
    public static final String BLUE   = "\u001B[34m";
    public static final String GRAY   = "\u001B[90m";

    // Status values for letter tracking
    public static final int STATUS_UNSEEN = 0;
    public static final int STATUS_GRAY   = 1;
    public static final int STATUS_YELLOW = 2;
    public static final int STATUS_GREEN  = 3;

    public static String coloredBorder(String borderRow) {
        return BLUE + borderRow + RESET;
    }

    public static String coloredCell(char letter, int col, String secretWord) {
        String color = getCellColor(letter, col, secretWord);
        return color + "*" + RESET + " " + letter + " " + color + "*" + RESET;
    }

    public static String getCellColor(char guessChar, int col, String secretWord) {
        if (guessChar == secretWord.charAt(col)) {
            return GREEN;
        } else if (secretWord.indexOf(guessChar) >= 0) {
            return YELLOW;
        } else {
            return RED;
        }
    }

    public static void updateLetterStatus(String guess, String secretWord, int[] letterStatus) {
        for (int col = 0; col < guess.length(); col++) {
            char ch = guess.charAt(col);
            if (ch < 'A' || ch > 'Z') continue;
            int index = ch - 'A';
            int nextStatus;
            if (ch == secretWord.charAt(col))      nextStatus = STATUS_GREEN;
            else if (secretWord.indexOf(ch) >= 0)  nextStatus = STATUS_YELLOW;
            else                                    nextStatus = STATUS_GRAY;
            if (nextStatus > letterStatus[index])   letterStatus[index] = nextStatus;
        }
    }

    public static void printAvailableLetters(int[] letterStatus) {
        String[] rows = { "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" };
        System.out.println("Available letters:");
        for (String row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                int status = letterStatus[ch - 'A'];
                String color = status == STATUS_GREEN ? GREEN
                             : status == STATUS_YELLOW ? YELLOW
                             : status == STATUS_GRAY ? GRAY : "";
                line.append(color).append(ch).append(RESET);
                if (i < row.length() - 1) line.append(' ');
            }
            System.out.println(line);
        }
        System.out.println();
    }
}