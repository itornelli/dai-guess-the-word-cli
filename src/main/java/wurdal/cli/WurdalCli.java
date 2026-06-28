package wurdal.cli;

import java.util.Locale;
import java.util.Optional;

import wurdal.structures.api.*;
import wurdal.cli.ApiClient.ApiException;

public class WurdalCli {
    private final ApiClient apiClient;
    private final SessionStore sessionStore;
    private final PasswordReader passwordReader;

    public WurdalCli() {
        this(new ApiClient(), new SessionStore(), new PasswordReader());
    }

    WurdalCli(ApiClient apiClient, SessionStore sessionStore, PasswordReader passwordReader) {
        this.apiClient = apiClient;
        this.sessionStore = sessionStore;
        this.passwordReader = passwordReader;
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

    private int handleRegister(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: wurdal register <name>");
            return 1;
        }
        String username = args[1].trim();
        String password = passwordReader.readPassword(username);
        RegisterRes response = apiClient.register(username);
        sessionStore.write(response.sessionId());
        //printBoardResponse(response.board(), response.board().playerName());
        return 0;
    }

    private int handleLogin(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: wurdal login <name>");
            return 1;
        }
        String username = args[1].trim();
        String password = passwordReader.readPassword(username);
        AuthResponse response = apiClient.login(username, password);
        sessionStore.write(response.sessionId());
        //Maybe sessionId should be renamed to Token
        //wasn't the playerId here?
        Board boardResponse = apiClient.board(response.sessionId());
        if (boardResponse instanceof BoardResError) {
            return 1;
        }
        BoardRes res = (BoardRes) boardResponse;
        printBoardResponse(res, res.user().name());
        return 0;
    }

    private int handleLogout() {
        Optional<String> session = sessionStore.read();
        if (session.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        MessageResponse response = apiClient.logout(session.get());
        sessionStore.clear();
        System.out.println(response.message());
        return 0;
    }

    private int handleBoard() {
        Optional<String> session = sessionStore.read();
        if (session.isEmpty()) {
            System.out.println("Please login to continue");
            return 1;
        }
        Board response = apiClient.board(session.get());
        if (response instanceof BoardResError) {
            return 1;
        }
        BoardRes res = (BoardRes)response;
        if (res.user() == null) {
            return 1;
        }
        printBoardResponse(res, res.user().name());
        return 0;
    }

    private int handleGuess(String[] args) {
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
        Board response = apiClient.guess(session.get(), guessWord);
        if (response instanceof BoardResError) {
            return 1;
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

    private void printApiError(ApiException exception) {
        if (exception.statusCode() == 401) {
            sessionStore.clear();
        }
        System.out.println(exception.error().message());
        if (exception.error().registerCommand() != null && !exception.error().registerCommand().isBlank()) {
            System.out.println(exception.error().registerCommand());
        }
    }
}
