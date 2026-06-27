package wurdal.structures;


public record Links(
        Register register,
        Login login,
        Logout logout,
        Leaderboard leaderboard,
        Guess guess,
        Board board
) {
    public record Login(String href) {}
    public record Logout(String href) {}
    public record Register(String href) {}
    public record Leaderboard(String href) {}
    public record Board(String href) {}
    public record Guess(String href) {}
}
