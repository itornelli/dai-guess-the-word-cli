package wurdal;

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
public class App {
    
    public static void main(String[] args) {
        // Initialize persistence layer (swap this for different storage backends)
        PersistenceLayer persistence = new FileBasedPersistence();
        
        // Initialize command parser and game engine
        CommandLineParser parser = new CommandLineParser();
        GameEngine game = new GameEngine(parser, persistence);

        if (args.length <= 0) {
            System.err.println("usage: wurdal <command>");
        }        
        
        String commandLine = String.join(" ", args);
        game.parser.Parse(game, commandLine);
        
        // Everything succeeded return 0
        System.exit(0);
    }
}
