package wurdal.structures.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = AuthResponse.class, name = "auth-response"),
		@JsonSubTypes.Type(value = BoardRes.class, name = "board"),
		@JsonSubTypes.Type(value = BoardResError.class, name = "board-error"),
		@JsonSubTypes.Type(value = ErrorResponse.class, name = "error"),
		@JsonSubTypes.Type(value = GenError.class, name = "generic-error"),
		@JsonSubTypes.Type(value = LeaderBoard.class, name = "leaderboard"),
		@JsonSubTypes.Type(value = Links.class, name = "links"),
		@JsonSubTypes.Type(value = MessageResponse.class, name = "message"),
		@JsonSubTypes.Type(value = RegisterRes.class, name = "register-response")
})
public interface ApiResponse {
}
