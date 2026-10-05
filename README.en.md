# Java and WireMock — shipping quotes

[Versão em português](README.md)

A Java 21 HTTP client with JUnit 5 tests against a WireMock service. Tests check outgoing requests, quote parsing and behavior when the provider returns invalid data or becomes unavailable.

## Run

Java 21 and Maven 3.9 or later:

```bash
cp .env.example .env
mvn -B -ntp verify
```

On PowerShell, use `Copy-Item .env.example .env`. Configuration uses plain `KEY=value`, without quotes or shell expansion, read through `java.util.Properties`. Process variables take precedence. The example token is fictitious. WireMock supplies a dynamic URL and port; the client contains no fixed service address.

## Scenarios

- POST path, headers and JSON body match the expected request; monetary values use BigDecimal.
- HTTP 401, 404 and 503 fail explicitly, without exposing the received error body or retrying.
- A slow response triggers the client's timeout with one recorded request.
- A stateful scenario fails first and recovers only when the caller explicitly requests again.
- Invalid JSON, missing fields, negative amounts, unexpected currency and invalid delivery days are rejected.
- Invalid postal codes are rejected before any HTTP request.

WireMock resets stubs and request history between tests. These tests do not call a real carrier. The local service is intentional **service virtualization**, not a production integration. A contract authored in this repository does not establish compatibility with an external provider.

## Structure and evidence

`src/main/java/portfolio/ShippingClient.java` contains the client. `src/test/java/portfolio/ShippingClientTest.java` defines stubs, controlled failures and request verification. CI runs `mvn verify` and uploads JUnit reports. The recorded CI run passed all 16 cases.

Open a run under **Actions**: **Summary** shows counts and step status; **Artifacts** provides `junit-results` with JUnit XML, Surefire text reports and a copy of the summary. Files are retained for seven days, including failed test runs. A missing report is identified as unconfirmed execution and fails the summary step.

[Test strategy and reporting examples](docs/test-strategy.md) · [Actions runs and artifacts](https://github.com/brunobaccari/java-wiremock-shipping/actions).

References: [WireMock JUnit Jupiter](https://wiremock.org/docs/junit-jupiter/), [fault simulation](https://wiremock.org/docs/simulating-faults/) and [stateful scenarios](https://wiremock.org/docs/stateful-behaviour/).

Commit dates in this portfolio were reorganized retroactively; Actions runs retain their actual execution dates.
