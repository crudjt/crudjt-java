package crudjt_errors;

public class InvalidState extends RuntimeException {
    public InvalidState(String message) {
        super(message);
    }
}
