package wurdal.structures.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = BoardRes.class, name = "board"),
		@JsonSubTypes.Type(value = BoardResError.class, name = "board-error")
})
public interface Board {

}