package com.hassanhibbert.coffee_tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proof that one user can never see or change another user's data, through any endpoint.
 * Real security, real signed JWTs, real Postgres: nothing mocked between the HTTP request
 * and the SQL "where owner_id = ?" that enforces it.
 *
 * Bob is fully authenticated in every request below. Authentication isn't authorization:
 * a valid token proves who Bob is, not that Alice's brew is his.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UserIsolationIntegrationTest {

    private static final String BEAN = """
            {"name":"Kenya AA","roastLevel":"LIGHT","countryOrigin":"Kenya"}""";
    private static final String EQUIPMENT = """
            {"name":"V60","method":"POUR_OVER","brand":"Hario"}""";
    private static final String BREW = """
            {"beanId": %d, "equipmentId": %d, "grindSize": "MEDIUM", "coffeeGrams": 15,
             "waterGrams": 250, "waterTempC": 94, "brewTimeSeconds": 180, "rating": 4}""";

    @Autowired MockMvc mvc;

    private String alice;
    private String bob;
    private long aliceBean;
    private long aliceEquipment;
    private long aliceBrew;

    @BeforeEach
    void aliceLogsABrew() throws Exception {
        // Unique names: the Spring context and its database are shared across test methods.
        alice = register("alice_" + shortId());
        bob = register("bob_" + shortId());
        aliceBean = createAndGetId(alice, "/api/v1/beans", BEAN);
        aliceEquipment = createAndGetId(alice, "/api/v1/equipment", EQUIPMENT);
        aliceBrew = createAndGetId(alice, "/api/v1/brews", BREW.formatted(aliceBean, aliceEquipment));
    }

    @Test
    void bobCannotReadAlicesData() throws Exception {
        as(bob, get("/api/v1/beans/" + aliceBean)).andExpect(status().isNotFound());
        as(bob, get("/api/v1/equipment/" + aliceEquipment)).andExpect(status().isNotFound());
        as(bob, get("/api/v1/brews/" + aliceBrew)).andExpect(status().isNotFound());
    }

    @Test
    void bobsListsAndStatsNeverIncludeAlicesData() throws Exception {
        as(bob, get("/api/v1/beans")).andExpect(jsonPath("$[?(@.id == " + aliceBean + ")]").isEmpty());
        as(bob, get("/api/v1/equipment")).andExpect(jsonPath("$[?(@.id == " + aliceEquipment + ")]").isEmpty());
        as(bob, get("/api/v1/brews?size=100")).andExpect(jsonPath("$.content[?(@.id == " + aliceBrew + ")]").isEmpty());
        // Filtering by Alice's bean id doesn't widen the query: the owner condition is always applied.
        as(bob, get("/api/v1/brews?beanId=" + aliceBean)).andExpect(jsonPath("$.totalElements").value(0));
        as(bob, get("/api/v1/brews?equipmentId=" + aliceEquipment)).andExpect(jsonPath("$.totalElements").value(0));
        // Stats aggregate only Bob's rows: Alice's bean never appears in his top beans.
        as(bob, get("/api/v1/stats")).andExpect(jsonPath("$.topBeans[?(@.beanId == " + aliceBean + ")]").isEmpty());
    }

    @Test
    void bobCannotChangeOrDeleteAlicesData() throws Exception {
        as(bob, put("/api/v1/beans/" + aliceBean).contentType(MediaType.APPLICATION_JSON)
                .content(BEAN.replace("Kenya AA", "hacked"))).andExpect(status().isNotFound());
        as(bob, put("/api/v1/equipment/" + aliceEquipment).contentType(MediaType.APPLICATION_JSON)
                .content(EQUIPMENT.replace("V60", "hacked"))).andExpect(status().isNotFound());
        as(bob, put("/api/v1/brews/" + aliceBrew).contentType(MediaType.APPLICATION_JSON)
                .content(BREW.formatted(aliceBean, aliceEquipment).replace("\"rating\": 4", "\"rating\": 1")))
                .andExpect(status().isNotFound());

        as(bob, delete("/api/v1/brews/" + aliceBrew)).andExpect(status().isNotFound());
        as(bob, delete("/api/v1/beans/" + aliceBean)).andExpect(status().isNotFound());
        as(bob, delete("/api/v1/equipment/" + aliceEquipment)).andExpect(status().isNotFound());

        // And from Alice's side, nothing moved.
        as(alice, get("/api/v1/beans/" + aliceBean)).andExpect(jsonPath("$.name").value("Kenya AA"));
        as(alice, get("/api/v1/equipment/" + aliceEquipment)).andExpect(jsonPath("$.name").value("V60"));
        as(alice, get("/api/v1/brews/" + aliceBrew)).andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void bobCannotLogABrewWithAlicesBeanOrEquipment() throws Exception {
        // Otherwise Bob's brew would point at Alice's rows, and his brew detail would leak
        // her bean's name through the join.
        long bobBean = createAndGetId(bob, "/api/v1/beans", BEAN);
        long bobEquipment = createAndGetId(bob, "/api/v1/equipment", EQUIPMENT);

        as(bob, post("/api/v1/brews").contentType(MediaType.APPLICATION_JSON)
                .content(BREW.formatted(aliceBean, bobEquipment))).andExpect(status().isNotFound());
        as(bob, post("/api/v1/brews").contentType(MediaType.APPLICATION_JSON)
                .content(BREW.formatted(bobBean, aliceEquipment))).andExpect(status().isNotFound());
    }

    @Test
    void everyEndpointRequiresAToken() throws Exception {
        mvc.perform(get("/api/v1/brews/" + aliceBrew)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/stats")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/beans/" + aliceBean)).andExpect(status().isUnauthorized());
    }

    private ResultActions as(String token, MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private String register(String username) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extract(json, "\"accessToken\":\"([^\"]+)\"");
    }

    private long createAndGetId(String token, String path, String body) throws Exception {
        String json = as(token, post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(extract(json, "\"id\":(\\d+)"));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (!m.find()) {
            throw new AssertionError("No match for " + regex + " in " + json);
        }
        return m.group(1);
    }
}
