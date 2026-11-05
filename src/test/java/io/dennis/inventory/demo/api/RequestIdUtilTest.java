package io.dennis.inventory.demo.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdUtilTest {

    @Test
    void returnsRequestIdAndStoresItInRequestAttribute() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        ApiRequest<Void> request = new ApiRequest<>("req-1", null);

        String requestId = RequestIdUtil.requireRequestId(request, servletRequest);

        assertThat(requestId).isEqualTo("req-1");
        assertThat(servletRequest.getAttribute(RequestIdUtil.REQUEST_ID_ATTRIBUTE))
                .isEqualTo("req-1");
        assertThat(RequestIdUtil.getRequestId(servletRequest)).isEqualTo("req-1");
    }

    @Test
    void rejectsNullRequest() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertInvalidRequest(() -> RequestIdUtil.requireRequestId(null, servletRequest));
        assertThat(RequestIdUtil.getRequestId(servletRequest)).isNull();
    }

    @Test
    void rejectsMissingRequestId() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        ApiRequest<Void> request = new ApiRequest<>(null, null);

        assertInvalidRequest(() -> RequestIdUtil.requireRequestId(request, servletRequest));
        assertThat(RequestIdUtil.getRequestId(servletRequest)).isNull();
    }

    @Test
    void rejectsBlankRequestId() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        ApiRequest<Void> request = new ApiRequest<>("   ", null);

        assertInvalidRequest(() -> RequestIdUtil.requireRequestId(request, servletRequest));
        assertThat(RequestIdUtil.getRequestId(servletRequest)).isEqualTo("   ");
    }

    @Test
    void returnsNullWhenRequestAttributeIsMissing() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertThat(RequestIdUtil.getRequestId(servletRequest)).isNull();
    }

    @Test
    void returnsNullWhenRequestAttributeIsNotAString() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(RequestIdUtil.REQUEST_ID_ATTRIBUTE, 123);

        assertThat(RequestIdUtil.getRequestId(servletRequest)).isNull();
    }

    private void assertInvalidRequest(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(exception.getMessage())
                            .isEqualTo("invalid request: requestId is required");
                });
    }
}
