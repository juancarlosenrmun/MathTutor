package com.mathtutor.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GoogleAiStudioRateLimiterTest {
    @Test
    void refusesInvalidQuotaAndAllowsBothCallModes() {
        assertThatThrownBy(() -> new GoogleAiStudioRateLimiter(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GoogleAiStudioRateLimiter(-1)).isInstanceOf(IllegalArgumentException.class);
        var limiter = new GoogleAiStudioRateLimiter(60000);
        limiter.acquire();
        limiter.acquire();
        limiter.acquireAsync().block();
    }

    @Test
    void preservesInterruption() {
        var limiter = new GoogleAiStudioRateLimiter(1);
        limiter.acquire();
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(limiter::acquire).isInstanceOf(IllegalStateException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
