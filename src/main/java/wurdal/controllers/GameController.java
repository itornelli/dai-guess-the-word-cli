package wurdal.controllers;

import org.apache.coyote.Response;
import org.springframework.data.domain.Sort;
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
    static final String REGISTER_ENDPOINT = "/player";
    static final String NEW_GAME_ENDPOINT = "/new-game/{playerId}";
    static final String GUESS_ENDPOINT = "/{playerId}/guess";
    static final String BOARD_ENDPOINT = "/{playerId}/board";
    static final String LOGIN_ENDPOINT = "/sessions";
    static final String LOGOUT_ENDPOINT = "";
    static final String LEADERBOARD_ENDPOINT = "/leaderboard";

    static final String[] GUESS_ENDPOINT_LINKS = {GUESS_ENDPOINT, BOARD_ENDPOINT};
    static final String[] BOARD_ENDPOINT_LINKS = {GUESS_ENDPOINT, BOARD_ENDPOINT};
    static final String[] NEWGAME_ENDPOINT_LINKS = {REGISTER_ENDPOINT, NEW_GAME_ENDPOINT, GUESS_ENDPOINT, BOARD_ENDPOINT};
    static final String[] REGISTER_LINKS = {REGISTER_ENDPOINT, NEW_GAME_ENDPOINT, GUESS_ENDPOINT, BOARD_ENDPOINT};

    static final String[] ALL_LINKS = {REGISTER_ENDPOINT, NEW_GAME_ENDPOINT, GUESS_ENDPOINT, BOARD_ENDPOINT};
    public Links buildLinkForPlayer(String[] endpoints, Player player) {
        Links.Register registerLink = null;
        Links.Login loginLink = null;
        Links.Logout logoutLink = null;
        Links.Leaderboard leaderboardLink = null;
        Links.Board boardLink = null;
        Links.Guess guessLink = null;

        for (String ep : endpoints) {
            switch(ep) {
                case REGISTER_ENDPOINT -> {
                    registerLink = new Links.Register(REGISTER_ENDPOINT);
                }
                case NEW_GAME_ENDPOINT -> {

                }
                case GUESS_ENDPOINT -> {
                    String href = GUESS_ENDPOINT.substring(0, GUESS_ENDPOINT.indexOf("{")) + player.getId() + GUESS_ENDPOINT.substring(GUESS_ENDPOINT.indexOf("}")+1, GUESS_ENDPOINT.length());
                    guessLink = new Links.Guess(href);
                }
                case BOARD_ENDPOINT -> {
                    String href = BOARD_ENDPOINT.substring(0, BOARD_ENDPOINT.indexOf("{")) + player.getId() + BOARD_ENDPOINT.substring(BOARD_ENDPOINT.indexOf("}")+1, BOARD_ENDPOINT.length());
                    boardLink = new Links.Board(href);
                }
            }
        }

        return new Links(registerLink, loginLink, logoutLink, leaderboardLink, guessLink, boardLink);
    }
    public Links buildAllLinks() {
        Links.Register registerLink = null;
        Links.Login loginLink = null;
        Links.Logout logoutLink = null;
        Links.Leaderboard leaderboardLink = null;
        Links.Board boardLink = null;
        Links.Guess guessLink = null;

        for (String ep : ALL_LINKS) {
            switch(ep) {
                case REGISTER_ENDPOINT -> {
                    registerLink = new Links.Register(REGISTER_ENDPOINT);
                }
                case NEW_GAME_ENDPOINT -> {

                }
                case GUESS_ENDPOINT -> {
                    String href = GUESS_ENDPOINT;
                    guessLink = new Links.Guess(href);
                }
                case BOARD_ENDPOINT -> {
                    String href = BOARD_ENDPOINT;
                    boardLink = new Links.Board(href);
                }
            }
        }

        return new Links(registerLink, loginLink, logoutLink, leaderboardLink, guessLink, boardLink);
    }

    //[CREATE]
    @PostMapping(value="/player")
    public ResponseEntity<BoardRes> register(@RequestBody Player player) {
        Player saved = playerRepo.save(player);
        if (saved.isInGame()) {
            //TODO(This should have some type of header that will say the player is in a game)
            return ResponseEntity.badRequest().build();
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), saved.getId());
        Game savedGame = gameRepo.save(newGame);
        return ResponseEntity.ok(new BoardRes(buildLinkForPlayer(REGISTER_LINKS, player), player.getId(), player.getName(), savedGame.getHiddenWord().length(), savedGame.getHiddenWord(), savedGame.getCurrentGuesses()));
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
        return ResponseEntity.ok(new BoardRes(buildLinkForPlayer(NEWGAME_ENDPOINT_LINKS, player), player.getId(), player.getName(), saved.getHiddenWord().length(), saved.getHiddenWord(), saved.getCurrentGuesses()));

    }

