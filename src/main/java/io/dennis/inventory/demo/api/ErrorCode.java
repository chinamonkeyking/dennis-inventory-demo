package io.dennis.inventory.demo.api;

import org.springframework.http.HttpStatus;

import java.util.Locale;

public enum ErrorCode {

    SUCCESS(0, HttpStatus.OK, "success"),
    INVALID_REQUEST(1000, HttpStatus.OK, "invalid request: %s"),
    INVENTORY_NOT_FOUND(1001, HttpStatus.OK, "inventory %s not found"), // when request itself can be parsed but invalid params
    DUPLICATE_INVENTORY_NAME(1002, HttpStatus.OK, "inventory name '%s' already exists"),
    INVALID_CATEGORY(1003, HttpStatus.OK, "invalid category: %s"),
    VERSION_CONFLICT(1004, HttpStatus.OK, "version conflict"),
    INVALID_REQUEST_BODY(1005, HttpStatus.BAD_REQUEST, "invalid request body"),  // when request itself cannot be parsed
    SYSTEM_ERROR(9999, HttpStatus.INTERNAL_SERVER_ERROR, "system error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(int code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String format(Object... arguments) {
        // Keep API messages stable regardless of the server's default locale.
        return String.format(Locale.ROOT, messageTemplate, arguments);
    }
}
