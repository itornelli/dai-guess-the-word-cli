import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;


public class wurdal {

    public String currentInput = "";
    public String hiddenWord = "";
    public Set<String> currentSeenWords = new HashSet<String>() {};
    public Set<String> currentGuesses = new HashSet<String>(){};
    public Set<String> currentGames = new HashSet<String>(){};
    public List<LeaderboardEntry> leaderboard = new ArrayList<LeaderboardEntry>(){};
    public CommandLineParser parser = new CommandLineParser(){};

    public wurdal() {
        loadPlayersFromFile();
        loadGamesFromFile();
    }

    private void loadPlayersFromFile() {
        try {
            if (Files.exists(Paths.get("players.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("players.txt"));
                for (String line : lines) {
                    String playerName = line.trim();
                    if (!playerName.isEmpty()) {
                        leaderboard.add(new LeaderboardEntry(playerName, new ArrayList<Integer>()));
                    }
                }
                System.out.println("Loaded " + leaderboard.size() + " registered players.");
            }
        } catch (IOException e) {
            System.err.println("Error loading players.txt: " + e.getMessage());
        }
    }

    private void loadGamesFromFile() {
        try {
            if (Files.exists(Paths.get("games.txt"))) {
                List<String> lines = Files.readAllLines(Paths.get("games.txt"));
                if (!lines.isEmpty()) {
                    String gameState = lines.get(0);
                    String[] parts = gameState.split("\\|");
                    if (parts.length >= 2) {
                        hiddenWord = parts[0].trim();
                        String[] guesses = parts[1].split(",");
                        for (String guess : guesses) {
                            String trimmedGuess = guess.trim();
                            if (!trimmedGuess.isEmpty()) {
                                currentGuesses.add(trimmedGuess);
                            }
                        }
                        System.out.println("Loaded game state: hidden word set, " + currentGuesses.size() + " guesses.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading games.txt: " + e.getMessage());
        }
    }

    private void savePlayersToFile() {
        try {
            List<String> playerNames = new ArrayList<>();
            for (LeaderboardEntry entry : leaderboard) {
                playerNames.add(entry.name());
            }
            Files.write(Paths.get("players.txt"), playerNames);
        } catch (IOException e) {
            System.err.println("Error saving players.txt: " + e.getMessage());
        }
    }

    private void saveGamesToFile() {
        try {
            String gameState = hiddenWord + "|" + String.join(",", currentGuesses);
            Files.write(Paths.get("games.txt"), List.of(gameState));
        } catch (IOException e) {
            System.err.println("Error saving games.txt: " + e.getMessage());
        }
    }

    public void printLeaderboard(Boolean byGames){
        if (leaderboard.isEmpty()) {
            System.out.println("Leaderboard is empty");
            return;
        }

        List<LeaderboardEntry> sortedLeaderboard = new ArrayList<>(leaderboard);
        
        if (byGames) {
            sortedLeaderboard.sort((a, b) -> Integer.compare(b.games().size(), a.games().size()));
        } else {
            sortedLeaderboard.sort((a, b) -> Double.compare(a.getAvgGuesses(), b.getAvgGuesses()));
        }

        int rank = 1;
        for (LeaderboardEntry entry : sortedLeaderboard) {
            int numGames = entry.games().size();
            double avgGuesses = entry.getAvgGuesses();
            String gameWord = numGames == 1 ? "game" : "games";
            System.out.println("%d. %s - %d %s - avg %.1f guesses".formatted(
                rank, entry.name(), numGames, gameWord, avgGuesses));
            rank++;
        }
    }
    public class CommandLineParser {
    
        public enum actions { REGISTER, NEW_GAME, GUESS, LEADERBOARD };

        public void Parse(String playerInput){
            // Check if inpiut is empty
            if (playerInput.isEmpty()){
                throw new IllegalArgumentException("Invalid command: " + playerInput);
            }

            // Normalize the player input
            var normInput = playerInput.strip().split(" ");
            // Extract the command
            var command = normInput[0].toUpperCase();
            
            // Short Circuit if command not in actions
            boolean commandExists = Arrays.stream(actions.values()).anyMatch(action -> action.name().equals(command));
            if (!commandExists) {
                throw new IllegalArgumentException("Unknown command: " + command);
            }

            switch (actions.valueOf(command)) {
                case REGISTER:
                    System.out.println("Called Register");
                    // Short Circuit if cannot form a valid command
                    if(normInput.length < 1){
                        throw new IllegalArgumentException("Invalid Arguments:" + normInput.toString());
                    }
                    String playerName = normInput[1].strip();
                    leaderboard.add(new LeaderboardEntry(playerName, new ArrayList<Integer>(){}));                    savePlayersToFile();                    break;
                case NEW_GAME:
                    System.out.println("Called New_Game");
                    if(normInput.length < 1){
                        throw new IllegalArgumentException("Invalid Arguments:" + normInput.toString());
                    }
                    String playerName2 = normInput[1].strip();
                    // Check if player exists in the leaderboard
                    boolean playerExists = leaderboard.stream().anyMatch(entry -> entry.name().equals(playerName2));
                    if (!playerExists) {
                        throw new IllegalArgumentException("Player not registered: " + playerName2);
                    }
                    currentGuesses.clear();
                    saveGamesToFile();
                    break;
                case GUESS:
                    System.out.println("Called Guess");
                    if(normInput.length < 1){
                        throw new IllegalArgumentException("Invalid Arguments:" + normInput.toString());
                    }
                    currentInput = normInput[1];
                    currentGuesses.add(currentInput);
                    saveGamesToFile();
                    break;
                case LEADERBOARD:
                    System.out.println("Called Leaderboard");
                    var varGuesses = normInput.length > 1 && normInput[1].equals("--by-guesses");
                    printLeaderboard(varGuesses);
                    break;
            
                default:
                    throw new IllegalArgumentException("Unknown command: " + command);
            }



        }
    }
    public static void main(String[] args) {
        System.out.println("Welcome to wurdal!");

        wurdal game = new wurdal();

        // Single-shot mode: wurdal <command> <args..>
        if (args.length > 0) {
            String commandLine = String.join(" ", args);
            game.parser.Parse(commandLine);
            return;
        }

        // Interactive mode: read commands from stdin
        String currInput = "";
        try (Scanner scanner = new Scanner(System.in)) {
            while (!currInput.equals("exit") && scanner.hasNextLine()) {
                System.out.print("> ");
                System.out.flush();
                currInput = scanner.nextLine();
                if (!currInput.equals("exit")) {
                    game.parser.Parse(currInput);
                }
            }
        }
    }
}
