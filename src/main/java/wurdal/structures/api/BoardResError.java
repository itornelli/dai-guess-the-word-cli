package wurdal.structures.api;

public record BoardResError(Links links, GenError error) implements Board {
}
