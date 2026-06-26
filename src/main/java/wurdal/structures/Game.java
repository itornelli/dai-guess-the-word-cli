package wurdal.structures;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="Games")
public class Game {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hiddenWord;
    private List<String> currentGuesses;
    private Integer playerId;

    public void addGuessWord(String guessWord) {
        this.currentGuesses.add(guessWord);
    }

}
