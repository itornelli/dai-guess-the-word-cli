package wurdal.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wurdal.model.Player;
import wurdal.repository.PlayerRepository;

import java.util.Map;

@RestController
@RequestMapping("/players")
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @PostMapping
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();

        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name cannot be empty."));
        }

        if (playerRepository.findByName(name.toLowerCase()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "That name is already taken. Please choose another."));
        }

        Player player = new Player(name.toLowerCase());
        player = playerRepository.save(player);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", player.getId(), "name", player.getName()));
    }
}

