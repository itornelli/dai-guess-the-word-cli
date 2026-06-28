package wurdal.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.game.GameEngine;
import wurdal.repository.GameRepository;
import wurdal.repository.PlayerRepository;
import wurdal.structures.*;
import wurdal.structures.api.*;

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
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterReq req) {
        Player player = new Player(req.name());
        Player saved = playerRepo.save(player);

        if (saved.isInGame()) {
            //TODO(This should have some type of header that will say the player is in a game)
            return ResponseEntity.badRequest().body(new GenError("name must be ..."));
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), saved.getId());
        Game savedGame = gameRepo.save(newGame);
        //null placeholder for sessionId until login is complete
        return ResponseEntity.ok(new RegisterRes(player.getId(), null, player.getName()));
//        return ResponseEntity.ok(new BoardRes(buildLinkForPlayer(REGISTER_LINKS, player), player.getId(), player.getName(), savedGame.getHiddenWord().length(), savedGame.getHiddenWord(), savedGame.getCurrentGuesses()));
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
    public ResponseEntity<Board> guessById(@PathVariable Integer playerId, @RequestBody GuessReq guessReq) {
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
            return ResponseEntity.badRequest().body(new BoardResError(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player), new GenError("Guess must be exactly " + wordLength + " letters")));
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


        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : updated.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
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
        Board guessResponse = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(updated.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (status == 0 || status == 2)? updated.getHiddenWord() : null)));
        return ResponseEntity.ok(guessResponse);
    }

    @PostMapping(value="/guess")
    public ResponseEntity<Board> guess(@RequestHeader Integer Bearer, @RequestBody GuessReq guessReq) {
        String guessWord = guessReq.guess();

        Optional<Player> play = playerRepo.findFirstByToken(Bearer);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = play.get();

        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(player.getId());
        if (gameOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Game updated = gameOpt.get();

        if(guessWord.length() != updated.getHiddenWord().length()) {
            int wordLength = updated.getHiddenWord().length();
            return ResponseEntity.badRequest().body(new BoardResError(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player), new GenError("Guess must be exactly " + wordLength + " letters")));
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


        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : updated.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
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
        Board guessResponse = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(updated.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (status == 0 || status == 2)? updated.getHiddenWord() : null)));
        return ResponseEntity.ok(guessResponse);
    }
    //[READ]
    //public void printBoardWithGuesses(int wordLength, String hiddenWord, List<String> guesses)
    @GetMapping("/board/{playerId}")
    public ResponseEntity<Board> boardById(@PathVariable Integer playerId) {
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

        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : session.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(session.getHiddenWord(), guess, session.getHiddenWord().length());
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
            guessList.add(guessObj);
        }

        String resultStatus = "";
        Integer status = session.getStatus();
        if (status == 0) {
            resultStatus = "won";
        }
        else if (status == 1) {
            resultStatus = "in-progress";
        }
        else if (status == 2){
            resultStatus = "lost";
        }

        Board boardResObj = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(session.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (session.getStatus() == 0 || session.getStatus() == 2)? session.getHiddenWord() : null)));
        return ResponseEntity.ok(boardResObj);
    }

    @GetMapping("/board")
    public ResponseEntity<Board> board(@RequestHeader Integer Bearer) {
        if (Bearer == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Player> play = playerRepo.findFirstByToken(Bearer);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Player player = play.get();
        Optional<Game> sessionOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(player.getId());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Game session = sessionOpt.get();

        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : session.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(session.getHiddenWord(), guess, session.getHiddenWord().length());
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
            guessList.add(guessObj);
        }

        String resultStatus = "";
        Integer status = session.getStatus();
        if (status == 0) {
            resultStatus = "won";
        }
        else if (status == 1) {
            resultStatus = "in-progress";
        }
        else if (status == 2){
            resultStatus = "lost";
        }

        Board boardResObj = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(session.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (session.getStatus() == 0 || session.getStatus() == 2)? session.getHiddenWord() : null)));
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

    //    public void printNewGameBoard(int wordLength)
    @GetMapping(value="/new-game/{playerId}")
    public ResponseEntity<Board> newGameById(@PathVariable Integer playerId) {
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

        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : saved.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(saved.getHiddenWord(), guess, saved.getHiddenWord().length());
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
            guessList.add(guessObj);
        }

        Integer status = saved.getStatus();
        String resultStatus = "";
        if (status == 0) {
            resultStatus = "won";
        }
        else if (status == 1) {
            resultStatus = "in-progress";
        }
        else if (status == 2){
            resultStatus = "lost";
        }
        else {
            resultStatus = "unknown";
        }

        Board boardResObj = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(saved.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (saved.getStatus() == 0 || saved.getStatus() == 2)? saved.getHiddenWord() : null)));
        return ResponseEntity.ok(boardResObj);
    }

    @GetMapping(value="/new-game")
    public ResponseEntity<Board> newGame(@RequestHeader Integer Bearer) {
        if(Bearer == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Player> play = playerRepo.findFirstByToken(Bearer);
        if (play.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Player player = play.get();
        if (player.isInGame()) {
            //start new game (maybe change this idk)
            //uses 007a-create-player-in-game-trigger
        }
        Game newGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), player.getId());
        Game saved = gameRepo.save(newGame);

        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : saved.getCurrentGuesses()) {
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
            String[] colors = gameEngine.evaluateGuessColors(saved.getHiddenWord(), guess, saved.getHiddenWord().length());
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
                BoardRes.LetterResult result = new BoardRes.LetterResult(guess.charAt(i), colorResult);
                letterResults.add(result);
            }
            BoardRes.Guess guessObj = new BoardRes.Guess(letterResults);
            guessList.add(guessObj);
        }

        Integer status = saved.getStatus();
        String resultStatus = "";
        if (status == 0) {
            resultStatus = "won";
        }
        else if (status == 1) {
            resultStatus = "in-progress";
        }
        else if (status == 2){
            resultStatus = "lost";
        }
        else {
            resultStatus = "unknown";
        }

        Board boardResObj = new BoardRes(buildLinkForPlayer(GUESS_ENDPOINT_LINKS, player),
                new BoardRes.User(player.getId(),player.getName()),
                new BoardRes.Current(saved.getHiddenWord().length(),guessList,
                        new BoardRes.Result(resultStatus, (saved.getStatus() == 0 || saved.getStatus() == 2)? saved.getHiddenWord() : null)));
        return ResponseEntity.ok(boardResObj);
    }

    @GetMapping(value="/getId/{playerName}")
    public ResponseEntity<JsonNode> getIdByName(@PathVariable String playerName) {
        Optional<Integer> playerId = playerRepo.findFirstByName(playerName);
        ObjectMapper objm = new ObjectMapper();
        ObjectNode out = objm.createObjectNode();
        out.put("id", "-2");
        playerId.ifPresent(id -> out.put("id", id));
        return ResponseEntity.ok(out);
    }

    @GetMapping(value="/getId")
    public ResponseEntity<JsonNode> getIdByName(@RequestHeader Integer Bearer) {
        Optional<Player> player = playerRepo.findFirstByToken(Bearer);
        ObjectMapper objm = new ObjectMapper();
        ObjectNode out = objm.createObjectNode();
        out.put("id", "-2");
        player.ifPresent(id -> out.put("id", player.get().getId()));
        return ResponseEntity.ok(out);
    }
}
