package enterprises.iwakura.amitracker.service;

import java.time.Duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.kirara.amiami.response.AmiAmiCurrencyLayerResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiCurrencyLayerResponse.Quote;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class CurrencyExchangeService {

    private final Cache<String, AmiAmiCurrencyLayerResponse> currencyLayerCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofDays(30))
        .build();
    private final AmiAmiApiService amiAmiApiService;

    /**
     * Exchange an amount from one currency to another.
     *
     * @param amount the amount to exchange
     * @param from   the source currency
     * @param to     the target currency
     *
     * @return the exchanged amount
     */
    @SneakyThrows
    public Double exchange(Double amount, Currency from, Currency to) {
        if (amount == null) {
            return null;
        }
        if (from == to) {
            return amount;
        }

        var ratesResponse = currencyLayerCache.get("", () -> amiAmiApiService.getCurrencyLayer().send().join());
        var quotes = ratesResponse.getQuotes();
        double usd = 0.0;

        switch (from) {
            case JPY -> usd = amount / quotes.get(Quote.USDJPY);
            case USD -> usd = amount;
            case CAD -> usd = amount / quotes.get(Quote.USDCAD);
            case CNY -> usd = amount / quotes.get(Quote.USDCNY);
            case EUR -> usd = amount / quotes.get(Quote.USDEUR);
            case GBP -> usd = amount / quotes.get(Quote.USDGBP);
            case HKD -> usd = amount / quotes.get(Quote.USDHKD);
            case KRW -> usd = amount / quotes.get(Quote.USDKRW);
        }

        double result = 0.0;

        switch (to) {
            case JPY -> result = usd * quotes.get(Quote.USDJPY);
            case USD -> result = usd;
            case CAD -> result = usd * quotes.get(Quote.USDCAD);
            case CNY -> result = usd * quotes.get(Quote.USDCNY);
            case EUR -> result = usd * quotes.get(Quote.USDEUR);
            case GBP -> result = usd * quotes.get(Quote.USDGBP);
            case HKD -> result = usd * quotes.get(Quote.USDHKD);
            case KRW -> result = usd * quotes.get(Quote.USDKRW);
        }

        return result;
    }

}
