package wurdal.structures;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Integer gamesWon = 0;
    private Integer gamesLost = 0;
    private Double averageGuesses = 0.0;
    private Boolean isInGame = false;

    public Player(String name) {
        this.name = name;
    }

    public Player() {
        this.name = null;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public int getGamesWon() { return gamesWon; }
    public int getGamesLost() { return gamesLost; }
    public double getAverageGuesses() { return averageGuesses; }
    public Boolean getIsInGame() { return isInGame; }

    public void setName(String name) { this.name = name; }
    public void setGamesWon(int gamesWon) { this.gamesWon = gamesWon; }
    public void setGamesLost(int gamesLost) { this.gamesLost = gamesLost; }
    public void setAverageGuesses(double averageGuesses) { this.averageGuesses = averageGuesses; }

    public void winGame(int guesses) {
        gamesWon++;
        isInGame = false;
        averageGuesses = (averageGuesses + guesses) / 2.0;
    }

    public void loseGame() {
        gamesLost++;
        isInGame = false;
    }
}
