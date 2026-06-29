package wurdal.structures.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = RegisterReq.class, name = "register-request"),
		@JsonSubTypes.Type(value = RegisterRes.class, name = "register-response")
})
public interface Register {
}
