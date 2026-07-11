package enterprises.iwakura.amitracker.object;

import lombok.Data;

@Data
public class LimitationDTO {

    private int maxWishlists = 1;
    private int maxWishlistEntriesPerList = 10;
    private int maxChannelProductListQueries = 5;

}
