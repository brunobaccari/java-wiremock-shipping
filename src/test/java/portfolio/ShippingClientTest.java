package portfolio;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.*;

class ShippingClientTest {
    @RegisterExtension
    static WireMockExtension api = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();
    static String token;
    static Duration timeout;
    ShippingClient client;

    @BeforeAll
    static void loadConfiguration() throws IOException {
        var config = new Properties();
        if (Files.exists(Path.of(".env"))) {
            try (var file = Files.newBufferedReader(Path.of(".env"))) { config.load(file); }
        }
        token = System.getenv().getOrDefault("SHIPPING_TOKEN", config.getProperty("SHIPPING_TOKEN"));
        timeout = Duration.ofMillis(Long.parseLong(System.getenv().getOrDefault(
                "SHIPPING_TIMEOUT_MS", config.getProperty("SHIPPING_TIMEOUT_MS"))));
    }

    @BeforeEach
    void createClient() {
        client = new ShippingClient(URI.create(api.baseUrl()), token, timeout);
    }

    @Test
    void parsesQuoteAndSendsExpectedRequest() throws Exception {
        api.stubFor(post("/quotes").withHeader("Authorization", equalTo("Bearer " + token))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("{\"postalCode\":\"13480000\"}"))
                .willReturn(okJson("{\"amount\":14.90,\"currency\":\"BRL\",\"deliveryDays\":3}")));
        var quote = client.quote("13480000");
        assertEquals(0, new BigDecimal("14.90").compareTo(quote.amount()));
        assertEquals("BRL", quote.currency());
        assertEquals(3, quote.deliveryDays());
        api.verify(1, postRequestedFor(urlEqualTo("/quotes")));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 404, 503})
    void exposesStatusWithoutRetryOrLeakingResponse(int status) {
        api.stubFor(post("/quotes").willReturn(aResponse().withStatus(status).withBody("internal details " + token)));
        var error = assertThrows(IOException.class, () -> client.quote("13480000"));
        assertEquals("Shipping API returned HTTP " + status, error.getMessage());
        api.verify(1, postRequestedFor(urlEqualTo("/quotes")));
    }

    @Test
    void timesOutWithoutRetry() {
        api.stubFor(post("/quotes").willReturn(okJson("{}").withFixedDelay(1500)));
        var fastClient = new ShippingClient(URI.create(api.baseUrl()), token, Duration.ofMillis(500));
        assertThrows(HttpTimeoutException.class, () -> fastClient.quote("13480000"));
        api.verify(1, postRequestedFor(urlEqualTo("/quotes")));
    }

    @Test
    void recoversOnlyWhenCallerRequestsAgain() throws Exception {
        api.stubFor(post("/quotes").inScenario("recovery").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("ready"));
        api.stubFor(post("/quotes").inScenario("recovery").whenScenarioStateIs("ready")
                .willReturn(okJson("{\"amount\":0,\"currency\":\"BRL\",\"deliveryDays\":1}")));
        assertThrows(IOException.class, () -> client.quote("13480000"));
        api.verify(1, postRequestedFor(urlEqualTo("/quotes")));
        assertEquals(0, BigDecimal.ZERO.compareTo(client.quote("13480000").amount()));
        api.verify(2, postRequestedFor(urlEqualTo("/quotes")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "not-json", "{\"amount\":-1,\"currency\":\"BRL\",\"deliveryDays\":3}",
            "{\"amount\":1,\"currency\":\"USD\",\"deliveryDays\":3}", "{\"amount\":1,\"currency\":\"BRL\",\"deliveryDays\":0}"})
    void rejectsMalformedOrInvalidQuote(String body) {
        api.stubFor(post("/quotes").willReturn(okJson(body)));
        assertThrows(IOException.class, () -> client.quote("13480000"));
        api.verify(1, postRequestedFor(urlEqualTo("/quotes")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1348000", "134800000", "1348A000", "13480-000"})
    void rejectsInvalidPostalCodeWithoutRequest(String postalCode) {
        assertThrows(IllegalArgumentException.class, () -> client.quote(postalCode));
        api.verify(0, anyRequestedFor(anyUrl()));
    }
}
