// This binding was generated automatically to ensure consistency across languages
// Generated using ChatGPT (GPT-5) from the canonical Ruby SDK
// API is stable and production-ready

import crudjt_errors.InternalError;
import crudjt_errors.InvalidState;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

public class CRUDJT_Errors {
    public static final Map<String, Class<? extends RuntimeException>> ERRORS = new HashMap<>();

    static {
        ERRORS.put("XX000", InternalError.class);
        ERRORS.put("55JT01", InvalidState.class);
    }

    public static RuntimeException createErrorByCode(String code, String message) {
        Class<? extends RuntimeException> cls = ERRORS.get(code);

        if (cls != null) {
            try {
                return cls.getConstructor(String.class).newInstance(message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create error instance for code: " + code, e);
            }
        }

        return new RuntimeException("Unknown error (" + code + "): " + message);
    }


}
