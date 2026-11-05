package io.dennis.inventory.demo.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiRequestLoggerTest {

    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ApiRequestLogger.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);

        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    void logsStartAndSuccessEndWithRequestIdAndElapsedTime() {
        String result = ApiRequestLogger.log("inventory.query", "req-1", () -> "ok");

        assertThat(result).isEqualTo("ok");

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(events.get(0).getFormattedMessage())
                .contains("event=request.start", "requestId=req-1", "operation=inventory.query");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(events.get(1).getFormattedMessage())
                .contains("event=request.end", "requestId=req-1", "operation=inventory.query",
                        "elapsedMs=", "outcome=success");
    }

    @Test
    void logsFailureEndAndRethrowsOriginalException() {
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> ApiRequestLogger.log("inventory.update", "req-2", () -> {
            throw failure;
        })).isSameAs(failure);

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFormattedMessage())
                .contains("event=request.start", "requestId=req-2", "operation=inventory.update");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.WARN);
        assertThat(events.get(1).getFormattedMessage())
                .contains("event=request.end", "requestId=req-2", "operation=inventory.update",
                        "elapsedMs=", "outcome=failure", "errorType=IllegalStateException");
    }
}
