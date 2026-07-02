package wurdal.structures.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterRes(Integer id, String name, @JsonProperty("_links") RegisterLinks links) implements ApiResponse {
    public record RegisterLinks(Links.Board board, Links.Guess guess) {}
}
