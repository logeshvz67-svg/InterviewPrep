package com.codequest.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * AI settings, all from environment variables. Model names are NEVER hardcoded:
 * set OPENROUTER_MODEL to the current Nemotron ":free" id from https://openrouter.ai/models.
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        boolean enabled,
        String apiKey,
        String model,
        String fallbackModels,
        int dailyLimitPerUser,
        int maxOutputTokens,
        int timeoutSeconds,
        int historyMessages,
        int globalPerMinute,
        String baseUrl,
        String appUrl
) {
    public List<String> modelChain() {
        List<String> chain = new java.util.ArrayList<>();
        if (model != null && !model.isBlank()) chain.add(model.trim());
        if (fallbackModels != null) {
            Arrays.stream(fallbackModels.split(",")).map(String::trim).filter(s -> !s.isEmpty() && !chain.contains(s)).forEach(chain::add);
        }
        return chain;
    }

    /** AI is usable only when switched on AND a key AND at least one model are configured. */
    public boolean usable() {
        return enabled && apiKey != null && !apiKey.isBlank() && !modelChain().isEmpty();
    }
}
