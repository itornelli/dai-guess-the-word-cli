package wurdal.controllers;

import org.apache.coyote.Response;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.AppConfig;
import wurdal.game.GameEngine;
import wurdal.repository.GameRepository;
import wurdal.repository.PlayerRepository;
import wurdal.structures.BoardRes;
import wurdal.structures.Game;
import wurdal.structures.Player;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.Optional;

@RestController
public record GameController(PlayerRepository playerRepo, GameRepository gameRepo, GameEngine gameEngine) {
    //[CREATE]
    @PostMapping(value="/register")
    public ResponseEntity<Player> register(@RequestBody Player player) {
        playerRepo.save(player);
        return ResponseEntity.ok(player);
    }

    //    public void printNewGameBoard(int wordLength)
    @PostMapping(value="/new-game/{playerId}")
    public ResponseEntity<BoardRes> newGame(@PathVariable Integer playerId) {
        if(playerId == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Player> play = playerRepo.findById(playerId);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Player player = play.get();
        if (player.isInGame()) {
            //TODO(This should have some type of header that will say the player is in a game)
            return ResponseEntity.badRequest().build();
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), playerId);
        Game saved = gameRepo.save(newGame);
        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), saved.getHiddenWord().length(), saved.getHiddenWord(), saved.getCurrentGuesses()));

    }
    @PostMapping(value="/guess/{guessWord}")
    //    public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
    public ResponseEntity<BoardRes> guess(@PathVariable String guessWord, @RequestBody Player player) {
        if (player.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Game updated = gameRepo.getByPlayerId(player.getId());
        updated.addGuessWord(guessWord);
        gameRepo.save(updated);
        //return the board string
        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), updated.getHiddenWord().length(), updated.getHiddenWord(), updated.getCurrentGuesses()));
    }
    //[READ]
    //    public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
    @GetMapping("/board/{playerId}")
    public ResponseEntity<BoardRes> board(@PathVariable Integer playerId) {
        if (playerId == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Player> play = playerRepo.findById(playerId);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Player player = play.get();
        Game session = gameRepo.getByPlayerId(playerId);
        BoardRes boardResObj = new BoardRes(playerId, player.getName(), session.getHiddenWord().length(), session.getHiddenWord(), session.getCurrentGuesses());
        return ResponseEntity.ok(boardResObj);
    }
}
