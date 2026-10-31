package com.codequest.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.regex.Pattern;

/** Small, well-tested helpers for cleaning AI input and output. */
public final class AiText {
    private AiText() {}

    private static final Pattern THINK = Pattern.compile("(?is)<think>.*?</think>|<thinking>.*?</thinking>");
    private static final Pattern UNCLOSED_THINK = Pattern.compile("(?is)^\\s*<think>.*");
    private static final Pattern FENCE = Pattern.compile("(?s)```(?:json)?\\s*(.*?)```");

    // Obvious secrets: API keys, tokens, private keys, passwords in key=value form.
    private static final Pattern[] SECRETS = {
            Pattern.compile("sk-[A-Za-z0-9_\\-]{16,}"),
            Pattern.compile("sk-or-v1-[A-Za-z0-9]{20,}"),
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            Pattern.compile("gh[pousr]_[A-Za-z0-9]{20,}"),
            Pattern.compile("xox[baprs]-[A-Za-z0-9-]{10,}"),
            Pattern.compile("AIza[0-9A-Za-z_\\-]{30,}"),
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
            Pattern.compile("(?i)(password|passwd|pwd|secret|api[_-]?key|token)\\s*[:=]\\s*\\S+"),
            Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-]{16,}"),
    };

    /** Remove reasoning blocks so raw "thinking" is never shown to the learner. */
    public static String stripReasoning(String s) {
        if (s == null) return "";
        String out = THINK.matcher(s).replaceAll("");
        if (UNCLOSED_THINK.matcher(out).matches()) return ""; // only reasoning, no answer
        return out.trim();
    }

    public static String scrubSecrets(String s) {
        if (s == null) return null;
        String out = s;
        for (Pattern p : SECRETS) out = p.matcher(out).replaceAll("[removed-secret]");
        return out;
    }

    /** Pull a JSON object/array out of a messy model reply (code fences, chatter before/after). */
    public static Optional<JsonNode> extractJson(String raw, ObjectMapper json) {
        if (raw == null) return Optional.empty();
        String s = raw.trim();
        var m = FENCE.matcher(s);
        if (m.find()) s = m.group(1).trim();
        int obj = s.indexOf('{');
        int arr = s.indexOf('[');
        int start = obj < 0 ? arr : arr < 0 ? obj : Math.min(obj, arr);
        if (start < 0) return Optional.empty();
        char close = s.charAt(start) == '{' ? '}' : ']';
        int end = s.lastIndexOf(close);
        if (end <= start) return Optional.empty();
        try {
            return Optional.of(json.readTree(s.substring(start, end + 1)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
