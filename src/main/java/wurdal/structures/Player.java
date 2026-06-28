package wurdal.structures;

import jakarta.persistence.*;


@Entity
@Table(name = "Players")
public class Player {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Integer gamesWon = 0;
    private Integer gamesLost = 0;
    private Double averageGuesses = 0.0;
    private Integer gameId = null;
    private Boolean isInGame = false;

    public Player(String name) {
        this.name = name;
    }

    public Integer getId() {
        return this.id;
    }
    public String getName() {return this.name;}
    public int getGamesWon() {return this.gamesWon;}
    public int gamesWon() {return this.gamesWon;}
    public int getGamesLost() {return this.gamesLost;}
    public double getAverageGuesses() {return this.averageGuesses;}
    public Integer getGameId() {return this.gameId;}
    public int gameId() {return this.gameId;}
    public boolean isInGame() {return this.isInGame;}

    public void setName(String name) {this.name = name;}
    public void setGamesWon(int gamesWon) {this.gamesWon = gamesWon;}
    public void setGamesLost(int gamesLost) {this.gamesLost = gamesLost;}
    public void setAverageGuesses(double averageGuesses) {this.averageGuesses = averageGuesses;}
    public void setGameId(int gameId) {this.gameId = gameId;}

    public void winGame() {
        this.gamesWon++;
        this.gameId = null;
        this.isInGame = false;
    }

    public void loseGame() {

    }

    public void findAverage(int guesses) {
        this.averageGuesses = (this.averageGuesses + guesses)/2;
    }


}
