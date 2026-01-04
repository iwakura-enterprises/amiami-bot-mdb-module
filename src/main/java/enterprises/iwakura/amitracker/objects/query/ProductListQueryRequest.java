package enterprises.iwakura.amitracker.objects.query;

import enterprises.iwakura.kirara.amiami.request.AmiAmiSearchRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ProductListQueryRequest implements QueryRequest {

    private final long productListQueryId;
    private final AmiAmiSearchRequest amiAmiSearchRequest;

}
