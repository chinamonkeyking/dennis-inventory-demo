package io.dennis.inventory.demo.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions from all controllers into the API response envelope.
 * HTTP statuses come from the selected ErrorCode. Unexpected exception details are logged
 * for diagnosis but never exposed to API callers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Raised by application code for expected business failures, such as validation errors.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.getRequestId(httpServletRequest);
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(requestId, errorCode.getCode(), exception.getMessage()));
    }

    // Raised by Spring MVC/Jackson when the request body is missing, malformed,
    // or cannot be bound to the controller's request DTO.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(null, errorCode));
    }

    // Raised by Spring ORM when a versioned entity is updated or deleted
    // using an outdated @Version value.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.getRequestId(httpServletRequest);
        ErrorCode errorCode = ErrorCode.VERSION_CONFLICT;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(requestId, errorCode));
    }

    // Raised by the persistence layer when a database constraint fails,
    // such as a unique, foreign-key, not-null, or check constraint.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.getRequestId(httpServletRequest);
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        log.warn("Data integrity violation while processing requestId={}", requestId, exception);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(
                        requestId, errorCode, "data conflict"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    // Fallback for any exception not handled by a more specific handler above.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.getRequestId(httpServletRequest);
        ErrorCode errorCode = ErrorCode.SYSTEM_ERROR;
        // Keep the stack trace in server logs, but return only a safe generic message.
        log.error("Unexpected error while processing requestId={}", requestId, exception);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(requestId, errorCode));
    }
}
