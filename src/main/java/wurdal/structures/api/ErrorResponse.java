package wurdal.structures.api;

public record ErrorResponse(String message, String registerCommand) implements ApiResponse {
}
