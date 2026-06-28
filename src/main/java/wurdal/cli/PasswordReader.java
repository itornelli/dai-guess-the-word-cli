package wurdal.cli;

import java.io.Console;
import java.util.Scanner;

public class PasswordReader {
    public String readPassword(String username) {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("Password for %s: ", username);
            return password == null ? "" : String.valueOf(password);
        }

        System.out.print("Password for " + username + ": ");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}
