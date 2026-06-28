package wurdal.structures.api;

public record AuthResponse(String message, String sessionId, BoardRes board) implements ApiResponse {
}
