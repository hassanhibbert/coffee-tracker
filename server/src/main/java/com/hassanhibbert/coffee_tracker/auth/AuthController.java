package com.hassanhibbert.coffee_tracker.auth;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements // these endpoints are public: no lock icon in Swagger UI
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return authService.register(request, http.getRemoteAddr());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // getRemoteAddr() is the real client IP behind Render's proxy because of
        // server.forward-headers-strategy=native (see application.properties).
        return authService.login(request, http.getRemoteAddr());
    }
}
