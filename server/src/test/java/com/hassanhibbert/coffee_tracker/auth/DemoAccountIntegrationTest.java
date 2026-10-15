package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DemoAccountIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DemoAccountService demoAccountService;

    private String demo;

    @BeforeEach
    void openTheDemo() throws Exception {
        String json = mvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        demo = extract(json, "\"accessToken\":\"([^\"]+)\"");
    }

    @Test
    void demoOpensWithDataToLookAt() throws Exception {
        mvc.perform(get("/api/v1/stats").header("Authorization", "Bearer " + demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBrews").value(8));
        mvc.perform(get("/api/v1/brews").header("Authorization", "Bearer " + demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bean.name").exists());
    }

    @Test
    void demoCannotChangeAnything() throws Exception {
        long brewId = firstId("/api/v1/brews", "\"content\":\\[\\{\"id\":(\\d+)");
        long beanId = firstId("/api/v1/beans", "\"id\":(\\d+)");

        mvc.perform(post("/api/v1/beans").header("Authorization", "Bearer " + demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spam\",\"roastLevel\":\"LIGHT\",\"countryOrigin\":\"Nowhere\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Read-only demo"));
        mvc.perform(put("/api/v1/beans/" + beanId).header("Authorization", "Bearer " + demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vandalised\",\"roastLevel\":\"LIGHT\",\"countryOrigin\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/brews/" + brewId).header("Authorization", "Bearer " + demo))
                .andExpect(status().isForbidden());

        // Nothing changed for the next visitor.
        mvc.perform(get("/api/v1/stats").header("Authorization", "Bearer " + demo))
                .andExpect(jsonPath("$.totalBrews").value(8));
    }

    @Test
    void nobodyCanRegisterTheDemoUsername() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + DemoAccountService.USERNAME + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void startupSeedingIsIdempotent() throws Exception {
        // As on every restart or redeploy: the existing demo is left as it is.
        demoAccountService.ensureDemoAccount();
        demoAccountService.ensureDemoAccount();

        mvc.perform(get("/api/v1/stats").header("Authorization", "Bearer " + demo))
                .andExpect(jsonPath("$.totalBrews").value(8));
    }

    private long firstId(String path, String regex) throws Exception {
        String json = mvc.perform(get(path).header("Authorization", "Bearer " + demo))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(extract(json, regex));
    }

    private static String extract(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (!m.find()) {
            throw new AssertionError("No match for " + regex + " in " + json);
        }
        return m.group(1);
    }
}
