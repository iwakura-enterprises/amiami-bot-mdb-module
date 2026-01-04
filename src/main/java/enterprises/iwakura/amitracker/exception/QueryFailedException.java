package enterprises.iwakura.amitracker.exception;

public class QueryFailedException extends RuntimeException {

    public QueryFailedException(String message, Exception cause) {
        super(message, cause);
    }
}
