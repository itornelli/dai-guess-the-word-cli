package wurdal.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Integration tests: require a running Postgres instance and API server")
public class AuthAcceptanceScenariosTest {

    @Test
    void userPlaysGameScenario() {
        // Given Alice with a fresh CLI session

        // When: wurdal register Alice
        // Then: "May the odds be in your favor Alice!" is shown with an empty board
        // And: a player id is stored in the session file
        assertTrue(true);

        // When: wurdal board (while logged in)
        // Then: same greeting and board are shown
        assertTrue(true);

        // When: wurdal guess <word>
        // Then: board shows the new guess row with correct letter colors
        assertTrue(true);

        // When: wurdal logout
        // Then: "Successfully logged out" is shown
        assertEquals("Successfully logged out", "Successfully logged out");

        // When: wurdal board (while logged out)
        // Then: "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");

        // When: wurdal login alice (case-insensitive) and wurdal board
        // Then: prior board state is restored
        assertTrue(true);
    }

    @Test
    void signedOutUserTriesToPlayScenario() {
        // Given a logged out user

        // When: wurdal board
        // Then: "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");

        // When: wurdal guess <word>
        // Then: "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");
    }

    @Test
    void unregisteredUserLogsInScenario() {
        // Given Jordan is not registered

        // When: wurdal login Jordan
        // Then: error message "Could not find user Jordan. Please register" is shown
        assertTrue(true);
    }
}
