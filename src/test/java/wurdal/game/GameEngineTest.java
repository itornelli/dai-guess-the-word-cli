package wurdal.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import wurdal.command.CommandLineParser;
import wurdal.persistence.PersistenceLayer;
import wurdal.leaderboard.LeaderboardEntry;
import java.util.*;

/**
 * Test suite for GameEngine core logic.
 * Tests game state management, word selection, and board rendering.
 */
public class GameEngineTest {
    
    private GameEngine game;
    
    @BeforeEach
    public void setUp() {
        // Use mock persistence that returns test data
        PersistenceLayer mockPersistence = new MockPersistenceLayer();
        game = new GameEngine(new CommandLineParser(), mockPersistence);
    }
    
    @Test
    public void testGameEngineInitialization() {
        assertNotNull(game);
        assertNotNull(game.parser);
        assertNotNull(game.wordDictionary);
        assertNotNull(game.guessableWords);
        assertNotNull(game.leaderboard);
    }
    
    @Test
    public void testChooseRandomWord() {
        String word = game.chooseRandomWord("alice");
        assertNotNull(word);
        assertTrue(game.guessableWords.contains(word));
    }
    
    @Test
    public void testChooseRandomWordTracksSeenWords() {
        Set<String> seenWords = new HashSet<>();
        for (int i = 0; i < game.guessableWords.size(); i++) {
            String word = game.chooseRandomWord("alice");
            assertTrue(seenWords.add(word), "Word should not repeat: " + word);
        }
    }
    
    @Test
    public void testChooseRandomWordThrowsAfterAllSeen() {
        // Choose all words
        for (int i = 0; i < game.guessableWords.size(); i++) {
            game.chooseRandomWord("alice");
        }

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> game.chooseRandomWord("alice")
        );
        assertEquals("No words available for player", exception.getMessage());
    }

    @Test
    public void testChooseRandomWordTracksSeenWordsPerPlayer() {
        Set<String> aliceSeenWords = new HashSet<>();
        Set<String> bobSeenWords = new HashSet<>();

        for (int i = 0; i < game.guessableWords.size(); i++) {
            assertTrue(aliceSeenWords.add(game.chooseRandomWord("alice")));
            assertTrue(bobSeenWords.add(game.chooseRandomWord("bob")));
        }

        assertEquals(game.guessableWords.size(), aliceSeenWords.size());
        assertEquals(game.guessableWords.size(), bobSeenWords.size());
    }
    
    @Test
    public void testDefaultGameConstants() {
        assertEquals(6, GameEngine.BOARD_ROWS);
    }
    
    /**
     * Mock persistence layer for testing with controlled data.
     */
    static class MockPersistenceLayer implements PersistenceLayer {
        @Override
        public List<LeaderboardEntry> loadPlayers() {
            return new ArrayList<>();
        }
        
        @Override
        public void savePlayers(List<LeaderboardEntry> players) {
        }
        
        @Override
        public Map<String, Object> loadGameState() {
            Map<String, Object> state = new HashMap<>();
            state.put("hiddenWords", new HashMap<String, String>());
            state.put("guesses", new HashMap<String, List<String>>());
            return state;
        }
        
        @Override
        public void saveGameState(Map<String, String> hiddenWords, Map<String, List<String>> guesses) {
        }
        
        private static final List<String> WORDS = List.of("stone", "crane", "stole", "steal", "melon", "cat", "jumble");

        @Override
        public List<String> loadWordDictionary() {
            return WORDS;
        }
        
        @Override
        public List<String> loadGuessableWords() {
            return WORDS;
        }
    }
}
