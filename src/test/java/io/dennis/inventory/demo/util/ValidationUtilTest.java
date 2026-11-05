package io.dennis.inventory.demo.util;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilTest {

    @Test
    void returnsTrimmedStringForValidValue() {
        assertThat(ValidationUtil.requireString("  Cake  ", "name", 255))
                .isEqualTo("Cake");
    }

    @Test
    void acceptsStringAtMaximumLength() {
        String value = "a".repeat(255);

        assertThat(ValidationUtil.requireString(value, "name", 255))
                .isEqualTo(value);
    }

    @Test
    void rejectsNullString() {
        assertInvalidRequest(
                () -> ValidationUtil.requireString(null, "name", 255),
                "name is required");
    }

    @Test
    void rejectsBlankString() {
        assertInvalidRequest(
                () -> ValidationUtil.requireString("   ", "name", 255),
                "name is required");
    }

    @Test
    void rejectsStringLongerThanMaximum() {
        assertInvalidRequest(
                () -> ValidationUtil.requireString("a".repeat(256), "name", 255),
                "name must be at most 255 characters");
    }

    @Test
    void acceptsZeroQuantity() {
        assertThat(ValidationUtil.requireNonNegativeInteger(0, "quantity"))
                .isZero();
    }

    @Test
    void rejectsNullQuantity() {
        assertInvalidRequest(
                () -> ValidationUtil.requireNonNegativeInteger(null, "quantity"),
                "quantity is required");
    }

    @Test
    void rejectsNegativeQuantity() {
        assertInvalidRequest(
                () -> ValidationUtil.requireNonNegativeInteger(-1, "quantity"),
                "quantity must be greater than or equal to 0");
    }

    @Test
    void acceptsPositiveId() {
        assertThat(ValidationUtil.requirePositiveInteger(1, "categoryId"))
                .isEqualTo(1);
    }

    @Test
    void rejectsNullId() {
        assertInvalidRequest(
                () -> ValidationUtil.requirePositiveInteger(null, "categoryId"),
                "categoryId is required");
    }

    @Test
    void rejectsZeroId() {
        assertInvalidRequest(
                () -> ValidationUtil.requirePositiveInteger(0, "categoryId"),
                "categoryId must be positive");
    }

    @Test
    void rejectsNegativeId() {
        assertInvalidRequest(
                () -> ValidationUtil.requirePositiveInteger(-1, "categoryId"),
                "categoryId must be positive");
    }

    private void assertInvalidRequest(Runnable action, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("invalid request: " + expectedMessage);
                });
    }
}
