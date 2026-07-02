package wurdal.structures.api;

import java.util.List;

public record LeaderBoard(List<PlayerStats> players) implements ApiResponse {
    public record PlayerStats(String name, int wins, int losses, double averageGuesses) {}
}
