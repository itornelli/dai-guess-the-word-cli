package wurdal.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import wurdal.structures.api.*;

public class ApiClient {

    private static final String SERVER_DOWN_MESSAGE =
            "Looks like the wurdal servers are taking a loss... try again later!";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = System.getenv().getOrDefault("WURDAL_SERVER_URL", "http://localhost:8080");
    }

    public RegisterRes register(String username) {
        try {
            HttpEntity<RegisterReq> entity = new HttpEntity<>(new RegisterReq(username));
            ResponseEntity<RegisterRes> response = restTemplate.exchange(
                    baseUrl + "/players",
                    HttpMethod.POST,
                    entity,
                    RegisterRes.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, new ErrorResponse(SERVER_DOWN_MESSAGE, null));
        }
    }

    public AuthResponse login(String username) {
        try {
            HttpEntity<CredentialsRequest> entity = new HttpEntity<>(new CredentialsRequest(username));
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    baseUrl + "/sessions",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, new ErrorResponse(SERVER_DOWN_MESSAGE, null));
        }
    }

    public Board board(Integer playerId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(String.valueOf(playerId));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Board> response = restTemplate.exchange(
                    baseUrl + "/players/" + playerId + "/board",
                    HttpMethod.GET,
                    entity,
                    Board.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, new ErrorResponse(SERVER_DOWN_MESSAGE, null));
        }
    }

    public Board guess(Integer playerId, String word) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(String.valueOf(playerId));
            HttpEntity<GuessReq> entity = new HttpEntity<>(new GuessReq(word), headers);
            ResponseEntity<Board> response = restTemplate.exchange(
                    baseUrl + "/players/" + playerId + "/guess",
                    HttpMethod.POST,
                    entity,
                    Board.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, new ErrorResponse(SERVER_DOWN_MESSAGE, null));
        }
    }

    public LeaderBoard leaderboard() {
        try {
            ResponseEntity<LeaderBoard> response = restTemplate.exchange(
                    baseUrl + "/leaderboard",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    LeaderBoard.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, new ErrorResponse(SERVER_DOWN_MESSAGE, null));
        }
    }

    private ApiException toApiException(HttpClientErrorException e) {
        try {
            JsonNode root = mapper.readTree(e.getResponseBodyAsString());
            // Handle nested {"error": {"description": "..."}} format (API errors)
            if (root.has("error") && root.get("error").has("description")) {
                String description = root.get("error").get("description").asText();
                return new ApiException(e.getStatusCode().value(), new ErrorResponse(description, null));
            }
            // Handle flat {"message": "...", "registerCommand": "..."} format
            ErrorResponse error = mapper.treeToValue(root, ErrorResponse.class);
            return new ApiException(e.getStatusCode().value(), error);
        } catch (Exception ex) {
            return new ApiException(e.getStatusCode().value(), new ErrorResponse(e.getMessage(), null));
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

        public int statusCode() { return statusCode; }
        public ErrorResponse error() { return error; }
    }
}
