package wurdal.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import wurdal.structures.api.*;

import java.net.http.HttpClient;
import java.util.Locale;


public class ApiClient {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = System.getenv().getOrDefault("WURDAL_SERVER_URL", "http://localhost:8080");
    }

    // example payload {"name": "Alice"}
    public ApiResponse register(String username) throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(GenError.class));
        }
    }

    public AuthResponse login(String username) throws ApiException {
        try {
            RestTemplate restTemplate = new RestTemplate();
//            HttpHeaders headers = new HttpHeaders();
//            SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
            HttpEntity<CredentialsRequest> entity = new HttpEntity<>(new CredentialsRequest(username));
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    baseUrl + "/session",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(GenError.class));
        }
    }

    public MessageResponse logout(String sessionId) throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(GenError.class));
        }
    }

    public BoardRes newGame() throws ApiException {
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

    public Board board(Integer playerId) throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(BoardResError.class));
        }
    }

    public Board board() throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(BoardResError.class));
        }
    }

    public Board guess(Integer playerId, String word) throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(BoardResError.class));
        }
    }

    public Board guess(String word) throws ApiException {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            SessionStore.getInstance().read().ifPresent(headers::setBearerAuth);
            HttpEntity<GuessReq> entity = new HttpEntity<>(new GuessReq(word), headers);
            ResponseEntity<Board> response = restTemplate.exchange(
                    baseUrl + "/guess",
                    HttpMethod.POST,
                    entity,
                    Board.class
            );
            return response.getBody();
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), hcee.getResponseBodyAs(BoardResError.class));
        }
    }

    public LeaderBoard leaderboard() throws ApiException {
        RestTemplate restTemplate = new RestTemplate();
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), new GenError("looks like the server is down"));
        }

    }

    public Links links() throws ApiException {
        try {
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
        } catch (HttpClientErrorException hcee) {
            throw toApiException(hcee.getStatusCode().value(), new GenError("looks like the server is down"));
        }
    }

    public Integer getId(String playerName) {
        RestTemplate restTemplate = new RestTemplate();
        JsonNode response = restTemplate.getForObject(
                baseUrl + "/getId/" + playerName.trim().toLowerCase(Locale.ROOT),
                JsonNode.class
        );

        if (response == null || !response.has("id")) {
            return null;
        }

        int id = response.get("id").asInt();
        return id == -2 ? null : id;
    }

    private ApiException toApiException(int statusCode, ApiError error) {
        return new ApiException(statusCode, error);
    }

    public static class ApiException extends Throwable {
        private final int statusCode;
        private final ApiError error;

        public ApiException(int statusCode, ApiError error) {
            this.statusCode = statusCode;
            this.error = error;
        }

        public int statusCode() {
            return statusCode;
        }

        public ApiError error() {
            return error;
        }
    }
}
