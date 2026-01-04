package enterprises.iwakura.amitracker.objects.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ProductListQueryRequest implements QueryRequest {

    private final long productListQueryId;
    private final int page;

}
