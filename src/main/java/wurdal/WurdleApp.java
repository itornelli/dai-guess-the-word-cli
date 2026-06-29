package wurdal;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import wurdal.cli.WurdalCli;

/**
 * Main entry point for the Wordle CLI application.
 * 
 * Sets up dependency injection:
 * - PersistenceLayer: File-based storage (can be swapped for DB, JSON, etc.)
 * - CommandLineParser: CLI command routing
 * - GameEngine: Core game logic
 */

public class WurdleApp {

    public static void main(String[] args) {
        WurdalCli wurdalCli = new WurdalCli();
        wurdalCli.run(args);
    }
}
