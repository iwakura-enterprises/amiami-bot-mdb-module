package enterprises.iwakura.amitracker.service;

import java.util.List;

import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductProcessorService {

    public void process(List<AmiAmiSearchResponse> searchResponses) {
        log.info("Processing search responses: {}", searchResponses);
        // TODO:
        //  - Get previous results from DB
        //  - Compare with new results
        //    - New items -> if on first page, classify as new item
        //    - Status changes
        //    - If removed - Fetch details to confirm removal.
        //      - If no changes, somehow tell to not track again (e.g. outside of the search results?)
        //      - Update DB with new results, if any -> update
        //  - Always for all fetched items create ProductEntity and for existing product the price history entry.
    }

    public void process(AmiAmiItemResponse itemResponse) {
        log.info("Processing item response: {}", itemResponse);
    }
}
