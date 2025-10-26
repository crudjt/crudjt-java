import crudjt_errors.InternalError;
import crudjt_errors.DonateException;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

public class CRUD_JT_Errors {
    public static final Map<String, Class<? extends RuntimeException>> ERRORS = new HashMap<>();

    static {
        ERRORS.put("XX000", InternalError.class);
        ERRORS.put("DE000", DonateException.class);
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
