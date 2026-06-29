package wurdal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wurdal.cli.WurdalCli;

/** * Main entry point for the Wordle CLI application. * * - With CLI args (register, login, guess, etc.): runs as CLI client * - Without args or with Spring flags (--): starts the Spring Boot server */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].startsWith("--")) {
            WurdalCli cli = new WurdalCli();
            System.exit(cli.run(args));
        } else {
            SpringApplication.run(App.class, args);
        }
    }
}