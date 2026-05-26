package com.ecommerce.config;

import com.ecommerce.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled maintenance tasks — runs in background to keep the database clean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Purge expired and revoked refresh tokens every day at 2 AM.
     * Prevents unbounded growth of the refresh_tokens table.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        log.info("Running scheduled refresh token cleanup...");
        refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now());
        log.info("Refresh token cleanup completed");
    }
}
