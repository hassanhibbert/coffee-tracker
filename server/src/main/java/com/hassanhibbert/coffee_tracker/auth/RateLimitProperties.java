package com.hassanhibbert.coffee_tracker.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds app.rate-limit.* from application.properties. */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int loginPerIpPerMinute, int loginPerUsernamePer15Minutes,
                                  int registerPerIpPerHour, int demoPerIpPerHour) {
}
