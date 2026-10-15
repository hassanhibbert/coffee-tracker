package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.exception.UsernameTakenException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.security.TokenService;
import com.hassanhibbert.coffee_tracker.service.SampleDataService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SampleDataService sampleDataService;
    private final AuthRateLimiter rateLimiter;
    // Hash of a throwaway password, computed once. See login().
    private final String dummyHash;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, SampleDataService sampleDataService,
                       AuthRateLimiter rateLimiter) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sampleDataService = sampleDataService;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-password");
    }

    @Transactional
    public TokenResponse register(RegisterRequest request, String clientIp) {
        // Before any database work: a rejected attempt should cost the server as little as possible.
        rateLimiter.checkRegister(clientIp);
        if (appUserRepository.existsByUsername(request.username())) {
            throw new UsernameTakenException(request.username());
        }
        AppUser user = appUserRepository.save(
                new AppUser(request.username(), passwordEncoder.encode(request.password())));
        // Same transaction: the account and its sample data are committed together or not at all.
        sampleDataService.addTo(user.getId());
        return TokenResponse.bearer(tokenService.issue(user), tokenService.ttlSeconds());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request, String clientIp) {
        // Before BCrypt: hashing is deliberately slow, so unlimited attempts would also be a cheap way
        // to burn the server's CPU.
        rateLimiter.checkLogin(clientIp, request.username());
        Optional<AppUser> user = appUserRepository.findByUsername(request.username());
        // Always run one BCrypt comparison, even for unknown usernames. Otherwise "no such user"
        // answers in ~1ms and "wrong password" in ~100ms, and an attacker can enumerate usernames.
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            // Same message for both cases, for the same reason.
            throw new BadCredentialsException("Invalid username or password");
        }
        rateLimiter.loginSucceeded(request.username());
        return TokenResponse.bearer(tokenService.issue(user.get()), tokenService.ttlSeconds());
    }
}
