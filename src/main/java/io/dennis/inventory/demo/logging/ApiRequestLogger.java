package io.dennis.inventory.demo.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Lightweight request timing/logging helper for this demo.
 *
 * <p>Emits start/end log lines with requestId, operation, elapsed time, and outcome.
 * Failures are logged as outcome=failure and rethrown, so the global exception handler
 * still controls the API response.
 *
 * <p>The elapsed time covers controller/service execution, not request parsing or response
 * serialization. For production observability, prefer a servlet filter or interceptor with
 * MDC-based requestId propagation and Micrometer/OpenTelemetry metrics.
 */
public final class ApiRequestLogger {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogger.class);

    private ApiRequestLogger() {
    }

    public static <T> T log(String operation, String requestId, Supplier<T> action) {
        long startNanos = System.nanoTime();
        log.info("event=request.start requestId={} operation={}", requestId, operation);

        try {
            T response = action.get();
            log.info(
                    "event=request.end requestId={} operation={} elapsedMs={} outcome=success",
                    requestId,
                    operation,
                    elapsedMillis(startNanos));
            return response;
        } catch (RuntimeException exception) {
            // Log the failure outcome, then rethrow so normal error handling is preserved.
            log.warn(
                    "event=request.end requestId={} operation={} elapsedMs={} outcome=failure errorType={}",
                    requestId,
                    operation,
                    elapsedMillis(startNanos),
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
