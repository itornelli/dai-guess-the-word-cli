package wurdal.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import wurdal.structures.api.*;

import java.net.http.HttpClient;
import java.util.Optional;


public class ApiClient {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = System.getenv().getOrDefault("WURDAL_SERVER_URL", "http://localhost:8080");
    }

    // example payload {"name": "Alice"}
    public RegisterRes register(String username) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<RegisterReq> entity = new HttpEntity<>(new RegisterReq(username), headers);
        ResponseEntity<RegisterRes> response = restTemplate.exchange(
                baseUrl + "/player",
                HttpMethod.POST,
                entity,
                RegisterRes.class
        );
        return response.getBody();
    }

    public AuthResponse login(String username) {
        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<CredentialsRequest> entity = new HttpEntity<>(new CredentialsRequest(username));
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/session",
                HttpMethod.POST,
                entity,
                AuthResponse.class
        );
        return response.getBody();
    }

    public MessageResponse logout(String sessionId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<MessageResponse> response = restTemplate.exchange(
                baseUrl + "/session" + "/" + sessionId,
                HttpMethod.GET,
                entity,
                MessageResponse.class
        );
        return response.getBody();
    }

    public BoardRes newGame() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<BoardRes> response = restTemplate.exchange(
                baseUrl + "/new-game",
                HttpMethod.POST,
                entity,
                BoardRes.class
        );
        return response.getBody();
    }

    public Board board(Integer playerId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Board> response = restTemplate.exchange(
                baseUrl + "/" + playerId + "/board",
                HttpMethod.GET,
                entity,
                Board.class
        );
        return response.getBody();
    }

    public Board board() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Board> response = restTemplate.exchange(
                baseUrl + "/board",
                HttpMethod.GET,
                entity,
                Board.class
        );
        return response.getBody();
    }

    public Board guess(Integer playerId, String word) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<GuessReq> entity = new HttpEntity<>(new GuessReq(word), headers);
        ResponseEntity<Board> response = restTemplate.exchange(
                baseUrl + "/" + playerId + "/guess",
                HttpMethod.POST,
                entity,
                Board.class
        );
        return response.getBody();
    }

    public Board guess(String word) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<GuessReq> entity = new HttpEntity<>(new GuessReq(word), headers);
        ResponseEntity<Board> response = restTemplate.exchange(
                baseUrl  + "/guess",
                HttpMethod.POST,
                entity,
                Board.class
        );
        return response.getBody();
    }

    public LeaderBoard leaderboard() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<LeaderBoard> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                entity,
                LeaderBoard.class
        );
        return response.getBody();
    }

    public Links links() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Links> response = restTemplate.exchange(
                baseUrl + "/",
                HttpMethod.GET,
                entity,
                Links.class
        );
        return response.getBody();
    }

    public Integer getId(String playerName) {
        RestTemplate restTemplate = new RestTemplate();
        JsonNode response = restTemplate.getForObject(
                baseUrl + "/getId/" + playerName,
                JsonNode.class
        );
        return (response != null && response.has("id"))? response.get("id").asInt() : null;
    }

    private ApiException toApiException(int statusCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ApiException(statusCode, new ErrorResponse("Request failed", null));
        }
        try {
            ErrorResponse error = mapper.readValue(responseBody, ErrorResponse.class);
            return new ApiException(statusCode, error);
        } catch (JsonProcessingException e) {
            return new ApiException(statusCode, new ErrorResponse(responseBody, null));
        }
    }

    public static class ApiException extends RuntimeException {
        private final int statusCode;
        private final ErrorResponse error;

        public ApiException(int statusCode, ErrorResponse error) {
            super(error.message());
            this.statusCode = statusCode;
            this.error = error;
        }

        public int statusCode() {
            return statusCode;
        }

        public ErrorResponse error() {
            return error;
        }
    }
}
