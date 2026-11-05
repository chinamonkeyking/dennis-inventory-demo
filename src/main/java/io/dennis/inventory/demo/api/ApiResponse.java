package io.dennis.inventory.demo.api;

public record ApiResponse<T>(String requestId, int code, String message, T data) {

    public static <T> ApiResponse<T> success(String requestId, T data) {
        return new ApiResponse<>(requestId, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.format(), data);
    }

    public static ApiResponse<Void> failure(String requestId, ErrorCode errorCode, Object... arguments) {
        return new ApiResponse<>(requestId, errorCode.getCode(), errorCode.format(arguments), null);
    }

    public static ApiResponse<Void> failure(String requestId, int code, String message) {
        return new ApiResponse<>(requestId, code, message, null);
    }
}
