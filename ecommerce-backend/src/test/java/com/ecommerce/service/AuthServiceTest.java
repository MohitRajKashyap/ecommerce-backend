package com.ecommerce.service;

import com.ecommerce.dto.request.LoginRequest;
import com.ecommerce.dto.request.RegisterRequest;
import com.ecommerce.dto.response.AuthResponse;
import com.ecommerce.entity.RefreshToken;
import com.ecommerce.entity.User;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setPhone("9876543210");
        registerRequest.setRole(Role.CUSTOMER);

        savedUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("$2a$12$encodedPassword")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(false)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var mockUserDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtUtils.generateAccessToken(any())).thenReturn("mock.access.token");
        when(jwtUtils.getAccessTokenExpiration()).thenReturn(900000L);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .expiryDate(Instant.now().plusMillis(604800000))
                .user(savedUser)
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);

        verify(userRepository).save(any(User.class));
        verify(cartRepository).save(any());
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("john.doe@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("Password@123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));

        var mockUserDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtUtils.generateAccessToken(any())).thenReturn("access.token.value");
        when(jwtUtils.getAccessTokenExpiration()).thenReturn(900000L);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-value")
                .expiryDate(Instant.now().plusMillis(604800000))
                .user(savedUser)
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Should logout and revoke refresh token")
    void shouldLogoutAndRevokeToken() {
        // Given
        RefreshToken refreshToken = RefreshToken.builder()
                .token("some-refresh-token")
                .revoked(false)
                .user(savedUser)
                .build();
        when(refreshTokenRepository.findByToken("some-refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);

        // When
        authService.logout("some-refresh-token");

        // Then
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }
}
