package wurdal.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.game.GameEngine;
import wurdal.repository.GameRepository;
import wurdal.repository.PlayerRepository;
import wurdal.structures.Game;
import wurdal.structures.Player;
import wurdal.structures.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public record GameController(PlayerRepository playerRepo, GameRepository gameRepo, GameEngine gameEngine) {

    static final Logger logger = LoggerFactory.getLogger(GameController.class);

    // ── Root ─────────────────────────────────────────────────────────────────

    @GetMapping("/")
    public ResponseEntity<?> getLinks() {
        return ResponseEntity.ok(Map.of("_links", Map.of(
                "register",    Map.of("href", "/players"),
                "login",       Map.of("href", "/sessions"),
                "leaderboard", Map.of("href", "/leaderboard")
        )));
    }

    // ── Players ───────────────────────────────────────────────────────────────

    @PostMapping("/players")
    public ResponseEntity<?> register(@RequestBody RegisterReq req) {
        String name = req.name() == null ? null : req.name().trim();
        if (name == null || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(error("Name is required"));
        }
        try {
            Player player = new Player(name);
            Player saved = playerRepo.saveAndFlush(player);
            gameRepo.save(new Game(gameEngine.chooseRandomWord(saved.getName()), new ArrayList<>(), saved.getId()));

            RegisterRes res = new RegisterRes(
                    saved.getId(),
                    saved.getName(),
                    new RegisterRes.RegisterLinks(
                            new Links.Board("/players/" + saved.getId() + "/board"),
                            new Links.Guess("/players/" + saved.getId() + "/guess")
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("That name is already taken. Please choose another."));
        }
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<?> login(@RequestBody CredentialsRequest req) {
        String name = req.name() == null ? null : req.name().trim();
        if (name == null || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(error("Name is required"));
        }
        Optional<Player> playerOpt = playerRepo.findFirstByName(name);
        if (playerOpt.isEmpty()) {
            // case-insensitive fallback
            playerOpt = playerRepo.findAll().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst();
        }
        if (playerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("Could not find user " + name + ". Please register"));
        }
        Player player = playerOpt.get();
        return ResponseEntity.ok(new AuthResponse(player.getId(), player.getName()));
    }

    // ── Board ─────────────────────────────────────────────────────────────────

    @GetMapping("/players/{playerId}/board")
    public ResponseEntity<?> board(
            @PathVariable Integer playerId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ResponseEntity<?> authCheck = checkAuth(playerId, authorization);
        if (authCheck != null) return authCheck;

        Optional<Player> playerOpt = playerRepo.findById(playerId);
        if (playerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(playerId);
        if (gameOpt.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(buildBoardRes(playerOpt.get(), gameOpt.get()));
    }

    // ── Guess ─────────────────────────────────────────────────────────────────

    @PostMapping("/players/{playerId}/guess")
    public ResponseEntity<?> guess(
            @PathVariable Integer playerId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GuessReq guessReq) {

        ResponseEntity<?> authCheck = checkAuth(playerId, authorization);
        if (authCheck != null) return authCheck;

        Optional<Player> playerOpt = playerRepo.findById(playerId);
        if (playerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<Game> gameOpt = gameRepo.findFirstByPlayerIdOrderByIdDesc(playerId);
        if (gameOpt.isEmpty()) return ResponseEntity.notFound().build();

        Player player = playerOpt.get();
        Game game = gameOpt.get();
        String guessWord = guessReq.guess() == null ? "" : guessReq.guess().trim().toLowerCase();

        if (guessWord.length() != game.getHiddenWord().length()) {
            int expected = game.getHiddenWord().length();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", Map.of("description", "Guess must be exactly " + expected + " letters")));
        }

        if (game.getStatus() == 1 && game.getCurrentGuesses().size() < GameEngine.BOARD_ROWS) {
            game.addGuessWord(guessWord);
            if (guessWord.equals(game.getHiddenWord())) {
                game.setStatus(0);
                player.winGame(game.getCurrentGuesses().size());
                playerRepo.save(player);
            } else if (game.getCurrentGuesses().size() >= GameEngine.BOARD_ROWS) {
                game.setStatus(2);
                player.loseGame();
                playerRepo.save(player);
            }
        }

        if (game.getStatus() == 0 || game.getStatus() == 2) {
            gameRepo.save(game);
            Game nextGame = new Game(gameEngine.chooseRandomWord(player.getName()), new ArrayList<>(), player.getId());
            gameRepo.save(nextGame);
        } else {
            gameRepo.save(game);
        }

        return ResponseEntity.ok(buildBoardRes(player, game));
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderBoard> getLeaderboard() {
        List<Player> players = playerRepo.findAll(Sort.by(Sort.Direction.DESC, "gamesWon"));
        List<LeaderBoard.PlayerStats> stats = players.stream()
                .map(p -> new LeaderBoard.PlayerStats(p.getName(), p.getGamesWon(), p.getGamesLost(), p.getAverageGuesses()))
                .toList();
        return ResponseEntity.ok(new LeaderBoard(stats));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<?> checkAuth(Integer playerId, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", Map.of("description", "Access denied")));
        }
        String token = authorization.replaceFirst("(?i)^Bearer\\s+", "");
        try {
            int tokenId = Integer.parseInt(token);
            if (tokenId != playerId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", Map.of("description", "Access denied")));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", Map.of("description", "Access denied")));
        }
        return null;
    }

    private BoardRes buildBoardRes(Player player, Game game) {
        List<BoardRes.Guess> guessList = new ArrayList<>();
        for (String guess : game.getCurrentGuesses()) {
            String[] colors = gameEngine.evaluateGuessColors(game.getHiddenWord(), guess, game.getHiddenWord().length());
            List<BoardRes.LetterResult> letterResults = new ArrayList<>();
            for (int i = 0; i < guess.length(); i++) {
                String match = switch (colors[i]) {
                    case GameEngine.ANSI_GREEN  -> "full";
                    case GameEngine.ANSI_YELLOW -> "partial";
                    default                     -> "none";
                };
                letterResults.add(new BoardRes.LetterResult(guess.charAt(i), match));
            }
            guessList.add(new BoardRes.Guess(letterResults));
        }

        String status = switch (game.getStatus()) {
            case 0  -> "won";
            case 2  -> "lost";
            default -> "in-progress";
        };

        Links links = new Links(
                null, null, null,
                new Links.Board("/players/" + player.getId() + "/board"),
                new Links.Guess("/players/" + player.getId() + "/guess")
        );

        return new BoardRes(
                links,
                new BoardRes.User(player.getId(), player.getName()),
                new BoardRes.Current(game.getHiddenWord().length(), guessList,
                        new BoardRes.Result(status, game.getHiddenWord()))
        );
    }

    private Map<String, Object> error(String description) {
        return Map.of("error", Map.of("description", description));
    }
}
