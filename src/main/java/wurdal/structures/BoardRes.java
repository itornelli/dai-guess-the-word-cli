package wurdal.structures;

import java.util.List;

public record BoardRes(
        Links links,
        int playerId,
        String playerName,
        int wordLength,
        String hiddenWord,
        List<String> guesses
) {
}
