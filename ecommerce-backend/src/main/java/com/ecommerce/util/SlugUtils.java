package com.ecommerce.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generates URL-safe slugs from arbitrary strings.
 * Example: "Men's Clothing & Accessories" → "mens-clothing-accessories"
 */
@Component
public class SlugUtils {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    public String toSlug(String input) {
        if (input == null || input.isBlank()) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String ascii = NON_ASCII.matcher(normalized).replaceAll("");
        String lower = ascii.toLowerCase(Locale.ENGLISH).trim();
        String spacesReplaced = lower.replace(" ", "-").replace("_", "-");
        String clean = NON_ALPHANUMERIC.matcher(spacesReplaced).replaceAll("");
        return MULTIPLE_DASHES.matcher(clean).replaceAll("-");
    }
}
