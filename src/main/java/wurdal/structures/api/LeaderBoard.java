package wurdal.structures.api;

import java.util.List;

public record LeaderBoard(List<PlayerStats> players) {
    public record PlayerStats(String name, int wins, int losses, double averageGuesses) {}
}
