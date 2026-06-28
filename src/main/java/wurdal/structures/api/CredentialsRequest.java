package wurdal.structures.api;

public record CredentialsRequest(String username, String password) implements ApiResponse {}
