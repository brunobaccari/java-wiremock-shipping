# Shipping API test strategy

The contract is a portfolio example: POST /quotes receives an eight-digit postal code and returns non-negative amount, BRL currency and a positive deliveryDays value. The client must fail explicitly when the response cannot be trusted.

| Risk | Technique | Evidence |
| --- | --- | --- |
| Incorrect request accepted by a broad mock | Exact request and header matching | Stub matching plus request count. |
| Free shipping rejected or negative amount accepted | Boundary values: zero and negative | Recovery case and invalid contract cases. |
| Error hidden by retries | HTTP status partitioning | One request for 401/404/503. |
| Slow provider blocks caller | Controlled latency | HTTP timeout with one recorded request. |
| Transient failure prevents later recovery | Stateful scenario | First call fails; explicit second call succeeds. |
| Invalid postcode reaches the network | Length and character partitions | Zero requests for invalid input. |

WireMock validates consumer behavior under controlled conditions. It does not establish provider compatibility, service availability, mobile UI behavior or performance under load. Before applying this client to a real provider, compare its published schema and auth requirements, then run a small contract suite against an authorized hosted environment.

## Reporting in English

Technical: “The request journal shows one POST for the timeout scenario. The client raises HttpTimeoutException and does not retry. See the JUnit artifact and workflow revision for the executed result.”

Non-technical: “The client reports unavailable shipping quotes instead of displaying an invented price. These checks use a simulated provider; a real integration remains outside this result.”

Defects should include input, expected contract, observed response, client exception and sanitized evidence. Triage → fix → reproduce original case → related regression → close or reopen. Do not paste authorization headers into Jira.

## Contract boundary

Numeric strings and fractional delivery days must not be silently converted. Concatenated JSON and overflowing integers are invalid; parser errors must not expose provider values or nested causes. The parameterized contract cases and sanitization test block acceptance when these guarantees regress. The exercise does not validate a real carrier contract.
