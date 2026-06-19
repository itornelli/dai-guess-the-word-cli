

import java.util.ArrayList;
// Games are a list of passes or fails 
public record LeaderboardEntry(String name, ArrayList<Integer> games) {

    public Double getAvgGuesses(){
        // Count number of games that are true and divide by total number of games 
        return (double) this.games.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
}
