package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.exception.TooManyRequestsException;
import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test with a fake clock: refills are checked by moving time, not by sleeping. */
class AuthRateLimiterTest {
    private FakeClock clock;
    private AuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        limiter = new AuthRateLimiter(new RateLimitProperties(3, 2, 2, 2), clock);
    }

    @Test
    void register_allowsTheLimitThenRejects_andTellsHowLongToWait() {
        limiter.checkRegister("203.0.113.7");
        limiter.checkRegister("203.0.113.7");

        assertThatThrownBy(() -> limiter.checkRegister("203.0.113.7"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, ex ->
                        // 2 per hour refills one token every 30 minutes.
                        assertThat(ex.getRetryAfterSeconds()).isEqualTo(30 * 60));
    }

    @Test
    void register_tokensRefillOverTime() {
        limiter.checkRegister("203.0.113.7");
        limiter.checkRegister("203.0.113.7");

        clock.advance(Duration.ofMinutes(30));

        assertThatCode(() -> limiter.checkRegister("203.0.113.7")).doesNotThrowAnyException();
    }

    @Test
    void eachIpHasItsOwnBucket() {
        limiter.checkRegister("203.0.113.7");
        limiter.checkRegister("203.0.113.7");

        assertThatCode(() -> limiter.checkRegister("198.51.100.9")).doesNotThrowAnyException();
    }

    @Test
    void login_perUsernameLimitHoldsAcrossManyIps() {
        // A botnet: every attempt from a different address, all against one account.
        limiter.checkLogin("10.0.0.1", "alice");
        limiter.checkLogin("10.0.0.2", "Alice ");

        assertThatThrownBy(() -> limiter.checkLogin("10.0.0.3", "ALICE"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void login_successClearsThatUsernamesFailures() {
        limiter.checkLogin("10.0.0.1", "alice");
        limiter.checkLogin("10.0.0.1", "alice");

        limiter.loginSucceeded("alice");

        assertThatCode(() -> limiter.checkLogin("10.0.0.1", "alice")).doesNotThrowAnyException();
    }

    @Test
    void login_perIpLimitHoldsAcrossManyUsernames() {
        // One client trying one password against many accounts ("password spraying").
        limiter.checkLogin("203.0.113.7", "alice");
        limiter.checkLogin("203.0.113.7", "bob");
        limiter.checkLogin("203.0.113.7", "carol");

        assertThatThrownBy(() -> limiter.checkLogin("203.0.113.7", "dave"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    private static final class FakeClock implements TimeMeter {
        private long nanos;

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }
}
