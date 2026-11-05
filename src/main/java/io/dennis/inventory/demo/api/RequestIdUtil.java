package io.dennis.inventory.demo.api;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIdUtil {

    public static final String REQUEST_ID_ATTRIBUTE = RequestIdUtil.class.getName() + ".requestId";

    private RequestIdUtil() {
    }

    public static String requireRequestId(ApiRequest<?> request, HttpServletRequest httpServletRequest) {
        String requestId = request == null ? null : request.requestId();
        httpServletRequest.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "requestId is required");
        }

        return requestId;
    }

    public static String getRequestId(HttpServletRequest httpServletRequest) {
        Object requestId = httpServletRequest.getAttribute(REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String value ? value : null;
    }
}
