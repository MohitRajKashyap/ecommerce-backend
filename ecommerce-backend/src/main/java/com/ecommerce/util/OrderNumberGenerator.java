package com.ecommerce.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, human-readable order numbers.
 *
 * <p>Format: ORD-YYYYMMDD-{sequence}
 * Example:   ORD-20241215-000001</p>
 */
@Component
public class OrderNumberGenerator {

    private static final String PREFIX = "ORD";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong sequence = new AtomicLong(0);

    public String generate() {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        long seq = sequence.incrementAndGet() % 1_000_000;
        return String.format("%s-%s-%06d", PREFIX, date, seq);
    }
}
