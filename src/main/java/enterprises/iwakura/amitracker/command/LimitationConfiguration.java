package enterprises.iwakura.amitracker.command;

import lombok.Data;

@Data
public class LimitationConfiguration {

    private int maxWishlists = 1;
    private int maxWishlistEntriesPerList = 10;
    private int maxChannelProductListQueries = 5;

}
