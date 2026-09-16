# ADR-0006: Resilience4j Circuit Breakers and Graceful Degradation

## Status
**Accepted** (2026-09-16)

## Context
External downstream integrations (e.g. third-party payment gateways like Stripe/Adyen, notification APIs like Twilio/SendGrid) frequently exhibit transient network anomalies, rate limiting, and outages.

Without fault isolation:
- Calls hang until client connection timeouts are reached.
- Tomcat worker threads become blocked, causing thread pool exhaustion.
- Cascading failures bring down the upstream microservices and API Gateway.

## Decision
We implemented **Resilience4j Circuit Breakers, Time Limiters, and Rate Limiters**.

1. **Circuit Breaker Configuration**:
   - Sliding window type: COUNT_BASED (sliding window size: 10 calls).
   - Failure rate threshold: 50% (if 5 of 10 calls fail, circuit trips to `OPEN`).
   - Slow call rate threshold: 50% (calls exceeding 2000ms duration count as slow).
   - Wait duration in open state: 10 seconds before transitioning to `HALF_OPEN`.
   - Permitted calls in half-open state: 3 trial calls.

2. **Time Limiter**:
   - Maximum call duration capped at 2.5 seconds to prevent unbounded thread blocking.

3. **Fallback Actions**:
   - Payment Service: When circuit is `OPEN`, immediately rejects charge with a `CircuitBreakerOpenException` and marks transaction as `FAILED_GATEWAY_UNAVAILABLE`, triggering the Saga compensation rollback without waiting for timeouts.

## Consequences

### Positive
- **Fast Failures**: Sub-millisecond failure response when downstream is known to be unhealthy, protecting CPU and memory resources.
- **Self-Healing**: Automatically tests recovery in `HALF_OPEN` state without operator intervention.
- **Metrics Visibility**: Micrometer Prometheus metrics export circuit breaker states (`resilience4j_circuitbreaker_state`).

### Negative / Trade-offs Accepted
- **Transient Rejection**: In `OPEN` state, even healthy requests during that 10s window are rejected immediately until the circuit enters `HALF_OPEN`.

## Verification
- Configured in `services/payment-service/src/main/resources/application.yml` and `services/order-service/src/main/resources/application.yml`.
- Validated via `scripts/test-failure-injection.ps1`.
