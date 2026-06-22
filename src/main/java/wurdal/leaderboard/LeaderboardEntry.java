package wurdal.leaderboard;

import java.util.ArrayList;

/**
 * Represents a player's leaderboard entry with their game history.
 */
public record LeaderboardEntry(String name, ArrayList<Integer> games) {

    /**
     * Calculate average guesses for games this player has solved (positive scores).
     * Games with -1 (losses) are excluded from the average.
     * 
     * @return average guesses per solved game, or 0.0 if no games solved
     */
    public Double getAvgGuesses() {
        if (this.games.stream().count() == 0) {
            return 0.0;
        }
        var validGames = this.games.stream().filter((game) -> game > 0).reduce(0, Integer::sum);
        var totalGames = this.games.stream().count();
        return (double) validGames / totalGames;
    }
}
