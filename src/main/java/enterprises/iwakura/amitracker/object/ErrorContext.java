package enterprises.iwakura.amitracker.object;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ErrorContext {

    private final Type type;
    private final String detail;

    public static ErrorContext of(Type type, String detail) {
        return new ErrorContext(type, detail);
    }

    public static ErrorContext success() {
        return new ErrorContext(Type.SUCCESS, null);
    }

    public boolean isSuccess() {
        return this.type == Type.SUCCESS;
    }

    public enum Type {
        SUCCESS,
        PRODUCT_NOT_FOUND,
        WISHLIST_NOT_FOUND,
        PRODUCT_ALREADY_IN_WISHLIST,
        WISHLIST_ENTRY_NOT_ADDED,
        WISHLIST_ALREADY_EXISTS,
        CHANNEL_PRODUCT_LIST_NOT_FOUND, SEARCH_PARAMETERS_EMPTY;
    }
}
