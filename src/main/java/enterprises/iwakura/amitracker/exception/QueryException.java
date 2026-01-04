package enterprises.iwakura.amitracker.exception;

public class QueryException extends RuntimeException {

    public QueryException(String message, Exception cause) {
        super(message, cause);
    }
}
