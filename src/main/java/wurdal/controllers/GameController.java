package wurdal.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.game.GameEngine;
import wurdal.repository.GameRepository;
import wurdal.repository.PlayerRepository;
import wurdal.structures.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public record GameController(PlayerRepository playerRepo, GameRepository gameRepo, GameEngine gameEngine) {
    //[CREATE]
    @PostMapping(value="/register")
    public ResponseEntity<BoardRes> register(@RequestBody Player player) {
        Player saved = playerRepo.save(player);
        if (saved.isInGame()) {
            //TODO(This should have some type of header that will say the player is in a game)
            return ResponseEntity.badRequest().build();
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), saved.getId());
        Game savedGame = gameRepo.save(newGame);
        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), savedGame.getHiddenWord().length(), savedGame.getHiddenWord(), savedGame.getCurrentGuesses()));
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
            //start new game (maybe change this idk)
            //uses 007a-create-player-in-game-trigger
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), playerId);
        Game saved = gameRepo.save(newGame);
        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), saved.getHiddenWord().length(), saved.getHiddenWord(), saved.getCurrentGuesses()));

    }

    //OLD
    @PostMapping(value="/guess/{guessWord}")
    //    public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
    public ResponseEntity<BoardRes> guess(@PathVariable String guessWord, @RequestBody Player player) {
        if (player.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(player.getId());
        if (gameOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Game updated = gameOpt.get();
        updated.addGuessWord(guessWord);
        gameRepo.save(updated);
        //return the board string
        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), updated.getHiddenWord().length(), updated.getHiddenWord(), updated.getCurrentGuesses()));
    }
    @PostMapping(value="/{playerId}/guess")
    public ResponseEntity<GuessRes> guessById(@PathVariable Integer playerId, @RequestBody GuessReq guessReq) {
        String guessWord = guessReq.word();

        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(playerId);
        if (gameOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Player> play = playerRepo.findById(playerId);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Player player = play.get();
        Game updated = gameOpt.get();
        if (updated.getStatus() == 1 && updated.getCurrentGuesses().size() < 6) {
            updated.addGuessWord(guessWord);
            if (guessWord.equals(updated.getHiddenWord())) {
                updated.setStatus(0);
            }
            else if (updated.getCurrentGuesses().size() >= 6) {
                updated.setStatus(2);
            }
        }


        List<GuessRes.Guess> guessList = new ArrayList<>();
        for (String guess : updated.getCurrentGuesses()) {
            List<GuessRes.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(updated.getHiddenWord(), guessWord);
            for(int i = 0; i < guess.length(); i++) {
                String colorResult = "";
                if(colors[i].equals(GameEngine.ANSI_GREEN)) {
                    colorResult = "full";
                }
                else if (colors[i].equals(GameEngine.ANSI_YELLOW)) {
                    colorResult = "partial";
                }
                else {
                    colorResult = "none";
                }
                GuessRes.LetterResult result = new GuessRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            GuessRes.Guess guessObj = new GuessRes.Guess(letterResults);
            guessList.add(guessObj);
        }
        String resultStatus = "";
        Integer status = updated.getStatus();
        if (status == 0) {
            resultStatus = "won";
        }
        else if (status == 1) {
            resultStatus = "in-progress";
        }
        else if (status == 2){
            resultStatus = "lost";
        }

        gameRepo.save(updated);
        GuessRes guessResponse = new GuessRes(
                new GuessRes.User(player.getId(),player.getName()),
                new GuessRes.Current(updated.getHiddenWord().length(),guessList,
                        new GuessRes.Result(resultStatus, (status == 0 || status == 2)? updated.getHiddenWord() : null)));
        return ResponseEntity.ok(guessResponse);
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
        Optional<Game> sessionOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(playerId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Game session = sessionOpt.get();
        BoardRes boardResObj = new BoardRes(playerId, player.getName(), session.getHiddenWord().length(), session.getHiddenWord(), session.getCurrentGuesses());
        return ResponseEntity.ok(boardResObj);
    }
}
