package wurdal.structures.api;

import wurdal.structures.Player;

import java.util.List;

public record LeaderBoard(List<Player> players) implements ApiResponse {
}
