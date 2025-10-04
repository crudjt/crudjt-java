import java.util.Base64;
import java.util.Map;

public class CRUD_JT_Validation {
    private static final long U64_MAX = (long) Math.pow(2, 64) - 1;

    public static final int MAX_HASH_SIZE = 256;

    public static final int ERROR_ALREADY_STARTED = 0;
    public static final int ERROR_NOT_STARTED = 1;
    public static final int ERROR_ENCRYPTED_KEY_NOT_SET = 2;

    public static final Map<Integer, String> ERROR_MESSAGES = Map.of(
        ERROR_ALREADY_STARTED, "CRUD_JT already started",
        ERROR_NOT_STARTED, "CRUD_JT has not started",
        ERROR_ENCRYPTED_KEY_NOT_SET, "Encrypted key is blank"
    );

    public static String errorMessage(int code) {
        return ERROR_MESSAGES.getOrDefault(code, "Unknown error (" + code + ")");
    }

    public static void validateHashBytesize(int hashBytesize) {
        if (hashBytesize > MAX_HASH_SIZE) {
            throw new IllegalArgumentException(
                "Hash can not be bigger than " + MAX_HASH_SIZE + " bytesize"
            );
        }
    }

    public static void validateToken(Object token) {
        if (!(token instanceof String)) {
            throw new IllegalArgumentException("token must be String");
        }
        String str = (String) token;
        if (str.isEmpty()) {
            throw new IllegalArgumentException("token cant be blank");
        }
    }

    public static void validateEncrypted_key(String key) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'encrypted_key' must be a valid Base64 string");
        }

        int size = decoded.length;
        if (!(size == 32 || size == 48 || size == 64)) {
            throw new IllegalArgumentException(
                "'encrypted_key' must be exactly 32, 48, or 64 bytes. Got " + size + " bytes"
            );
        }
    }

    public static void validateInsertion(Map<String, Object> hash, long ttl, long silence_read) {
        if (!(hash instanceof Map)) {
            throw new IllegalArgumentException("Must be Hash");
        }

        if (ttl != -1 && (ttl <= 0 || ttl > U64_MAX)) {
            throw new IllegalArgumentException("ttl should be greater than 0 and less than 2^64");
        }

        if (silence_read != -1 && (silence_read < 1 || silence_read > U64_MAX)) {
            throw new IllegalArgumentException("silence_read should be greater than 0 and less than 2^64");
        }
    }
}
