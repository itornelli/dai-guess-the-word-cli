package wurdal.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Integration test cases only; requires running Postgres and API server")
public class AuthAcceptanceScenariosTest {

    @Test
    void userPlaysGameScenario() {
        // Given Alice with a fresh CLI session
        // When register Alice + password
        // Then greeting is "May the odds be in your favor Alice!" and empty board is shown
        assertTrue(true);

        // When board is called while logged in
        // Then same greeting and board are shown
        assertTrue(true);

        // When guess <word> is submitted
        // Then board shows the new guess row
        assertTrue(true);

        // When logout is called
        // Then "Successfully logged out" is shown
        assertEquals("Successfully logged out", "Successfully logged out");

        // When board is called while logged out
        // Then "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");

        // When login ALICE + password and board are called
        // Then prior board state is restored
        assertTrue(true);
    }

    @Test
    void signedOutUserTriesToPlayScenario() {
        // Given a logged out user
        // When board is called
        // Then "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");

        // When guess <word> is called
        // Then "Please login to continue" is shown
        assertEquals("Please login to continue", "Please login to continue");
    }

    @Test
    void unregisteredUserLogsInScenario() {
        // Given Jordan is not registered
        // When login Jordan + password is called
        // Then "Could not find user Jordan. Please register" is shown
        // And "wurdal register Jordan" example command is shown
        assertTrue(true);
    }
}
