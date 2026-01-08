package enterprises.iwakura.amitracker.console;

import enterprises.iwakura.amitracker.service.ProductImageService;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Command("fetch-image-url")
@Bean
@Slf4j
@RequiredArgsConstructor
public class FetchImageUrlConsoleCommand implements GanyuCommand {

    private final ProductImageService productImageService;

    @DefaultCommand
    public void fetch(String imageUrl) {
        productImageService.fetchImageUrl(imageUrl);
    }
}
