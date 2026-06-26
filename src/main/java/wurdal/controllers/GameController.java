package wurdal.controllers;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.repository.GameRepository;
import wurdal.repository.PlayerRepository;
import wurdal.structures.Game;
import wurdal.structures.Player;

@RestController
public record GameController(PlayerRepository playerRepo, GameRepository gameRepo) {
    //[CREATE]
    @PostMapping(value="/register")
    public ResponseEntity<Player> register(@RequestBody Player player) {
        playerRepo.save(player);
        return ResponseEntity.ok(player);
    }
    @PostMapping(value="/guess/{guessWord}")
    public ResponseEntity<String> guess(@PathVariable String guessWord, @ResponseBody Player player) {
        Game updated = gameRepo.
        updated.addGuessWord(guessWord);
        gameRepo.save(updated);
        return ResponseEntity.ok(updated);
    }
    //[READ]
    @GetMapping("/board/{id}")
    public ResponseEntity<String> board(@PathVariable Integer playerId) {
        gameRepo.findById(playerId);
    }
}
