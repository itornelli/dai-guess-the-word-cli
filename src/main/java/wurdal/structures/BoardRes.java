package wurdal.structures;

import java.util.List;

public record BoardRes(
        int playerId,
        String playerName,
        int wordLength,
        String hiddenWord,
        List<String> guesses
) {
}
