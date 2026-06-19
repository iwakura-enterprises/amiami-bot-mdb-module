package enterprises.iwakura.amitracker.service;

import java.time.OffsetDateTime;
import java.util.List;

import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.database.entity.BoughtProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.repository.BoughtProductRepository;
import enterprises.iwakura.amitracker.object.Page;
import enterprises.iwakura.amitracker.object.ProductChoice;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final DatabaseService databaseService;
    private final BoughtProductRepository boughtProductRepository;
    private final UserService userService;

    /**
     * Get all bought products for a specific user.
     *
     * @param userId the ID of the user
     *
     * @return list of BoughtProductEntity associated with the user
     */
    public List<BoughtProductEntity> getBoughtProductsForUser(long userId) {
        return boughtProductRepository.findByUserId(userId);
    }

    /**
     * Check if a user has bought a specific product.
     *
     * @param userId      User ID
     * @param productCode Product code
     *
     * @return true if the user has bought the product, false otherwise
     */
    public boolean hasUserBoughtProduct(long userId, String productCode) {
        return boughtProductRepository.hasUserBoughtProduct(userId, productCode);
    }

    /**
     * Add a bought product for a user.
     *
     * @param userId   User ID
     * @param product  Product entity
     * @param boughtAt Date and time of purchase
     * @param price    Price paid
     * @param currency Currency of the price
     *
     * @return The created BoughtProductEntity
     */
    public BoughtProductEntity addBoughProduct(
        @NonNull Long userId,
        @NonNull ProductEntity product,
        @NonNull OffsetDateTime boughtAt,
        @NonNull Long price,
        @NonNull Currency currency
    ) {
        return databaseService.runInThreadTransaction(session -> {
            var user = userService.getUserOrThrow(userId);
            return boughtProductRepository.save(new BoughtProductEntity(
                user, product, boughtAt, price, currency
            ));
        });
    }

    /**
     * Get bought product codes for a user filtered by a search string.
     *
     * @param userId               User ID
     * @param searchingProductCode Search string for product codes
     *
     * @return List of matching product codes
     */
    public List<Choice> suggestBoughtProducts(long userId, String searchingProductCode) {
        var products = boughtProductRepository.suggestBoughtProducts(userId, URLHelper.extractProductCode(searchingProductCode, true), OptionData.MAX_CHOICES);
        return products.stream()
            .map(product -> new ProductChoice(product).toChoice())
            .toList();
    }

    /**
     * Remove a product from a user's inventory.
     *
     * @param userId      User ID
     * @param productCode Product code
     *
     * @return true if the product was removed, false otherwise
     */
    public boolean removeProductFromInventory(long userId, String productCode) {
        return boughtProductRepository.removeBoughtProduct(userId, productCode);
    }

    /**
     * Get a paged view of the user's inventory.
     *
     * @param userId    User ID
     * @param pageSize  Number of items per page
     * @param pageIndex Index of the page to retrieve
     *
     * @return Paged list of BoughtProductEntity
     */
    public Page<BoughtProductEntity> getInventoryPage(long userId, int pageSize, int pageIndex) {
        return boughtProductRepository.findBoughtProductsPaged(userId, pageSize, pageIndex);
    }
}
