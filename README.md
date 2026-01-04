# Ami Tracker (MDB Module)

This is a MDB module for the AmiAmi Discord Bot.

## (Planned) Features
- Keep track of AmiAmi product releases and changes.
  - Notifications for new/upcoming releases.
  - Notifications for stock changes (back in stock, out of stock, etc.)
  - Price drop alerts for tracked items.
  - Wishlist management (notify when items on wishlist are back in stock or discounted).

## Algorithms

### Product Query Algorithm

1. ProductQueryEntity defines parameters for querying products from AmiAmi.
    - This entity is unique across entire database. If two different channels define the same query, they will share the same ProductQueryEntity.
2. Approx. every n minutes (configurable, default 1) a background job runs to query AmiAmi for each ProductQueryEntity.
    - This job runs every second and schedules queries to be ran based on last queried time + interval.
3. Based on ProductQueryEntity.maxPagination there will be maxPagination queries made to AmiAmi's API to fetch products.
4. For the entire result there will be constructed ProductQueryResult and for each of its entries ProductQueryResultEntry.
5. These entries link to ProductEntity. After fetching, they don't immediately update the ProductEntity.
6. Now, all the entries are compared to existing ProductEntity entries in the database.
    - Some of the entries could be new, in which case new ProductEntity is created, if missing.
    - Some of the entries could be existing, in which case existing ProductEntity is updated if needed.
    - Some of the entries could be removed, in which case we can't determine what products were removed in the long run.
      Thus, we can't delete them (the ProductQueryResultEntry) nor check if they have been removed. In this case, there
      will be:
      - a) Periodic cleanup job that will delete ProductQueryResultEntries that have not been present in the last N days.
      - b) A missing data. If the queries list contains more than ProductQueryEntity.maxPagination * pageSize products, we can't be sure
        if some products were removed. However, users will be instructed to limit their queries to reasonable sizes to avoid this.
      - NOTE: Another solution is to query the items themselves and check for differences. However, this could bubble up the
        number of requests significantly. We will be already making quite a bit of requests to query products for ProductQueryEntities
        and for wishlists (which are highly inefficient due to one request per item).
7. We will collect the changes (new products, updated products) and schedule notifications for channels that track the ProductQueryEntity.
    - This will also effect wishlist notifications, as they depend on ProductEntity changes.

After these steps, there should be a consistent state of ProductQueryEntities, ProductQueryResultEntries and ProductEntities in the database.
And channels tracking the ProductQueryEntities should be notified of changes without problems, if queries were reasonable in size (less than maxPagination * pageSize).

All queries are made with sort key "Last Updated" to ensure we get the most recently changed products first. AmiAmi hopefully
respects this and for any update to the Product (be it just price change or stock change) and sorts them accordingly.

### Wishlist Tracking Algorithm

1. User are able to create wishlists for themselves. Each wishlist can contain multiple WishlistEntryEntities that link to ProductEntity.
2. We should run a background job, every second as with ProductQueryEntity, to check for changes in wishlists.
3. We will collect all items that need to be queried. Instead of one minute interval, for wishlists there will be 5 or 10 minute interval.
4. There should never be a case where item should be updated more frequently than the minimum interval due to precise checking of their last update time.
5. Thanks to Product Query Algorithm, we will be able to actually notify wishlist changes more often, if they happen to be in ProductQueryEntity queries.
   This should improve the UX significantly.
6. For each wishlisted item, we will fetch its current data from AmiAmi. We will compare it to existing ProductEntity data and update if needed.
7. If there are changes, we will schedule notifications for users that have the item in their wishlist. This should include
   notifications for Product Query Algorithm as well.

### ProductEntity history tracking

If we update ProductEntity, we will check for changes in the price/status. If there were changes, we will log them in ProductHistoryEntity.
This way we can track price changes over time and maybe sometime in the future graph them or use them for analytics.

### Bought Items

Users may add items as "bought". This acts like a list of bought products with timestamps and prices. Users will be able
to add/remove items from their bought list, as well as set custom prices (in case they bought from a different store or
used a discount). This data will be global and not per-guild.

Later on, users could see each other's bought lists and compare prices or see recommendations based on what others bought.

### Image data

All products have images. We will store the image URLs in ProductEntity as well as the imageData (byte array). This image
will be updated on weekly basis to avoid stale images. This way, when we send notifications, we can include the image
without needing to fetch it every time.

Future releases may use S3 or other storage solutions to store images more efficiently. However, for now, storing
them in database is good enough.

### Notification System

As with sending e-mails, we will try and batch notifications to avoid spamming Discord API. There will be a
configurable delay (default 10 seconds) where notifications will be collected and sent in a single message. If the first
change would be made at time T, the message would be sent at T + delay with all changes that happened in between. This 
should prevent stalling the notification system if there are many changes happening at once for long time.

User's wishlists will be notified into DMs, while ProductQueryEntity changes will be notified into the channel
where the query was created.

For queries in channels that may tag different roles, the roles will be merged to make sure all interested parties
are notified. The message itself will distinguish what changes were made for what queries.

## Concurrency Model

Queries would be done in one or more background threads. This needs to be careful as if we will update the same product
in two threads, there could be issues regarding data consistency and lost and/or multiple updates. The safest way would
be making the API requests async while the processing of responses would be synchronized on per-product basis. That should
prevent most issues.

Notifications will be handled in another thread, that will collect notifications and send them after the delay. When it comes
to Discord rate limits, JDA should hopefully handle them internally. But due to notification grouping, we should be fine as well.