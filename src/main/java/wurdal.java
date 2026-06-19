import java.util;

public class wurdal {
    public static void main(String[] args) {

        String currInput = "";
        Scanner scanner = new Scanner(System.in);

        while(!currInput.equals("exit")){
            currInput = scanner.nextLine();
            System.out.println("User entered: %s".formatted(currInput));
        } 
        System.out.println("Welcome to wurdal!");
    }
}
