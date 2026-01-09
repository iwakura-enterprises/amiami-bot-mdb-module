package enterprises.iwakura.amitracker.service;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.database.repository.WishlistEntryRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistRepository;
import enterprises.iwakura.amitracker.object.ErrorContext;
import enterprises.iwakura.amitracker.object.ErrorContext.Type;
import enterprises.iwakura.amitracker.object.WishlistChoice;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

@Bean
@Slf4j
@RequiredArgsConstructor
public class WishlistService {

    public static final WishlistEntity DEFAULT_WISHLIST_ENTITY_PLACEHOLDER = WishlistEntity.createDefault();

    private final ProductService productService;
    private final WishlistRepository wishlistRepository;
    private final WishlistEntryRepository wishlistEntryRepository;

    /**
     * Finds a wishlist for a specific user by wishlist name.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     *
     * @return an Optional containing the WishlistEntity if found, or empty if not found
     */
    public Optional<WishlistEntity> findWishlistForUser(long userId, String wishlistName) {
        return wishlistRepository.findByUserIdAndName(userId, wishlistName);
    }

    /**
     * Adds a product to a user's wishlist.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     * @param productCode  the code of the product to add
     *
     * @return an ErrorContext indicating success or the type of error encountered
     */
    public ErrorContext addProductToWishlist(long userId, String wishlistName, String productCode) {
        var optionalWishlist = findWishlistForUser(userId, wishlistName);

        if (optionalWishlist.isPresent()) {
            var wishlist = optionalWishlist.get();

            if (!wishlist.containsProductCode(productCode)) {
                var optionalProduct = productService.getOrQueryProduct(productCode);

                if (optionalProduct.isPresent()) {
                    var product = optionalProduct.get();

                    var wishlistEntry = wishlistEntryRepository.createWishlistEntry(wishlist, product);

                    if (wishlistEntry.isPresent()) {
                        return ErrorContext.success();
                    } else {
                        return ErrorContext.of(Type.WISHLIST_ENTRY_NOT_ADDED, productCode);
                    }
                } else {
                    return ErrorContext.of(Type.PRODUCT_NOT_FOUND, productCode);
                }
            } else {
                return ErrorContext.of(Type.PRODUCT_ALREADY_IN_WISHLIST, productCode);
            }
        } else {
            return ErrorContext.of(Type.WISHLIST_NOT_FOUND, wishlistName);
        }
    }

    /**
     * Suggests wishlist names for a user based on a partial wishlist name. Includes a default placeholder if no
     * wishlists are found.
     *
     * @param userId       the ID of the user
     * @param wishlistName the partial name of the wishlist
     *
     * @return a list of Choice objects representing the suggested wishlist names
     */
    public List<Choice> suggestWishlistNames(long userId, String wishlistName) {
        var wishlists = wishlistRepository.suggestWishlistNamesForUser(userId, wishlistName);
        if (wishlists.isEmpty()) {
            wishlists = List.of(DEFAULT_WISHLIST_ENTITY_PLACEHOLDER);
        }
        return wishlists.stream()
            .map(wishlist -> new WishlistChoice(wishlist).toChoice())
            .toList();
    }

    /**
     * Ensures that a default wishlist exists for the specified user. If it does not exist, creates one.
     *
     * @param userId the ID of the user
     */
    public void ensureDefaultWishlistExists(long userId) {
        wishlistRepository.ensureDefaultWishlistExistsForUser(userId);
    }
}
