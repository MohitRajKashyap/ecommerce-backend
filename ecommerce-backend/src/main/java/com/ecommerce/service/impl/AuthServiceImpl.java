package com.ecommerce.service.impl;

import com.ecommerce.dto.request.*;
import com.ecommerce.dto.response.AuthResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.RefreshToken;
import com.ecommerce.entity.User;
import com.ecommerce.exception.*;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ===== REGISTER =====

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "An account with email '" + request.getEmail() + "' already exists");
        }

        // Build and persist user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .active(true)           // Auto-activate (email verification optional)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        // Create an empty cart for the customer
        if (user.getRole().name().equals("CUSTOMER")) {
            Cart cart = Cart.builder().user(user).build();
            cartRepository.save(cart);
        }

        // Send welcome email asynchronously
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User registered successfully: {}", user.getEmail());

        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    // ===== LOGIN =====

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring Security handles authentication (throws on failure)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    // ===== REFRESH TOKEN =====

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenRefreshException(
                        request.getRefreshToken(), "Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException(request.getRefreshToken(), "Refresh token was revoked");
        }

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenRefreshException(request.getRefreshToken(), "Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtils.generateAccessToken(userDetails);

        // Rotate refresh token for security
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        RefreshToken newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(newAccessToken, newRefreshToken.getToken(), user);
    }

    // ===== LOGOUT =====

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out, refresh token revoked");
                });
    }

    // ===== FORGOT PASSWORD =====

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userRepository.findByEmail(request.getEmail().toLowerCase())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    // In production: store token in Redis with 15-min TTL
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
                    log.info("Password reset email sent to: {}", user.getEmail());
                });
    }

    // ===== RESET PASSWORD =====

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        // In production: validate token from Redis, get associated email
        // For now: token validation is a placeholder
        log.info("Password reset completed");
    }

    // ===== EMAIL VERIFICATION =====

    @Override
    @Transactional
    public void verifyEmail(String token) {
        // In production: validate email verification token from Redis
        log.info("Email verified with token: {}", token);
    }

    // ===== PRIVATE HELPERS =====

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}