//    //OLD
//    @PostMapping(value="/guess/{guessWord}")
//    public ResponseEntity<BoardRes> guess(@PathVariable String guessWord, @RequestBody Player player) {
//        if (player.getId() == null) {
//            return ResponseEntity.badRequest().build();
//        }
//        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(player.getId());
//        if (gameOpt.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        Game updated = gameOpt.get();
//        updated.addGuessWord(guessWord);
//        gameRepo.save(updated);
//        //return the board string
//        return ResponseEntity.ok(new BoardRes(player.getId(), player.getName(), updated.getHiddenWord().length(), updated.getHiddenWord(), updated.getCurrentGuesses()));
//    }
    //public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
    @PostMapping(value="/{playerId}/guess")
    public ResponseEntity<GuessRes> guessById(@PathVariable Integer playerId, @RequestBody GuessReq guessReq) {
        String guessWord = guessReq.guess();

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

        if(guessWord.length() != updated.getHiddenWord().length()) {
            int wordLength = updated.getHiddenWord().length();
            return ResponseEntity.badRequest().body(new GuessResError(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player), new GuessResError.Error("Guess must be exactly " + wordLength + " letters")));
        }

        if (updated.getCurrentGuesses() == null) {
            updated.emergencySetCurrentGuesses(new ArrayList<>());
        }
        if (updated.getStatus() == 1 && updated.getCurrentGuesses().size() < 6) {
            updated.addGuessWord(guessWord);
            if (guessWord.equals(updated.getHiddenWord())) {
                updated.setStatus(0);
                player.setGamesWon(player.gamesWon() + 1);
            }
            else if (updated.getCurrentGuesses().size() >= 6) {
                updated.setStatus(2);
            }
        }


        List<GuessResPos.Guess> guessList = new ArrayList<>();
        for (String guess : updated.getCurrentGuesses()) {
            List<GuessResPos.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(updated.getHiddenWord(), guess, updated.getHiddenWord().length());
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
                GuessResPos.LetterResult result = new GuessResPos.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            GuessResPos.Guess guessObj = new GuessResPos.Guess(letterResults);
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
        GuessRes guessResponse = new GuessResPos(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new GuessResPos.User(player.getId(),player.getName()),
                new GuessResPos.Current(updated.getHiddenWord().length(),guessList,
                        new GuessResPos.Result(resultStatus, (status == 0 || status == 2)? updated.getHiddenWord() : null)));
        return ResponseEntity.ok(guessResponse);
    }
    //[READ]
    //public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
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
        BoardRes boardResObj = new BoardRes(buildLinkForPlayer(BOARD_ENDPOINT_LINKS, player), playerId, player.getName(), session.getHiddenWord().length(), session.getHiddenWord(), session.getCurrentGuesses());
        return ResponseEntity.ok(boardResObj);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderBoard> getLeaderboard() {
        List<Player> players = playerRepo.findAll(Sort.by(Sort.Direction.DESC, "gamesWon"));
        return ResponseEntity.ok(new LeaderBoard(players));
    }

    @GetMapping("/")
    public ResponseEntity<Links> getLinks() {
        Links all = buildAllLinks();
        return ResponseEntity.ok(all);
    }
}
