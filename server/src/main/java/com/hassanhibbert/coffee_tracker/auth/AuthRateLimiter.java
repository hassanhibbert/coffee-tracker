package com.hassanhibbert.coffee_tracker.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hassanhibbert.coffee_tracker.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Token buckets for the public auth endpoints: each key (an IP, or a username) holds N tokens,
 * every attempt takes one, and they refill gradually over the period. An empty bucket means 429.
 *
 * In memory, so limits are per instance: fine for one Render instance; running several would
 * need a shared store (e.g. Redis, which Bucket4j supports) or each instance would allow N.
 */
@Component
public class AuthRateLimiter {
    private final RateLimitProperties limits;
    private final TimeMeter timeMeter;
    // Bounded and expiring, so an attacker sending endless random usernames or spoofed
    // addresses can't grow this map until the server runs out of memory.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    @Autowired
    public AuthRateLimiter(RateLimitProperties limits) {
        this(limits, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** Lets tests pass a fake clock and check refills without sleeping. */
    AuthRateLimiter(RateLimitProperties limits, TimeMeter timeMeter) {
        this.limits = limits;
        this.timeMeter = timeMeter;
    }

    /**
     * Per IP stops one client guessing passwords across many accounts; per username stops
     * many IPs (a botnet) guessing one account's password.
     */
    public void checkLogin(String clientIp, String username) {
        consume("login-ip:" + clientIp, limits.loginPerIpPerMinute(), Duration.ofMinutes(1));
        consume(usernameKey(username), limits.loginPerUsernamePer15Minutes(), Duration.ofMinutes(15));
    }

    /** A successful login clears that username's failures, so the real owner isn't left locked out. */
    public void loginSucceeded(String username) {
        buckets.invalidate(usernameKey(username));
    }

    public void checkRegister(String clientIp) {
        consume("register-ip:" + clientIp, limits.registerPerIpPerHour(), Duration.ofHours(1));
    }

    public void checkDemo(String clientIp) {
        consume("demo-ip:" + clientIp, limits.demoPerIpPerHour(), Duration.ofHours(1));
    }

    private void consume(String key, int perPeriod, Duration period) {
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perPeriod).refillGreedy(perPeriod, period).build())
                .withCustomTimePrecision(timeMeter)
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    // Case-insensitive, so "Alice" and "alice " share one bucket.
    private static String usernameKey(String username) {
        return "login-user:" + username.trim().toLowerCase(Locale.ROOT);
    }
}
