package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.repository.BrewsRepository;
import com.hassanhibbert.coffee_tracker.security.TokenService;
import com.hassanhibbert.coffee_tracker.service.SampleDataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A shared, read-only account visitors can open with one click, without signing up.
 *
 * - No password to share: the demo endpoint hands out a token directly. The stored password
 *   is random and never kept anywhere, so nobody can sign in to it the normal way.
 * - Read-only: its token carries scope "demo", and SecurityConfig refuses writes from it.
 *   That's what makes sharing safe: nobody can delete the data or leave notes for the next visitor.
 * - Its username contains a space, which RegisterRequest doesn't allow, so nobody can
 *   register it first and have their account mistaken for the demo.
 */
@Service
public class DemoAccountService {
    public static final String USERNAME = "Demo Brewer";

    private final AppUserRepository appUserRepository;
    private final BrewsRepository brewsRepository;
    private final PasswordEncoder passwordEncoder;
    private final SampleDataService sampleDataService;
    private final TokenService tokenService;
    private final AuthRateLimiter rateLimiter;

    public DemoAccountService(AppUserRepository appUserRepository, BrewsRepository brewsRepository,
                              PasswordEncoder passwordEncoder, SampleDataService sampleDataService,
                              TokenService tokenService, AuthRateLimiter rateLimiter) {
        this.appUserRepository = appUserRepository;
        this.brewsRepository = brewsRepository;
        this.passwordEncoder = passwordEncoder;
        this.sampleDataService = sampleDataService;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Runs once the app has started. Idempotent: creates the account and its data only if
     * they're missing, so restarts and redeploys leave the existing demo alone.
     * (Called through the Spring proxy, so @Transactional applies.)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDemoAccount() {
        AppUser demo = appUserRepository.findByUsername(USERNAME).orElseGet(() ->
                appUserRepository.save(new AppUser(USERNAME, passwordEncoder.encode(UUID.randomUUID().toString()))));
        if (brewsRepository.countByOwnerId(demo.getId()) == 0) {
            sampleDataService.addToReadOnlyDemo(demo.getId());
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse demoToken(String clientIp) {
        rateLimiter.checkDemo(clientIp);
        AppUser demo = appUserRepository.findByUsername(USERNAME)
                .orElseThrow(() -> new IllegalStateException("Demo account missing; it is created at startup"));
        return TokenResponse.bearer(tokenService.issueReadOnlyDemo(demo), tokenService.ttlSeconds());
    }
}
