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
    private int gamesWon = 0;
    private Integer gameId;
    private boolean isInGame;

    public Integer getId() {
        return this.id;
    }
    public String getName() {return this.name;}
    public int gamesWon() {return this.gamesWon;}
    public int gameId() {return this.gameId;}
    public boolean isInGame() {return this.isInGame;}

    public void setName(String name) {this.name = name;}
    public void setGamesWon(int gamesWon) {this.gamesWon = gamesWon;}
    public void setGameId(int gameId) {this.gameId = gameId;}

    public void winGame() {
        this.gamesWon++;
        this.gameId = null;
        this.isInGame = false;
    }


}
