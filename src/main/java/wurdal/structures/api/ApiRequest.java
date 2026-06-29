package wurdal.structures.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = AuthRequest.class, name = "auth-request"),
		@JsonSubTypes.Type(value = CredentialsRequest.class, name = "credentials-request"),
		@JsonSubTypes.Type(value = GuessReq.class, name = "guess-request"),
		@JsonSubTypes.Type(value = RegisterReq.class, name = "register-request")
})
public interface ApiRequest {
}
