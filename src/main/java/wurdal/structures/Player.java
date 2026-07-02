package wurdal.structures;

import jakarta.persistence.*;

import java.util.UUID;


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
    @Column(insertable = false, updatable = false)
    private UUID token;

    public Player(String name) {
        this.name = name;
    }
    public Player() {
        this.name = null;
    }

    public Integer getId() {
        return this.id;
    }
    public String getName() {return this.name;}
    public int getGamesWon() {return this.gamesWon;}
    public int getGamesLost() {return this.gamesLost;}
    public double getAverageGuesses() {return this.averageGuesses;}
    public Integer getGameId() {return this.gameId;}
    public Boolean getIsInGame() {return this.isInGame;}
    public UUID getToken() { return token; }

    public void setToken(UUID token) { this.token = token; }
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
        //0 average is impossible
        if (this.averageGuesses == 0.0) {
            this.averageGuesses = (double) guesses;
            return;
        }
        this.averageGuesses = (this.averageGuesses + guesses)/2;
    }


}
