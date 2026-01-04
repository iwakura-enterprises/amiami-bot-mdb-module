package enterprises.iwakura.amitracker.objects.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ProductQueryRequest implements QueryRequest {

    private final String productCode;

}
