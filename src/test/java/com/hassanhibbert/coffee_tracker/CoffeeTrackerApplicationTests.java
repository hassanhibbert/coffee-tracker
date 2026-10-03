package com.hassanhibbert.coffee_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: real Spring context, real security with real signed JWTs, real Postgres.
 * Slow-ish, so there are only a few, covering what the slices can't: the pieces working together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CoffeeTrackerApplicationTests {

    @Autowired MockMvc mvc;

    @Test
    void registerLogBrewAndKeepUsersApart() throws Exception {
        String alice = register("alice_it");
        String bob = register("bob_it");

        long beanId = createAndGetId(alice, "/api/v1/beans",
                "{\"name\":\"Kenya AA\",\"roastLevel\":\"LIGHT\",\"countryOrigin\":\"Kenya\"}");
        long equipmentId = createAndGetId(alice, "/api/v1/equipment",
                "{\"name\":\"V60\",\"method\":\"POUR_OVER\",\"brand\":\"Hario\"}");
        createAndGetId(alice, "/api/v1/brews", """
                {"beanId": %d, "equipmentId": %d, "grindSize": "MEDIUM", "coffeeGrams": 15,
                 "waterGrams": 250, "waterTempC": 94, "brewTimeSeconds": 180, "rating": 4}
                """.formatted(beanId, equipmentId));

        mvc.perform(get("/api/v1/brews").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ratio").value("1:16.7"));

        // Bob is authenticated, but authentication isn't authorization: alice's bean is invisible to him.
        mvc.perform(get("/api/v1/beans/" + beanId).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/brews").header("Authorization", "Bearer " + bob))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        String token = register("carol_it");
        String forged = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/v1/beans").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    private String register(String username) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extract(json, "\"accessToken\":\"([^\"]+)\"");
    }

    private long createAndGetId(String token, String path, String body) throws Exception {
        String json = mvc.perform(post(path).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(extract(json, "\"id\":(\\d+)"));
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (!m.find()) {
            throw new AssertionError("No match for " + regex + " in " + json);
        }
        return m.group(1);
    }
}
