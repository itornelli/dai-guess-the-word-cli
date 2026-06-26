package wurdal.structures;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="Games")
public class Game {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hiddenWord;
    @ElementCollection
    private List<String> currentGuesses;
    private Integer playerId;

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

    public void setHiddenWord(String word) {
        this.hiddenWord = word;
    }
}
