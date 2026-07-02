package wurdal.structures.api;

public record Links(
        Register register,
        Login login,
        Leaderboard leaderboard,
        Board board,
        Guess guess
) {
    public record Login(String href) {}
    public record Register(String href) {}
    public record Leaderboard(String href) {}
    public record Board(String href) {}
    public record Guess(String href) {}
}
