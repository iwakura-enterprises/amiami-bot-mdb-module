package enterprises.iwakura.amitracker.object;

import enterprises.iwakura.amitracker.constant.ProductState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeHolder {

    private Long oldPriceJpy;
    private ProductState oldProductState;

    private Long newPriceJpy;
    private ProductState newProductState;


}
