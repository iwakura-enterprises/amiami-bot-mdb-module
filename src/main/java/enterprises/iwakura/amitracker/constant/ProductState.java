package enterprises.iwakura.amitracker.constant;

import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse.ResultItem;
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
    public static ProductState parse(AmiAmiItemResponse.Item item) {
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
        } else if (item.getOrderClosedFlag() == 1) {
            return ORDER_CLOSED;
        } else if (item.getStock() == 1) {
            return IN_STOCK;
        }
        return ORDER_CLOSED;
    }

    /**
     * Parses the product state from the given AmiAmi search result item
     *
     * @param item Item
     *
     * @return parsed product state
     */
    public static ProductState parse(ResultItem item) {
        if (item.getPreOrderItem() == 1) {
            if (item.getListPreorderAvailable() == 1) {
                return PRE_ORDER_AVAILABLE;
            } else {
                return PRE_ORDER_CLOSED;
            }
        } else if (item.getListBackorderAvailable() == 1) {
            if (item.getInStockFlag() == 1) {
                return BACK_ORDER_AVAILABLE;
            } else {
                return BACK_ORDER_CLOSED;
            }
        } else if (item.getPreownedSaleFlag() == 1) {
            return PRE_OWNED;
        } else if (item.getOrderClosedFlag() == 1) {
            return ORDER_CLOSED;
        } else if (item.getInStockFlag() == 1) {
            return IN_STOCK;
        }
        return ORDER_CLOSED;
    }

    @Override
    public String toString() {
        switch (this) {
            case PRE_ORDER_AVAILABLE -> {
                return "Pre-order available";
            }
            case PRE_ORDER_CLOSED -> {
                return "Pre-order closed";
            }
            case BACK_ORDER_AVAILABLE -> {
                return "Back-order available";
            }
            case BACK_ORDER_CLOSED -> {
                return "Back-order closed";
            }
            case PRE_OWNED -> {
                return "Pre-owned";
            }
            case IN_STOCK -> {
                return "In stock";
            }
            case ORDER_CLOSED -> {
                return "Order closed";
            }
            default -> {
                return "Unknown state";
            }
        }
    }
}
