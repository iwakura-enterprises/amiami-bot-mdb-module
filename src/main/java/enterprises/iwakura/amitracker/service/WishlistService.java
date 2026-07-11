package enterprises.iwakura.amitracker.service;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductChangeAnnouncementRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistEntryRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistRepository;
import enterprises.iwakura.amitracker.object.ErrorContext;
import enterprises.iwakura.amitracker.object.ErrorContext.Type;
import enterprises.iwakura.amitracker.object.Page;
import enterprises.iwakura.amitracker.object.ProductChoice;
import enterprises.iwakura.amitracker.object.WishlistChoice;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
@Slf4j
@RequiredArgsConstructor
public class WishlistService {

    public static final WishlistEntity DEFAULT_WISHLIST_ENTITY_PLACEHOLDER = WishlistEntity.createDefault();

    private final ProductService productService;
    private final LimitationService limitationService;

    private final WishlistRepository wishlistRepository;
    private final WishlistEntryRepository wishlistEntryRepository;
    private final ProductChangeAnnouncementRepository productChangeAnnouncementRepository;
    private final DatabaseService databaseService;

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
            var limitations = limitationService.findEffectiveLimitationsForUser(userId);
            var numberOfEntriesInWishlist = wishlistEntryRepository.countEntriesForWishlist(wishlist.getId());

            if (numberOfEntriesInWishlist < limitations.getMaxWishlistEntriesPerList()) {
                if (!wishlistRepository.isProductInWishlist(wishlist.getId(), productCode)) {
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
                return ErrorContext.of(Type.WISHLIST_ENTRY_LIMIT_REACHED, String.valueOf(limitations.getMaxWishlistEntriesPerList()));
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
        return databaseService.runInThreadTransaction(session -> {
            var wishlists = wishlistRepository.suggestWishlistNamesForUser(userId, wishlistName);
            if (wishlists.isEmpty()) {
                wishlists = List.of(DEFAULT_WISHLIST_ENTITY_PLACEHOLDER);
            }
            return wishlists.stream()
                .map(wishlist -> new WishlistChoice(wishlist).toChoice())
                .toList();
        });
    }

    /**
     * Suggests product codes in a user's wishlist based on a search string.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     * @param productCode  the search string for product codes
     *
     * @return a list of Choice objects representing the suggested product codes
     */
    public List<Choice> suggestProductCodesInWishlist(long userId, String wishlistName, String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            var optionalWishlist = findWishlistForUser(userId, wishlistName);
            if (optionalWishlist.isEmpty()) {
                return List.of();
            }
            var wishlist = optionalWishlist.get();
            var entries = wishlistEntryRepository.suggestProductsInWishlist(
                userId, wishlist.getId(), productCode, OptionData.MAX_CHOICES);
            return entries.stream()
                .map(entry -> new ProductChoice(entry.getProduct()).toChoice())
                .toList();
        });
    }

    /**
     * Ensures that a default wishlist exists for the specified user. If it does not exist, creates one.
     *
     * @param userId the ID of the user
     */
    public void ensureDefaultWishlistExists(long userId) {
        wishlistRepository.ensureDefaultWishlistExistsForUser(userId);
    }

    /**
     * Retrieves a paginated list of wishlist entries for a specific user and wishlist.
     *
     * @param userId     the ID of the user
     * @param wishlistId the ID of the wishlist
     * @param pageSize   the number of entries per page
     * @param pageIndex  the index of the page to retrieve
     *
     * @return a Page object containing the wishlist entries
     */
    public Page<WishlistEntryEntity> getInventoryPage(long userId, long wishlistId, int pageSize, int pageIndex) {
        return wishlistEntryRepository.findWishlistEntriesPaged(userId, wishlistId, pageSize, pageIndex);
    }

    /**
     * Removes a product from a user's wishlist.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     * @param productCode  the code of the product to remove
     *
     * @return true if the product was removed, false otherwise
     */
    public boolean removeProductFromWishlist(long userId, String wishlistName, String productCode) {
        return wishlistEntryRepository.removeWishlistEntry(userId, wishlistName, productCode);
    }

    /**
     * Creates a new wishlist for a user.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist to create
     *
     * @return an ErrorContext indicating success or if the wishlist already exists
     */
    public ErrorContext createWishlist(long userId, String wishlistName) {
        var optionalWishlist = findWishlistForUser(userId, wishlistName);

        if (optionalWishlist.isPresent()) {
            return ErrorContext.of(Type.WISHLIST_ALREADY_EXISTS, wishlistName);
        } else {
            var limitations = limitationService.findEffectiveLimitationsForUser(userId);
            var numberOfExistingWishlists = wishlistRepository.countForUserId(userId);

            if (numberOfExistingWishlists >= limitations.getMaxWishlists()) {
                return ErrorContext.of(Type.WISHLIST_LIMIT_REACHED, String.valueOf(limitations.getMaxWishlists()));
            }

            wishlistRepository.createWishlist(userId, wishlistName);
            return ErrorContext.success();
        }
    }

    /**
     * Deletes a wishlist for a user.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist to delete
     *
     * @return an ErrorContext indicating success or if the wishlist was not found
     */
    public ErrorContext deleteWishlist(long userId, String wishlistName) {
        return databaseService.runInThreadTransaction(session -> {
            var optionalWishlist = findWishlistForUser(userId, wishlistName);

            if (optionalWishlist.isPresent()) {
                var wishlist = optionalWishlist.get();
                productChangeAnnouncementRepository.deleteAll(wishlist.getProductChangeAnnouncements());
                wishlistRepository.delete(wishlist);
                return ErrorContext.success();
            } else {
                return ErrorContext.of(Type.WISHLIST_NOT_FOUND, wishlistName);
            }
        });
    }

    /**
     * Saves the given wishlist entity to the database.
     *
     * @param wishlist the WishlistEntity to save
     */
    public void saveWishlist(WishlistEntity wishlist) {
        wishlistRepository.save(wishlist);
    }
}
