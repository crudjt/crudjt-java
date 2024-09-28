import java.util.Map;

public class Validation {
    private static final long U64_MAX = (long) Math.pow(2, 64) - 1;

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

    public static void validatetoken(Object token) {
        if (!(token instanceof String)) {
            throw new IllegalArgumentException("token must be a String");
        }

        if (((String) token).isEmpty()) {
            throw new IllegalArgumentException("token can't be blank");
        }
    }
}
