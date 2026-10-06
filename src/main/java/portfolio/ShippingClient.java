package portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class ShippingClient {
    public record Quote(BigDecimal amount, String currency, Integer deliveryDays) {}

    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper json = JsonMapper.builder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public ShippingClient(URI endpoint, String token, Duration timeout) {
        if (endpoint == null || !java.util.Set.of("http", "https").contains(endpoint.getScheme())
                || endpoint.getHost() == null || endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid API endpoint");
        }
        if (token == null || token.isBlank() || timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Token and positive timeout are required");
        }
        this.endpoint = endpoint;
        this.token = token;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public Quote quote(String postalCode) throws IOException, InterruptedException {
        if (postalCode == null || !postalCode.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("Postal code must contain eight digits");
        }
        var request = HttpRequest.newBuilder(endpoint.resolve("/quotes"))
                .timeout(timeout)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("postalCode", postalCode))))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Shipping API returned HTTP " + response.statusCode());
        }
        Quote quote = json.readValue(response.body(), Quote.class);
        if (quote == null || quote.amount() == null || quote.amount().signum() < 0
                || !"BRL".equals(quote.currency()) || quote.deliveryDays() == null || quote.deliveryDays() < 1) {
            throw new IOException("Invalid shipping quote contract");
        }
        return quote;
    }
}
