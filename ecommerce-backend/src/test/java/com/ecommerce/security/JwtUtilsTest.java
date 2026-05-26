package com.ecommerce.security;

import com.ecommerce.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private UserDetails userDetails;

    // 256-bit base64 encoded test secret
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpiration", 900000L);

        userDetails = new User(
                "test@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        String token = jwtUtils.generateAccessToken(userDetails);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        String token = jwtUtils.generateAccessToken(userDetails);
        String extracted = jwtUtils.extractUsername(token);
        assertThat(extracted).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void shouldValidateTokenForCorrectUser() {
        String token = jwtUtils.generateAccessToken(userDetails);
        boolean isValid = jwtUtils.isTokenValid(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        String token = jwtUtils.generateAccessToken(userDetails);

        UserDetails otherUser = new User(
                "other@example.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        boolean isValid = jwtUtils.isTokenValid(token, otherUser);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpiration", -1000L);
        String expiredToken = jwtUtils.generateAccessToken(userDetails);
        assertThat(jwtUtils.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("Should return correct expiration time")
    void shouldReturnCorrectExpirationTime() {
        assertThat(jwtUtils.getAccessTokenExpiration()).isEqualTo(900000L);
    }
}
