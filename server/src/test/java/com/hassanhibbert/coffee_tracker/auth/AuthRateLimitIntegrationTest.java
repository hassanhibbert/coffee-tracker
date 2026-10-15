package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real Tomcat on a random port, not MockMvc: the step that turns
 * X-Forwarded-For into the client's address (Tomcat's RemoteIpValve) only exists in a real
 * server. These requests come from 127.0.0.1, a trusted internal address, which plays the
 * part of Render's proxy: it appends the real client's IP to whatever the client sent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.rate-limit.register-per-ip-per-hour=2",
        "app.rate-limit.login-per-ip-per-minute=100",
        "app.rate-limit.login-per-username-per15-minutes=3"
})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuthRateLimitIntegrationTest {

    @LocalServerPort int port;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void registerIsLimitedPerClient_with429AndRetryAfter() throws Exception {
        String client = "203.0.113.7";
        assertThat(register(client).statusCode()).isEqualTo(201);
        assertThat(register(client).statusCode()).isEqualTo(201);

        HttpResponse<String> limited = register(client);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Retry-After")).hasValueSatisfying(
                seconds -> assertThat(Long.parseLong(seconds)).isPositive());
        assertThat(limited.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(limited.body()).contains("\"title\":\"Too many requests\"");

        // Someone else isn't affected.
        assertThat(register("198.51.100.9").statusCode()).isEqualTo(201);
    }

    @Test
    void spoofingXForwardedForDoesNotGetAFreshLimit() throws Exception {
        String realClient = "192.0.2.44";
        // The attacker controls the start of the header; the proxy appends the real address.
        assertThat(register("6.6.6.1, " + realClient).statusCode()).isEqualTo(201);
        assertThat(register("6.6.6.2, " + realClient).statusCode()).isEqualTo(201);

        assertThat(register("6.6.6.3, " + realClient).statusCode()).isEqualTo(429);
    }

    @Test
    void loginGuessesAgainstOneAccountAreLimitedEvenFromManyIps() throws Exception {
        String username = "victim_" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(registerAs("192.0.2.200", username).statusCode()).isEqualTo(201);

        for (int i = 1; i <= 3; i++) {
            assertThat(login("198.18.0." + i, username, "wrong-guess-" + i).statusCode()).isEqualTo(401);
        }
        // A fourth address, and even the right password: the account is cooling down.
        assertThat(login("198.18.0.4", username, "correct-horse").statusCode()).isEqualTo(429);
    }

    private HttpResponse<String> register(String forwardedFor) throws Exception {
        return registerAs(forwardedFor, "user_" + UUID.randomUUID().toString().substring(0, 8));
    }

    private HttpResponse<String> registerAs(String forwardedFor, String username) throws Exception {
        return post("/api/v1/auth/register", forwardedFor,
                "{\"username\":\"" + username + "\",\"password\":\"correct-horse\"}");
    }

    private HttpResponse<String> login(String forwardedFor, String username, String password) throws Exception {
        return post("/api/v1/auth/login", forwardedFor,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private HttpResponse<String> post(String path, String forwardedFor, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
