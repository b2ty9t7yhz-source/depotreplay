package dev.depotreplay.core.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Stable JSON bytes and SHA-256 hashes for state and artifact verification. */
public final class CanonicalJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();

    private CanonicalJson() { }

    public static byte[] bytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException error) {
            throw new DepotReplayException("Could not serialize canonical JSON", error);
        }
    }

    public static String string(Object value) {
        return new String(bytes(value), StandardCharsets.UTF_8);
    }

    public static String sha256(Object value) {
        return sha256(bytes(value));
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", error);
        }
    }

    public static <T> T read(Path path, Class<T> type) {
        try {
            return MAPPER.readValue(Files.readAllBytes(path), type);
        } catch (IOException error) {
            throw new DepotReplayException("Could not read JSON from " + path, error);
        }
    }

    public static <T> T read(byte[] bytes, Class<T> type) {
        try {
            return MAPPER.readValue(bytes, type);
        } catch (IOException error) {
            throw new DepotReplayException("Could not parse JSON", error);
        }
    }

    public static void write(Path path, Object value) {
        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
            try {
                Files.write(temporary, bytes(value));
                try {
                    Files.move(
                            temporary,
                            absolute,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, absolute, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException error) {
            throw new DepotReplayException("Could not write JSON to " + absolute, error);
        }
    }
}
