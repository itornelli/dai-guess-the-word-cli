package wurdal.structures.api;

public record RegisterRes(Integer id, String sessionId, String name) implements Register {
}
