package wurdal.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import wurdal.structures.api.AuthResponse;
import wurdal.structures.api.CredentialsRequest;
import wurdal.structures.api.ErrorResponse;
import wurdal.structures.api.MessageResponse;
import wurdal.structures.api.BoardRes;
import wurdal.structures.api.GuessReq;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ApiClient {
    private static final String SESSION_HEADER = "X-Session-Id";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = System.getenv().getOrDefault("WURDAL_SERVER_URL", "http://localhost:8080");
    }

    public AuthResponse register(String username, String password) {
        return sendJson("POST", "/register", null, new CredentialsRequest(username, password), AuthResponse.class);
    }

    public AuthResponse login(String username, String password) {
        return sendJson("POST", "/login", null, new CredentialsRequest(username, password), AuthResponse.class);
    }

    public MessageResponse logout(String sessionId) {
        return sendJson("POST", "/logout", sessionId, null, MessageResponse.class);
    }

    public BoardRes board(String sessionId) {
        return sendJson("GET", "/board", sessionId, null, BoardRes.class);
    }

    public BoardRes guess(String sessionId, String word) {
        return sendJson("POST", "/guess", sessionId, new GuessReq(word), BoardRes.class);
    }

    private <T> T sendJson(String method, String path, String sessionId, Object requestBody, Class<T> responseType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json");

            if (sessionId != null && !sessionId.isBlank()) {
                builder.header(SESSION_HEADER, sessionId);
            }

            if ("GET".equalsIgnoreCase(method)) {
                builder.GET();
            } else {
                String body = requestBody == null ? "" : mapper.writeValueAsString(requestBody);
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status >= 200 && status < 300) {
                if (responseType == MessageResponse.class && body.isBlank()) {
                    return responseType.cast(new MessageResponse("Successfully logged out"));
                }
                return mapper.readValue(body, responseType);
            }
            throw toApiException(status, body);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call server at " + baseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Server request interrupted", e);
        }
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
