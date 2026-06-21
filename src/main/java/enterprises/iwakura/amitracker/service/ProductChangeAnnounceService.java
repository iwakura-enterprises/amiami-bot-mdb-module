package enterprises.iwakura.amitracker.service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import enterprises.iwakura.amitracker.AmiTracker;
import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.constant.ProductChangeType;
import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.database.entity.ProductChangeAnnouncementEntity;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.UserEntity;
import enterprises.iwakura.amitracker.database.repository.ChannelListProductQueryRepository;
import enterprises.iwakura.amitracker.database.repository.ProductChangeAnnouncementRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistRepository;
import enterprises.iwakura.amitracker.object.MessageTarget;
import enterprises.iwakura.amitracker.object.ProductChangeHolder;
import enterprises.iwakura.cirno.ExceptionUtils;
import enterprises.iwakura.modularbot.ModularBotShardManager;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductChangeAnnounceService {

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-dd-MM hh:mm");
    private final static List<ProductChangeType> PRODUCT_ADDED_LIST = List.of(ProductChangeType.PRODUCT_LIST_NEW_PRODUCT);
    private final static Object DUMMY = new Object();

    private final AmiAmiApiService amiAmiApiService;
    private final ConfigurationService configurationService;

    private final DatabaseService databaseService;
    private final WishlistRepository wishlistRepository;
    private final ChannelListProductQueryRepository channelListProductQueryRepository;
    private final ProductChangeAnnouncementRepository productChangeAnnouncementRepository;

    private final ModularBotShardManager shardManager;

    @Bean
    private final BeanAccessor<ProductService> productService = new BeanAccessor<>(ProductService.class);

    private Cache<String, Object> recentlyPingedRolesInChannelCache;

    public void init() {
        log.info("Initializing ProductChangeAnnounceService...");
        var config = configurationService.getProductChangeAnnouncementConfiguration();
        recentlyPingedRolesInChannelCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMillis(config.getRecentRolePingBackoff()))
            .build();
    }

    /**
     * For the specified entity, finds corresponding wishlist entries or product list queries that contain
     * this product, checks their filtering, and schedules an announcement.
     *
     * @param productEntity       Product entity
     * @param productChangeHolder product change holder
     * @param productChangeTypes  changes
     */
    public void schedule(
        ProductEntity productEntity,
        List<ProductChangeType> productChangeTypes,
        ProductChangeHolder productChangeHolder
    ) {
        var wishlists = wishlistRepository.findWishlistsToNotify(productEntity, productChangeTypes);
        var channelProductListQueryEntries = channelListProductQueryRepository.findEntriesToNotify(
            productEntity,
            null,
            productChangeTypes,
            productChangeHolder
        );
        List<ProductChangeAnnouncementEntity> announcements = new ArrayList<>();

        log.debug("Found {} wishlists and {} channel product list queries to announce product code {} with changes {}",
            wishlists.size(), channelProductListQueryEntries.size(), productEntity.getCode(), productChangeTypes
        );

        announcements.addAll(wishlists.stream().map(wishlist -> productChangeAnnouncementRepository.save(
            ProductChangeAnnouncementEntity.builder()
                .announcementState(QueueState.QUEUED)
                .productChangeTypes(productChangeTypes)
                .productEntity(productEntity)
                .wishlist(wishlist)
                .productChangeHolder(productChangeHolder)
                .build()
        )).toList());

        announcements.addAll(channelProductListQueryEntries.stream()
            .map(channelProductListQueryEntity -> productChangeAnnouncementRepository.save(
                ProductChangeAnnouncementEntity.builder()
                    .announcementState(QueueState.QUEUED)
                    .productChangeTypes(productChangeTypes)
                    .productEntity(productEntity)
                    .channelProductListQuery(channelProductListQueryEntity)
                    .productChangeHolder(productChangeHolder)
                    .build()
            )).toList());

        log.info("Scheduled {} announcement(s) for product code {} with product change types {} and holder {}",
            announcements.size(), productEntity.getCode(), productChangeTypes, productChangeHolder
        );
    }

    /**
     * Schedules notification about added product in list
     *
     * @param productListQueryEntity product list query
     * @param productEntity          Product entity
     */
    public void scheduleProductAddedInList(ProductListQueryEntity productListQueryEntity, ProductEntity productEntity) {
        var channelProductListQueryEntries = channelListProductQueryRepository.findEntriesToNotify(
            productEntity,
            productListQueryEntity,
            PRODUCT_ADDED_LIST,
            null
        );

        var announcements = channelProductListQueryEntries.stream()
            .map(channelProductListQueryEntity -> productChangeAnnouncementRepository.save(
                ProductChangeAnnouncementEntity.builder()
                    .announcementState(QueueState.QUEUED)
                    .productChangeTypes(PRODUCT_ADDED_LIST)
                    .productEntity(productEntity)
                    .channelProductListQuery(channelProductListQueryEntity)
                    .build()
            )).toList();

        log.info("Scheduled {} announcement(s) for product code {} that newly appeared in {} product list queries",
            announcements.size(), productEntity.getCode(), channelProductListQueryEntries.size()
        );
    }

    /**
     * Finds all queued-up product announcements and sends them
     */
    public void sendQueuedProductChangeAnnouncements() {
        var queuedProductAnnouncements = databaseService.runInThreadTransaction(session -> {
            var announcements = productChangeAnnouncementRepository.findAllQueued();
            announcements.forEach(entity -> {
                entity.setAnnouncementState(QueueState.PROCESSING);
                productChangeAnnouncementRepository.save(entity);
            });
            return announcements;
        });

        if (queuedProductAnnouncements.isEmpty()) {
            log.debug("No queued product announcements.");
            return;
        }

        Map<MessageTarget, List<ProductChangeAnnouncementEntity>> groupedProductChangeAnnouncements = queuedProductAnnouncements.stream()
            .collect(Collectors.toMap(
                this::createMessageTarget,
                item -> new ArrayList<>(List.of(item)),
                (v1, v2) -> {
                    v1.addAll(v2);
                    return v1;
                }
            ));

        log.info("Found {} message targets for product change announcements (total of {} queued announcements)",
            groupedProductChangeAnnouncements.size(), queuedProductAnnouncements.size()
        );

        groupedProductChangeAnnouncements.forEach((target, targetAnnouncements) -> {
            // Changes for specific product, if more changes occurred between announcements
            Map<ProductEntity, List<ProductChangeAnnouncementEntity>> groupedProducts = targetAnnouncements.stream()
                .collect(Collectors.groupingBy(ProductChangeAnnouncementEntity::getProductEntity));

            // Embeds per product, multiple ProductChangeAnnouncementEntities get merged into one embed
            Map<ProductEntity, EmbedBuilder> embedByProduct = groupedProducts.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, v -> createEmbed(target.getGuild(), v.getKey(), v.getValue())));

            // Messages that will be sent out with the corresponding product change announcement entities
            Map<MessageCreateData, List<ProductChangeAnnouncementEntity>> messagesToSend = new HashMap<>();
            List<ProductChangeAnnouncementEntity> processedProductChangeAnnouncementEntities = new ArrayList<>();
            MessageCreateBuilder currentMessageBuilder = null;
            for (var entry : embedByProduct.entrySet()) {
                var product = entry.getKey();
                var embed = entry.getValue();

                // Save current message builder and set it to messagesToSend
                if (currentMessageBuilder != null && currentMessageBuilder.getEmbeds().size() == Message.MAX_EMBED_COUNT) {
                    messagesToSend.put(currentMessageBuilder.build(), processedProductChangeAnnouncementEntities);
                    currentMessageBuilder = null;
                    processedProductChangeAnnouncementEntities = null;
                }

                // Create message builder
                if (currentMessageBuilder == null) {
                    currentMessageBuilder = new MessageCreateBuilder();
                    processedProductChangeAnnouncementEntities = new ArrayList<>();

                    if (target.isForGuild() && target.getRolesToPing() != null && !target.getRolesToPing().isEmpty()) {
                        var guild = shardManager.get().getGuildById(target.getGuildId());

                        if (guild != null) {
                            specifyRolePings(guild, target, currentMessageBuilder);
                        } else {
                            handleFailure(new IllegalStateException("Guild with ID %d does not exist".formatted(target.getGuildId())), targetAnnouncements);
                            return;
                        }
                    }
                }

                processedProductChangeAnnouncementEntities.addAll(groupedProducts.get(product));
                currentMessageBuilder.addEmbeds(embed.build());
            }
            if (currentMessageBuilder != null && !currentMessageBuilder.getEmbeds().isEmpty()) {
                messagesToSend.put(currentMessageBuilder.build(), processedProductChangeAnnouncementEntities);
            }

            // Sends messages
            if (target.isForGuild()) {
                GuildChannel guildChannel;

                try {
                    guildChannel = shardManager.get().getGuildChannelById(target.getTargetId());
                } catch (Exception exception) {
                    handleFailure(exception, targetAnnouncements);
                    return;
                }

                if (guildChannel != null) {
                    if (guildChannel instanceof GuildMessageChannel guildMessageChannel) {
                        messagesToSend.forEach((messageCreateData, messageAnnouncements) ->
                            guildMessageChannel.sendMessage(messageCreateData)
                                .queue(message -> handleSuccess(message, messageAnnouncements),
                                    failure -> handleFailure(failure, messageAnnouncements)
                                ));
                    } else {
                        handleFailure(new IllegalStateException("Channel ID %d is not of GuildMessageChannel (is %s)".formatted(
                            target.getTargetId(), guildChannel.getType()
                        )), targetAnnouncements);
                    }
                } else {
                    handleFailure(new IllegalStateException("Channel ID %d not found".formatted(target.getTargetId())), targetAnnouncements);
                }
            } else {
                PrivateChannel privateChannel;

                try {
                    privateChannel = shardManager.get().retrieveUserById(target.getTargetId())
                        .complete()
                        .openPrivateChannel()
                        .complete();
                } catch (Exception exception) {
                    handleFailure(exception, targetAnnouncements);
                    return;
                }

                if (privateChannel != null) {
                    messagesToSend.forEach((messageCreateData, messageAnnouncements) ->
                        privateChannel.sendMessage(messageCreateData)
                            .queue(message -> handleSuccess(message, messageAnnouncements),
                                failure -> handleFailure(failure, messageAnnouncements)
                            ));
                } else {
                    handleFailure(new IllegalStateException("Private channel for user ID %d not found".formatted(target.getTargetId())), targetAnnouncements);
                }
            }
        });
    }

    private void specifyRolePings(Guild guild, MessageTarget target, MessageCreateBuilder currentMessageBuilder) {
        var roles = guild.getRoles().stream()
            .filter(role -> target.getRolesToPing().contains(role.getIdLong()))
            .toList();

        if (!roles.isEmpty()) {
            var suppressedRoles = roles.stream()
                .filter(role -> isSuppressed(role, target))
                .toList();
            var pingRoles = roles.stream()
                .filter(role -> !suppressedRoles.contains(role))
                .toList();
            pingRoles.forEach(role -> recentlyPingedRolesInChannelCache.put(createRecentlyPingedRoleKey(role, target.getTargetId()), DUMMY));

            var sb = new StringBuilder();

            if (!pingRoles.isEmpty()) {
                sb.append(pingRoles.stream()
                    .map(Role::getAsMention)
                    .collect(Collectors.joining(" ")));
            }

            if (!suppressedRoles.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("Suppressed role pings: ");
                sb.append(suppressedRoles.stream()
                    .map(role -> "`%s`".formatted(role.getName()))
                    .collect(Collectors.joining(", ")));
            }

            if (!sb.isEmpty()) {
                currentMessageBuilder.setContent(sb.toString());
            }
        }
    }

    /**
     * Creates embed for product and its changes
     *
     * @param product       Product
     * @param announcements announcement changes
     *
     * @return Embed builder
     */
    private EmbedBuilder createEmbed(
        GuildEntity guildEntity,
        ProductEntity product,
        List<ProductChangeAnnouncementEntity> announcements
    ) {
        var productListQuery = announcements.stream()
            .map(ProductChangeAnnouncementEntity::getChannelProductListQuery)
            .findAny()
            .map(ChannelProductListQueryEntity::getProductListQuery)
            .orElse(null);

        var builder = new EmbedBuilder();
        var descriptionSb = new StringBuilder();
        descriptionSb.append(productService.getBeanInstance().createProductInfoDescription(guildEntity, null, product, productListQuery));

        builder.setTitle(product.getName());
        builder.setUrl(amiAmiApiService.createAmiAmiProductDetailUrl(product.getCode()));
        builder.setImage(AmiTracker.AMI_AMI_IMAGE_URL.formatted(product.getImageUrl()));
        builder.setColor(product.getProductState().getColor());

        if (product.getImageUrl().equalsIgnoreCase(AmiAmiApiService.NO_IMAGE_URL)) {
            builder.setFooter("Once the image is available, you will be notified.");
        }

        if (!announcements.isEmpty()) {
            announcements.sort(Comparator.comparing(ProductChangeAnnouncementEntity::getCreatedAt).reversed());
            builder.setTimestamp(announcements.getFirst().getCreatedAt().toInstant());

            var fields = new ArrayList<Field>();
            for (ProductChangeAnnouncementEntity announcement : announcements) {
                if (fields.size() >= MessageEmbed.MAX_FIELD_AMOUNT) {
                    descriptionSb.append("\n\nThere are some old changes not shown.");
                    break;
                }

                var fieldName = "Changes @ %s".formatted(FORMATTER.format(announcement.getCreatedAt()));
                var fieldValue = createBulletPointChanges(announcement);

                var newField = fields.stream().noneMatch(field ->
                    field.getName().equals(fieldName) && field.getValue().equals(fieldValue));

                if (newField) {
                    fields.add(new Field(fieldName, fieldValue, false));
                }
            }

            if (fields.size() == 1) {
                descriptionSb.append("\n");
                descriptionSb.append(fields.getFirst().getValue());
            } else {
                fields.forEach(builder::addField);
            }
        } else {
            log.warn("No changes were passed into #createEmbed() for product code {}", product.getCode());
            descriptionSb.append("\n\nNo changes were specified.");
        }

        builder.setDescription(descriptionSb);
        return builder;
    }

    /**
     * Creates bullet point changes for the specified ProductChangeAnnouncementEntity
     *
     * @param announcementEntity entity
     *
     * @return Bullet points
     */
    private String createBulletPointChanges(ProductChangeAnnouncementEntity announcementEntity) {
        var sb = new StringBuilder();
        var changeHolder = announcementEntity.getProductChangeHolder();
        for (ProductChangeType productChangeType : announcementEntity.getProductChangeTypes()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            switch (productChangeType) {
                case PRICE_DISCOUNT -> {
                    sb.append("- Price discount (%s %s -> **%s %s**)".formatted(
                        changeHolder.getOldPriceJpy(), Currency.JPY.getSymbol(),
                        changeHolder.getNewPriceJpy(), Currency.JPY.getSymbol()
                    ));
                }
                case PRODUCT_STATE_CHANGED -> {
                    sb.append("- Product state (%s -> **%s**)".formatted(
                        changeHolder.getOldProductState(), changeHolder.getNewProductState()
                    ));
                }
                case PRODUCT_LIST_NEW_PRODUCT -> {
                    sb.append("- Product appeared in the search results");
                }
                case IMAGE_URL_CHANGE -> {
                    sb.append("- Image changed");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Handles successfully sending product announcements via the specific message
     *
     * @param message              Message
     * @param messageAnnouncements announcements sent in the message
     */
    private void handleSuccess(Message message, List<ProductChangeAnnouncementEntity> messageAnnouncements) {
        var announcementIds = messageAnnouncements.stream().map(ProductChangeAnnouncementEntity::getId).toList();
        try {
            log.info("Successfully sent product change announcements {} via message {}",
                announcementIds, message.getIdLong()
            );
            databaseService.runInThreadTransaction(session -> {
                messageAnnouncements.forEach(entity -> {
                    entity.setAnnouncementState(QueueState.COMPLETED);
                    entity.setSendLog("Sent via message %d".formatted(message.getIdLong()));
                    productChangeAnnouncementRepository.save(entity);
                });
            });
        } catch (Exception exception) {
            log.error("Failed to handle successful product change announcements {} on message {}",
                announcementIds, message.getIdLong(), exception
            );
        }
    }

    /**
     * Handles failed product announcement
     *
     * @param failure              Failure
     * @param messageAnnouncements Message announcements affected by the failure
     */
    private void handleFailure(Throwable failure, List<ProductChangeAnnouncementEntity> messageAnnouncements) {
        var announcementIds = messageAnnouncements.stream().map(ProductChangeAnnouncementEntity::getId).toList();
        try {
            log.error("Failed to send product change announcements {} due to error {}",
                announcementIds, failure.toString(), failure
            );
            databaseService.runInThreadTransaction(session -> {
                messageAnnouncements.forEach(entity -> {
                    entity.setAnnouncementState(QueueState.FAILED);
                    entity.setSendLog("Failed due to error\n%s".formatted(ExceptionUtils.dumpExceptionStacktrace(failure)));
                    productChangeAnnouncementRepository.save(entity);
                });
            });
        } catch (Exception exception) {
            log.error("Failed to handle failed product change announcements {}!", announcementIds, exception);
        }
    }

    /**
     * Creates a group identifier for the entity based on the related entities. The identifier is such that
     * grouped ProductChangeAnnouncementEntities will detonate one singular place to send the messages to and
     * ping correct roles.</br>
     * e.g., one channel that have 2 different product change announcements but ping the same roles will be grouped
     * together
     * be sent as one message.
     *
     * @param entity ProductChangeAnnouncementEntity
     *
     * @return Identifier
     */
    private MessageTarget createMessageTarget(ProductChangeAnnouncementEntity entity) {
        if (entity.getWishlist() != null) {
            return new MessageTarget(entity.getWishlist().getUser().getId());
        } else if (entity.getChannelProductListQuery() != null) {
            var productListQuery = entity.getChannelProductListQuery();
            var rolesToPing = new HashSet<>(productListQuery.getRoleIdsToNotify());

            // If only image URL change, don't ping any roles
            if (entity.getProductChangeTypes().contains(ProductChangeType.IMAGE_URL_CHANGE) && entity.getProductChangeTypes().size() == 1) {
                rolesToPing.clear();
            }

            return new MessageTarget(productListQuery.getChannel().getId(), productListQuery.getChannel().getGuild(), rolesToPing);
        } else {
            return null;
        }
    }

    /**
     * Checks whenever pinging the role in the specified message target should be suppressed
     *
     * @param role   Role
     * @param target Target
     *
     * @return If pinging should be suppressed
     */
    private boolean isSuppressed(Role role, MessageTarget target) {
        return recentlyPingedRolesInChannelCache.getIfPresent(createRecentlyPingedRoleKey(role, target.getTargetId())) != null;
    }

    /**
     * Creates a cache key for the recently pinged role
     *
     * @param role     Role
     * @param targetId Target id
     *
     * @return Cache key
     */
    private String createRecentlyPingedRoleKey(Role role, long targetId) {
        return "%s:%d".formatted(role.getId(), targetId);
    }
}
