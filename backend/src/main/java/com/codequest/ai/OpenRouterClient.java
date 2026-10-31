package com.codequest.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * Calls OpenRouter's chat completions endpoint (OpenAI-compatible). ONLY the backend calls it:
 * the API key never reaches the browser.
 */
@Component
public class OpenRouterClient implements ChatClient {
    private final AiProperties props;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OpenRouterClient(AiProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public String complete(String model, List<Message> messages, int maxTokens, int timeoutSeconds) {
        try {
            ObjectNode body = json.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.4);
            // Ask OpenRouter not to send reasoning text back (we also strip it ourselves).
            body.putObject("reasoning").put("exclude", true);
            var arr = body.putArray("messages");
            for (Message m : messages) arr.addObject().put("role", m.role()).put("content", m.content());

            HttpRequest req = HttpRequest.newBuilder(URI.create(props.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + props.apiKey())
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", props.appUrl() == null ? "" : props.appUrl())
                    .header("X-Title", "Code Quest")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code == 429 || code >= 500) throw new AiCallException(code, true, "Provider busy (" + code + ")");
            if (code >= 400) throw new AiCallException(code, false, "Request rejected (" + code + ")");

            JsonNode root = json.readTree(res.body());
            // Some providers return HTTP 200 with an error object inside.
            if (root.has("error")) {
                int inner = root.path("error").path("code").asInt(0);
                throw new AiCallException(inner, inner == 429 || inner >= 500 || inner == 0, "Provider error: " + root.path("error").path("message").asText("unknown"));
            }
            JsonNode msg = root.path("choices").path(0).path("message");
            // We deliberately ignore msg.reasoning / reasoning_details: raw reasoning is never shown.
            return msg.path("content").asText("");
        } catch (AiCallException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new AiCallException(0, true, "Timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiCallException(0, false, "Interrupted");
        } catch (Exception e) {
            throw new AiCallException(0, true, "Network problem: " + e.getClass().getSimpleName());
        }
    }
}
