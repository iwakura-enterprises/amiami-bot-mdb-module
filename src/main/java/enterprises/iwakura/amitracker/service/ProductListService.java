package enterprises.iwakura.amitracker.service;

import java.util.List;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.repository.ChannelListProductQueryRepository;
import enterprises.iwakura.amitracker.database.repository.ChannelRepository;
import enterprises.iwakura.amitracker.database.repository.ProductListQueryRepository;
import enterprises.iwakura.amitracker.object.ErrorContext;
import enterprises.iwakura.amitracker.object.ErrorContext.Type;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

@Slf4j
@Bean
@RequiredArgsConstructor
public class ProductListService {

    private final ProductListQueryRepository productListQueryRepository;
    private final ChannelListProductQueryRepository channelListProductQueryRepository;
    private final ChannelRepository channelRepository;
    private final DatabaseService databaseService;

    /**
     * Gets channel product lists by channel ID
     *
     * @param channelId Channel ID
     *
     * @return List of ChannelProductListQueryEntity
     */
    public List<ChannelProductListQueryEntity> getChannelProductLists(long channelId) {
        return channelListProductQueryRepository.findAllByChannelId(channelId);
    }

    /**
     * Creates new channel product list query. Uses existing product list queries if found.
     *
     * @param channel                 Channel
     * @param entity                  ChannelProductListQueryEntity
     * @param productSearchParameters ProductSearchParameters
     *
     * @return Error context determining whenever the operation was successful.
     */
    public ErrorContext createChannelProductListQuery(
        GuildChannel channel,
        ChannelProductListQueryEntity entity,
        ProductSearchParameters productSearchParameters
    ) {
        if (productSearchParameters.isEmpty()) {
            return ErrorContext.of(Type.SEARCH_PARAMETERS_EMPTY, "Search parameters are empty");
        }

        return databaseService.runInThreadTransaction(session -> {
            var channelEntity = channelRepository.getOrCreate(channel);
            var productListQuery = productListQueryRepository.getOrCreate(productSearchParameters);
            entity.setChannel(channelEntity);
            entity.setProductListQuery(productListQuery);
            log.info("Creating ChannelProductListQueryEntity in channel {} with parameters {} with product list query {}",
                channelEntity.getId(), productSearchParameters, productListQuery.getId()
            );
            channelListProductQueryRepository.save(entity);

            return ErrorContext.success();
        });
    }

    /**
     * Deletes {@link ChannelProductListQueryEntity}. If the {@link ProductListQueryEntity} holding the
     * {@link ChannelProductListQueryEntity} has no other channels and is not of global template, it will be deleted as well.
     *
     * @param entityId Entity ID
     *
     * @return Error context determining whenever the operation was successful.
     */
    public ErrorContext deleteChannelProductListQuery(long entityId) {
        var optionalEntity = channelListProductQueryRepository.findById(entityId);

        if (optionalEntity.isEmpty()) {
            return ErrorContext.of(Type.CHANNEL_PRODUCT_LIST_NOT_FOUND, "Channel product list with ID %d not found".formatted(entityId));
        }

        var entity = optionalEntity.get();
        var productListQuery = entity.getProductListQuery();
        var containsEntity = productListQuery.getChannelsWithQuery().stream().anyMatch(otherEntity -> otherEntity.getId().equals(entityId));

        if (!productListQuery.isGlobalTemplate() && productListQuery.getChannelsWithQuery().size() <= 1 && containsEntity) {
            log.info("Deleting non-global template product list query {} because its last channel product list query {} is being deleted",
                productListQuery.getId(), entity.getId()
            );

            productListQueryRepository.delete(productListQuery);
        }

        log.info("Deleting channel product list query {}", entity.getId());
        channelListProductQueryRepository.delete(entity);
        return ErrorContext.success();
    }

    public void save(ChannelProductListQueryEntity entity) {
        channelListProductQueryRepository.save(entity);
    }
}
