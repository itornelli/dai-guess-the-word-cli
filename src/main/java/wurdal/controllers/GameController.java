package wurdal.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.repository.GameRepository;
import wurdal.structures.Player;

@RestController
public record GameController(GameRepository gameRepo) {
    //[CREATE]
    @PostMapping(value="/register")
    public ResponseEntity<Player> register(@RequestBody Player player) {
        gameRepo.save(player);
        return ResponseEntity.ok(player);
    }
    //[READ]
    @GetMapping(value="/guess/{guessWord}")
    public ResponseEntity<String> guess(@PathVariable String guessWord) {

    }
}
