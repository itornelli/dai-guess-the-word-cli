import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class wurdal {
    
    public String currentInput = "";
    public Set<String> currentSeenWords = new HashSet<String>() {};


    public static void main(String[] args) {
        
        String currInput = "";
        try (Scanner scanner = new Scanner(System.in)) {
            while(!currInput.equals("exit") && scanner.hasNextLine()){
                currInput = scanner.nextLine();
                System.out.println("User entered: %s".formatted(currInput));
            }
        }

        System.out.println("Welcome to wurdal!");
    }
}
