package wurdal.api;

import wurdal.structures.api.BoardRes;

public record AuthResponse(String message, String sessionId, BoardRes board) {
}
