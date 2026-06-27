package wurdal.structures;

import java.util.List;

public record GuessResPos(Links links, User user, Current current) implements GuessRes {
    public record User(Integer id, String name) {}

    public record Current(int length, List<Guess> guesses, Result result) {}

    public record Guess(List<LetterResult> letters) {}

    public record LetterResult(Character letter, String match) {}

    public record Result(String status, String word) {}
}
