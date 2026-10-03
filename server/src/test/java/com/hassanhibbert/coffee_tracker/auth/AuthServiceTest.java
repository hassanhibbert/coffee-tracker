package com.hassanhibbert.coffee_tracker.auth;

import com.hassanhibbert.coffee_tracker.entity.AppUser;
import com.hassanhibbert.coffee_tracker.exception.UsernameTakenException;
import com.hassanhibbert.coffee_tracker.repository.AppUserRepository;
import com.hassanhibbert.coffee_tracker.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private AppUserRepository users;
    private PasswordEncoder encoder;
    private TokenService tokens;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        encoder = mock(PasswordEncoder.class);
        tokens = mock(TokenService.class);
        when(encoder.encode(anyString())).thenAnswer(call -> "hashed:" + call.getArgument(0));
        authService = new AuthService(users, encoder, tokens);
    }

    @Test
    void login_unknownUser_stillChecksAPasswordAndGivesTheGenericError() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
        // The dummy comparison is what keeps response time the same as for a wrong password.
        verify(encoder).matches(eq("whatever1"), eq("hashed:timing-equalizer-password"));
    }

    @Test
    void login_wrongPassword_givesTheSameGenericError() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(new AppUser("alice", "stored-hash")));
        when(encoder.matches("wrong-pass", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-pass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void register_storesOnlyTheHash() {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
        when(tokens.issue(any())).thenReturn("token");

        authService.register(new RegisterRequest("alice", "correct-horse"));

        verify(users).save(argThat(user ->
                user.getPasswordHash().equals("hashed:correct-horse")));
    }

    @Test
    void register_takenUsername_isRejected() {
        when(users.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "correct-horse")))
                .isInstanceOf(UsernameTakenException.class);
        verify(users, never()).save(any());
    }
}
