package wurdal.structures;

public record GuessResError(Links links, Error error) implements GuessRes {
    public record Error(String description) {}
}
