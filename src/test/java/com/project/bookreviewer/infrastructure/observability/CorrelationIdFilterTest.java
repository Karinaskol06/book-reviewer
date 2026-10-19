package com.project.bookreviewer.infrastructure.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    // Tests that a correlation id is generated when no header is present
    void generatesCorrelationId_whenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNotBlank();

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    // Tests that an incoming correlation id is reused when present
    void reusesIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "demo-correlation-123");
        // Creates a response object
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Creates a filter chain object
        FilterChain chain = (req, res) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("demo-correlation-123");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("demo-correlation-123");
    }

    @Test
    // Tests that long correlation ids are truncated to 128 characters
    void resolveCorrelationId_truncatesOverlongValues() {
        String longId = "x".repeat(200);
        assertThat(CorrelationIdFilter.resolveCorrelationId(longId)).hasSize(128);
    }
}
