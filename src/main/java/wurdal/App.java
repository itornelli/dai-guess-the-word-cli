package wurdal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wurdal.command.CommandLineParser;
import wurdal.game.GameEngine;
import wurdal.persistence.FileBasedPersistence;
import wurdal.persistence.PersistenceLayer;
import java.util.Arrays;
import wurdal.cli.WurdalCli;

/**
 * Main entry point for the Wordle CLI application.
 * 
 * Sets up dependency injection:
 * - PersistenceLayer: File-based storage (can be swapped for DB, JSON, etc.)
 * - CommandLineParser: CLI command routing
 * - GameEngine: Core game logic
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        if (args.length == 0 || hasSpringArgs(args)) {
            SpringApplication.run(App.class, args);
        }
      else {
            runCLI(args);
        }
    }

    private static void runCLI(String[] args) {
        PersistenceLayer persistence = new FileBasedPersistence();
        CommandLineParser parser = new CommandLineParser();
        GameEngine game = new GameEngine(parser, persistence);

        String commandLine = String.join(" ", args);
        game.parser.Parse(game, commandLine);
        System.exit(0);
    }

    private static boolean hasSpringArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--") || arg.startsWith("-D")) {
                return true;
            }
        }
        return false;
    }
}
