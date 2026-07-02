package wurdal.cli;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

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
            System.err.println("usage: wurdal <register|login|logout|board|guess|leaderboard> ...");
            return 1;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (command) {
                case "register"    -> handleRegister(args);
                case "login"       -> handleLogin(args);
                case "logout"      -> handleLogout();
                case "board"       -> handleBoard();
                case "guess"       -> handleGuess(args);
                case "leaderboard" -> handleLeaderboard();
                default            -> unknownCommand(command);
            };
        } catch (ApiException e) {
            printApiError(e);
            return e.statusCode() == 401 || e.statusCode() == 403 ? 1 : 2;
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            return 3;
        }
    }

    private int handleRegister(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: wurdal register <name>");
            return 1;
        }
        String username = args[1].trim();
        if (!PLAYER_NAME_PATTERN.matcher(username).matches()) {
            System.err.println("Invalid player name. Try a different name.");
            return 1;
        }
        RegisterRes response = apiClient.register(username);
        sessionStore.write(String.valueOf(response.id()));
        System.out.println("May the odds be in your favor " + response.name() + "!");
        Board board = apiClient.board(response.id());
        if (board instanceof BoardRes res) {
            BoardRenderer.print(res);
        }
        return 0;
    }

    private int handleLogin(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: wurdal login <name>");
            return 1;
        }
        String username = args[1].trim();
        AuthResponse response = apiClient.login(username);
        sessionStore.write(String.valueOf(response.id()));
        Board board = apiClient.board(response.id());
        if (board instanceof BoardRes res) {
            printBoardResponse(res);
        }
        return 0;
    }

    private int handleLogout() {
        if (sessionStore.read().isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        sessionStore.clear();
        System.out.println("Successfully logged out");
        return 0;
    }

    private int handleBoard() {
        Optional<Integer> playerId = readPlayerId();
        if (playerId.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        Board response = apiClient.board(playerId.get());
        if (response instanceof BoardRes res) {
            printBoardResponse(res);
        }
        return 0;
    }

    private int handleGuess(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: wurdal guess <word>");
            return 1;
        }
        Optional<Integer> playerId = readPlayerId();
        if (playerId.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        String guessWord = args[1].trim();
        Board response = apiClient.guess(playerId.get(), guessWord);
        if (response instanceof BoardRes res) {
            printBoardResponse(res);
        }
        return 0;
    }

    private int handleLeaderboard() {
        LeaderBoard leaderboard = apiClient.leaderboard();
        if (leaderboard.players().isEmpty()) {
            System.out.println("No players on the leaderboard yet. Be the first to register!");
            return 0;
        }
        System.out.println("Leaderboard:");
        for (LeaderBoard.PlayerStats p : leaderboard.players()) {
            System.out.printf("  %-20s  wins: %d  losses: %d  avg guesses: %.1f%n",
                    p.name(), p.wins(), p.losses(), p.averageGuesses());
        }
        return 0;
    }

    private int unknownCommand(String command) {
        System.err.println("Unknown command: " + command);
        return 1;
    }

    private Optional<Integer> readPlayerId() {
        return sessionStore.read().map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    private void printBoardResponse(BoardRes response) {
        System.out.println("May the odds be in your favor " + response.user().name() + "!");
        BoardRenderer.print(response);
    }

    private void printApiError(ApiException exception) {
        System.out.println(exception.error().message());
        if (exception.error().registerCommand() != null && !exception.error().registerCommand().isBlank()) {
            System.out.println(exception.error().registerCommand());
        }
    }
}
