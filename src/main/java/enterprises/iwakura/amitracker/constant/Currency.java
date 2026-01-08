package enterprises.iwakura.amitracker.constant;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

@Getter
@RequiredArgsConstructor
public enum Currency {
    JPY("Japanese Yen", "日本円", "¥"),
    USD("US Dollar", "US Dollar", "$"),
    CAD("Canadian Dollar", "Canadian Dollar", "C$"),
    CNY("Chinese Yuan", "人民币", "¥"),
    EUR("Euro", "Euro", "€"),
    GBP("British Pound", "British Pound", "£"),
    HKD("Hong Kong Dollar", "港元", "HK$"),
    KRW("South Korean Won", "대한민국 원", "₩");

    private final String englishName;
    private final String localName;
    private final String symbol;

    public static final List<Currency> ALL = List.of(Currency.values());
    public static final List<Choice> CHOICES = ALL.stream()
        .map(currency -> new Choice("%s (%s)".formatted(currency.getEnglishName(), currency.getLocalName()), currency.name()))
        .toList();
    public static final String CHOICES_STRING = ALL.stream()
        .map(Currency::name)
        .reduce((a, b) -> a + ", " + b)
        .orElse("");

    /**
     * Convert a string to a Currency enum, case-insensitive.
     *
     * @param str the string representation of the currency
     * @return an Optional containing the Currency if found, otherwise empty
     */
    public static Optional<Currency> fromString(String str) {
        try {
            return Optional.of(Currency.valueOf(str.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
