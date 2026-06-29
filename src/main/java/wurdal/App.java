package wurdal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wurdal.cli.WurdalCli;
import wurdal.command.CommandLineParser;
import wurdal.game.GameEngine;
import wurdal.persistence.FileBasedPersistence;
import wurdal.persistence.PersistenceLayer;

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
        if (hasSpringArgs(args)) {
            SpringApplication.run(App.class, args);
        }
            else {
            WurdalCli wurdalCli = new WurdalCli();
            wurdalCli.run(args);
        }
    }

    private static boolean hasSpringArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("server") || arg.startsWith("-D")) {
                return true;
            }
        }
        return false;
    }
}
