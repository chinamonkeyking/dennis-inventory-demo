package io.dennis.inventory.demo.util;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static String requireString(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() > maxLength) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " must be at most " + maxLength + " characters");
        }
        return trimmedValue;
    }

    public static int requireNonNegativeInteger(Integer value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        if (value < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " must be greater than or equal to 0");
        }
        return value;
    }

    public static int requirePositiveInteger(Integer value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        if (value <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " must be positive");
        }
        return value;
    }

    public static Integer optionalPositiveInteger(Integer value, String fieldName) {
        return value == null ? null : requirePositiveInteger(value, fieldName);
    }

    public static int requireIntegerInRange(
            Integer value, String fieldName, int minValue, int maxValue) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        if (value < minValue || value > maxValue) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " must be between " + minValue + " and " + maxValue);
        }
        return value;
    }
}
