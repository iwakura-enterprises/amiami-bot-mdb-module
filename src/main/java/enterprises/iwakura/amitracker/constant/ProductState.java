package enterprises.iwakura.amitracker.constant;

import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductState {
    PRE_ORDER_AVAILABLE(true), // ok
    PRE_ORDER_CLOSED(false), // ok
    BACK_ORDER_AVAILABLE(true), // ok
    BACK_ORDER_CLOSED(false), // ok
    PRE_OWNED(true), // ok
    IN_STOCK(true), // ok
    ORDER_CLOSED(false);

    private final boolean inStock;

    /**
     * Parses the product state from the given AmiAmi item.
     *
     * @param item the AmiAmi item
     *
     * @return the parsed ProductState
     */
    public static ProductState parse(Item item) {
        if (item.getPreOrderItem() == 1) {
            if (item.getStock() == 1) {
                return PRE_ORDER_AVAILABLE;
            } else {
                return PRE_ORDER_CLOSED;
            }
        } else if (item.getBackOrderItem() == 1) {
            if (item.getStock() == 1) {
                return BACK_ORDER_AVAILABLE;
            } else {
                return BACK_ORDER_CLOSED;
            }
        } else if (item.getPreownAttention() == 1) {
            return PRE_OWNED;
        } else if (item.getStock() == 1) {
            return IN_STOCK;
        }

        return ORDER_CLOSED;
    }
}
