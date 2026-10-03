package com.hassanhibbert.coffee_tracker.controller;

import com.hassanhibbert.coffee_tracker.dto.BrewFilter;
import com.hassanhibbert.coffee_tracker.dto.BrewRequest;
import com.hassanhibbert.coffee_tracker.dto.BrewResponse;
import com.hassanhibbert.coffee_tracker.dto.PageResponse;
import com.hassanhibbert.coffee_tracker.entity.BrewMethod;
import com.hassanhibbert.coffee_tracker.entity.GrindSize;
import com.hassanhibbert.coffee_tracker.exception.BrewRuleViolationException;
import com.hassanhibbert.coffee_tracker.exception.ResourceNotFoundException;
import com.hassanhibbert.coffee_tracker.security.SecurityConfig;
import com.hassanhibbert.coffee_tracker.service.BrewsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test: only the web layer (this controller, the exception handler, JSON, validation
 * and our real SecurityConfig) is started. The service is a Mockito mock, so these tests
 * check the HTTP contract: status codes, JSON shape, which user id reaches the service.
 */
@WebMvcTest(BrewsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class BrewsControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean BrewsService brewsService;

    /** Simulates an already-validated JWT whose subject (user id) is 42. No real token needed. */
    private static RequestPostProcessor user42() {
        return jwt().jwt(token -> token.subject("42"));
    }

    private static final String VALID_BODY = """
            {"beanId": 1, "equipmentId": 2, "grindSize": "FINE", "coffeeGrams": 18,
             "waterGrams": 36, "waterTempC": 93, "brewTimeSeconds": 28, "rating": 5}
            """;

    @Test
    void requestWithoutToken_is401() throws Exception {
        mvc.perform(get("/api/v1/brews")).andExpect(status().isUnauthorized());
        verifyNoInteractions(brewsService);
    }

    @Test
    void corsPreflightFromAllowedOrigin_isAccepted() throws Exception {
        mvc.perform(options("/api/v1/brews")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflightFromUnknownOrigin_isRejected() throws Exception {
        mvc.perform(options("/api/v1/brews")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_returns201WithLocation_andPassesTheTokensUserId() throws Exception {
        when(brewsService.create(any(), eq(42L))).thenReturn(sampleResponse(7L));

        mvc.perform(post("/api/v1/brews").with(user42())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/brews/7"))
                .andExpect(jsonPath("$.ratio").value("1:2.0"))
                .andExpect(jsonPath("$.equipment.method").value("ESPRESSO"));
    }

    @Test
    void create_withInvalidFields_is400WithEveryFieldError() throws Exception {
        String body = """
                {"beanId": 1, "equipmentId": 2, "grindSize": "FINE", "coffeeGrams": -5,
                 "waterGrams": 36, "brewTimeSeconds": 28, "rating": 9}
                """;

        mvc.perform(post("/api/v1/brews").with(user42())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.coffeeGrams").exists())
                .andExpect(jsonPath("$.errors.rating").exists());
        verifyNoInteractions(brewsService);
    }

    @Test
    void create_withUnknownEnumValue_is400() throws Exception {
        mvc.perform(post("/api/v1/brews").with(user42()).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"FINE\"", "\"POWDER\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brewRuleViolation_is422ProblemDetail() throws Exception {
        when(brewsService.create(any(BrewRequest.class), eq(42L)))
                .thenThrow(new BrewRuleViolationException("Grind size COARSE does not suit ESPRESSO"));

        mvc.perform(post("/api/v1/brews").with(user42())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Brew rule violated"))
                .andExpect(jsonPath("$.detail").value("Grind size COARSE does not suit ESPRESSO"));
    }

    @Test
    void missingBrew_is404ProblemDetail() throws Exception {
        when(brewsService.findById(99L, 42L)).thenThrow(new ResourceNotFoundException("Brew", 99L));

        mvc.perform(get("/api/v1/brews/99").with(user42()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Brew with id 99 was not found"));
    }

    @Test
    void search_bindsQueryParamsAndDefaultsToNewestFirst() throws Exception {
        when(brewsService.search(eq(42L), any(), any()))
                .thenReturn(new PageResponse<>(List.of(sampleResponse(1L)), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/brews").param("method", "ESPRESSO").param("minRating", "4").with(user42()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1));

        ArgumentCaptor<BrewFilter> filter = ArgumentCaptor.forClass(BrewFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(brewsService).search(eq(42L), filter.capture(), pageable.capture());
        assertThat(filter.getValue().method()).isEqualTo(BrewMethod.ESPRESSO);
        assertThat(filter.getValue().minRating()).isEqualTo(4);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("brewedAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    private static BrewResponse sampleResponse(Long id) {
        return new BrewResponse(id, new BrewResponse.BeanSummary(1L, "Kenya AA"),
                new BrewResponse.EquipmentSummary(2L, "Gaggia", BrewMethod.ESPRESSO), GrindSize.FINE,
                new BigDecimal("18.0"), new BigDecimal("36.0"), "1:2.0", 93, 28, 5, null,
                Instant.parse("2026-10-01T08:00:00Z"));
    }
}
