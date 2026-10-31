package com.codequest.ai;

import java.util.List;

/** Talks to an OpenAI-compatible chat API. An interface so tests can plug in a fake. */
public interface ChatClient {
    record Message(String role, String content) {}

    /** Throws AiCallException on any failure. Returns the raw assistant text (may contain think-blocks). */
    String complete(String model, List<Message> messages, int maxTokens, int timeoutSeconds);

    class AiCallException extends RuntimeException {
        private final int status;      // HTTP status, or 0 for network/timeout problems
        private final boolean retryable;

        public AiCallException(int status, boolean retryable, String message) {
            super(message);
            this.status = status;
            this.retryable = retryable;
        }

        public int status() { return status; }
        public boolean retryable() { return retryable; }
    }
}
