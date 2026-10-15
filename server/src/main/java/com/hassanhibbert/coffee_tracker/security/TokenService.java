package com.hassanhibbert.coffee_tracker.security;

import com.hassanhibbert.coffee_tracker.entity.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {
    public static final String DEMO_SCOPE = "demo";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * The token is signed, not encrypted: anyone holding it can base64-decode the claims.
     * So it carries only an id and a username, never anything secret.
     */
    public String issue(AppUser user) {
        return encode(claimsFor(user));
    }

    /**
     * A token for the shared demo account. "scope" is the standard OAuth2 claim for what a token
     * may do; Spring Security turns each scope into a SCOPE_* authority, and SecurityConfig refuses
     * writes from SCOPE_demo. The claim is signed, so nobody can remove it to unlock writes.
     */
    public String issueReadOnlyDemo(AppUser user) {
        return encode(claimsFor(user).claim("scope", DEMO_SCOPE));
    }

    private JwtClaimsSet.Builder claimsFor(AppUser user) {
        Instant now = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()));
    }

    private String encode(JwtClaimsSet.Builder claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long ttlSeconds() {
        return properties.ttl().toSeconds();
    }
}
