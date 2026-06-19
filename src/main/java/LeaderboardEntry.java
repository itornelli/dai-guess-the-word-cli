

import java.util.ArrayList;
// Games are a list of passes or fails 
public record LeaderboardEntry(String name, ArrayList<Integer> games) {

    public Double getAvgGuesses(){
        // Division by zero logic
        if(this.games.stream().count() == 0){
            return 0.0;
        }
        var validGames = this.games.stream().filter((game) -> game > 0).reduce(0, Integer::sum);
        var totalGames = this.games.stream().count(); 
        // Count number of games that are true and divide by total number of games
        return (double) validGames / totalGames;
    }
}
