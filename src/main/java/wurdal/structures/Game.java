package wurdal.structures;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Games")
public class Game {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hiddenWord;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> currentGuesses = new ArrayList<>();
    private Integer playerId;
    private Integer status = 1;

    protected Game() {
        //required by JPA
    }

    public Game(String hiddenWord, List<String> guesses, Integer playerId) {
        this.hiddenWord = hiddenWord;
        this.playerId = playerId;
        this.currentGuesses = guesses;
    }

    public void addGuessWord(String guessWord) {
        this.currentGuesses.add(guessWord);
    }

    public List<String> getCurrentGuesses() {
        return this.currentGuesses;
    }
    public String getHiddenWord() {
        return this.hiddenWord;
    }
    public Integer getPlayerId() {
        return this.playerId;
    }
    public Integer getStatus() {return this.status;}

    public void setHiddenWord(String word) {
        this.hiddenWord = word;
    }
    public void setStatus(Integer status) {this.status = status;}
    public void emergencySetCurrentGuesses(List<String> current) {this.currentGuesses = current;}
}
