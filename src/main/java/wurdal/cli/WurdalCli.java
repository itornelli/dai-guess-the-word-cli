package wurdal.cli;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.web.client.RestClientException;
import wurdal.structures.Player;
import wurdal.structures.api.*;
import wurdal.cli.ApiClient.ApiException;

public class WurdalCli {
    private final ApiClient apiClient;
    private final SessionStore sessionStore;

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    public WurdalCli() {
        this(new ApiClient(), SessionStore.getInstance());
    }

    WurdalCli(ApiClient apiClient, SessionStore sessionStore) {
        this.apiClient = apiClient;
        this.sessionStore = sessionStore;
    }

    public int run(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: wurdal <register|login|logout|board|guess> ...");
            return 1;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (command) {
                case "register" -> handleRegister(args);
                case "login" -> handleLogin(args);
                case "logout" -> handleLogout();
                case "board" -> handleBoard();
                case "guess" -> handleGuess(args);
                case "leaderboard" -> handleLeaderboard();
                default -> unknownCommand(command);
            };
        } catch (ApiException e) {
            printApiError(e);
            return e.statusCode() == 401 ? 1 : 2;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
             return 3;
        }
    }

    private int handleLeaderboard() throws ApiException {
        try {
            LeaderBoard leaderBoard = apiClient.leaderboard();
            if (leaderBoard.players() == null || leaderBoard.players().isEmpty()) {
                System.out.println("No players on the leaderboard yet. Be the first to register!");
                return 0;
            }

            leaderBoard.players().forEach(p ->
                    System.out.println(
                            p.getName() + " with " + p.getGamesWon() + " wins, " +
                                    p.getGamesLost() + " losses, average " + p.getAverageGuesses() + " guesses"
                    )
            );
            return 0;
        } catch (RestClientException e) {
            System.out.println("Looks like the wurdal servers are taking a loss... try again later!");
            return 2;
        }
    }

    private boolean ensureServerUp() throws ApiException {
        try {
            apiClient.links();
            return true;
        } catch (RestClientException e) {
            System.err.println("Looks like the wurdal servers are taking a loss... try again later!");
            return false;
        }
    }

    private int handleRegister(String[] args) throws ApiException {
        if (!ensureServerUp()) {
            return 1;
        }

        if (args.length < 2) {
            System.err.println("Name cannot be empty.");
            System.err.println("usage: wurdal register <name>");
            return 1;
        }

        String username = args[1].trim();
        ApiResponse response = apiClient.register(username);
        if (response instanceof GenError) {
            throw new ApiException(402, (GenError) response);
        } else if (response instanceof RegisterRes) {
            if (((RegisterRes) response).sessionId() != null) {
                sessionStore.write(((RegisterRes) response).sessionId());
            }
        }

        if (!PLAYER_NAME_PATTERN.matcher(username).matches()) {
            System.err.println("Invalid player name. Try a different name.");
            return 1;
        }

        if (apiClient.getId(username) != null) {
            System.err.println("Player name already exists.");
            return 1;
        }

        System.out.println("May the odds be in your favor " + username +"!");
        return 0;
    }

    private int handleLogin(String[] args) throws ApiException {
        if (args.length < 2) {
            System.err.println("usage: wurdal login <name>");
            return 1;
        }
        String username = args[1].trim();
            AuthResponse response = apiClient.login(username);
            SessionStore.getInstance().write(response.sessionId());
            //Maybe sessionId should be renamed to Token
            //wasn't the playerId here?
            Board boardResponse = apiClient.board();
        if (boardResponse instanceof BoardResError) {
            throw new ApiException(401, (BoardResError)boardResponse);
        }
        BoardRes res = (BoardRes) boardResponse;
        if (res.user() != null) {
            printBoardResponse(res, res.user().name());
        }
        else {
            throw new RuntimeException("[WurdalCli.handleLogin]res.user() is null");
        }
        return 0;
    }

    private int handleLogout() {
        Optional<String> session = sessionStore.read();
        if (session.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        sessionStore.clear();
        return 0;
    }

    private int handleBoard() throws ApiException {
        Optional<String> session = sessionStore.read();
        if (session.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        Board response = apiClient.board();
        if (response instanceof BoardResError) {
            throw new ApiException(402, (BoardResError)response);
        }
        BoardRes res = (BoardRes)response;
        if (res.user() == null) {
            return 1;
        }
        printBoardResponse(res, res.user().name());
        return 0;
    }

    private int handleGuess(String[] args) throws ApiException {
        if (args.length < 2) {
            System.err.println("usage: wurdal guess <word>");
            return 1;
        }
        Optional<String> session = sessionStore.read();
        if (session.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        String guessWord = args[1].trim();
        Board response = apiClient.guess(guessWord);
        if (response instanceof BoardResError) {
            throw new ApiException(402, (BoardResError)response);
        }
        BoardRes res = (BoardRes)response;
        printBoardResponse(res, res.user().name());
        return 0;
    }

    private int unknownCommand(String command) {
        System.err.println("Unknown command: " + command);
        return 1;
    }

    private void printBoardResponse(BoardRes response, String displayName) {
        System.out.println("May the odds be in your favor " + displayName + "!");
        BoardRenderer.print(response);
    }

    private void printApiError(ApiException ex) {
        if (ex.error() instanceof GenError) {
            System.out.println(((GenError) ex.error()).description());
        }
        else if (ex.error() instanceof BoardResError) {
            System.out.println(((BoardResError) ex.error()).error().description());
        }
        else if (ex.error() instanceof wurdal.structures.api.ErrorResponse) {
            System.out.println(((ErrorResponse) ex.error()).message());
        }
    }
}
