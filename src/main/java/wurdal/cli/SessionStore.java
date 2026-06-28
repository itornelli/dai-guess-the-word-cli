package wurdal.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


//Could save this in a system register instead
public class SessionStore {
    private static final SessionStore INSTANCE = new SessionStore();
    private final Path sessionFile;

    private SessionStore() {
        this.sessionFile = Paths.get(System.getProperty("user.home"), ".wurdal", "session-id");
    }

    public static SessionStore getInstance() {
        return INSTANCE;
    }

    public Optional<String> read() {
        if (!Files.exists(sessionFile)) {
            return Optional.empty();
        }
        try {
            String value = Files.readString(sessionFile, StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void write(String sessionId) {
        try {
            Files.createDirectories(sessionFile.getParent());
            Files.writeString(sessionFile, sessionId, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist CLI session", e);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clear CLI session", e);
        }
    }
}
