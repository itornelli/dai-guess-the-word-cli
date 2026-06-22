package wurdal.persistence;

import java.util.List;
import java.util.Map;
import wurdal.leaderboard.LeaderboardEntry;

/**
 * Interface for persisting game state and player data.
 * 
 * Implementations can use different backends (files, databases, etc.) while
 * maintaining the same contract. This enables:
 * - Easy switching between storage backends
 * - Mock implementations for testing
 * - Future migration to databases
 * 
 * Example implementations:
 * - FileBasedPersistence (CSV files)
 * - JsonFilePersistence (JSON files)
 * - DatabasePersistence (SQL database)
 */
public interface PersistenceLayer {
    
    /**
     * Load all registered players and their game history.
     * 
     * @return list of LeaderboardEntry objects, or empty list if none exist
     */
    List<LeaderboardEntry> loadPlayers();
    
    /**
     * Save all players and their game history.
     * 
     * @param players list of LeaderboardEntry objects to persist
     */
    void savePlayers(List<LeaderboardEntry> players);
    
    /**
     * Load current game state for all players.
     * Returns a map with two keys:
     * - "hiddenWords": Map<String, String> mapping player name to their current secret word
     * - "guesses": Map<String, List<String>> mapping player name to list of guesses
     * 
     * @return map containing hiddenWords and guesses, or empty map if no games exist
     */
    Map<String, Object> loadGameState();
    
    /**
     * Save current game state for all players.
     * 
     * @param hiddenWords map of player name to their current secret word
     * @param guesses map of player name to list of guesses
     */
    void saveGameState(Map<String, String> hiddenWords, Map<String, List<String>> guesses);
    
    /**
     * Load the dictionary of words that can be picked as secret words.
     * 
     * @return list of valid secret words
     */
    List<String> loadWordDictionary();
    
    /**
     * Load the list of words that can be submitted as valid guesses.
     * 
     * @return list of valid guess words
     */
    List<String> loadGuessableWords();
}
