package com.codequest.service;

import com.codequest.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * OPTIONAL code runner using Judge0 CE. Off unless JUDGE0_URL is set.
 * Honest limits: the public Judge0 CE on RapidAPI has a small free daily quota and needs an API key;
 * self-hosting Judge0 needs Docker on a machine with more RAM than Render's free tier.
 */
@Service
public class CodeRunnerService {
    static final int JAVA_LANGUAGE_ID = 62; // "Java (OpenJDK 13)" on Judge0 CE; check /languages on your instance

    private final String url;
    private final String apiKey;
    private final String apiHost;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CodeRunnerService(@Value("${code-runner.url:}") String url, @Value("${code-runner.api-key:}") String apiKey,
                             @Value("${code-runner.api-host:}") String apiHost, ObjectMapper json) {
        this.url = url == null ? "" : url.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.apiHost = apiHost;
        this.json = json;
    }

    public boolean enabled() { return !url.isBlank(); }

    public Map<String, Object> run(String code, String stdin) {
        if (!enabled()) throw ApiException.badRequest("The code runner is off. Run code in your IDE or an online compiler, then paste the output.");
        try {
            ObjectNode body = json.createObjectNode();
            body.put("source_code", code);
            body.put("language_id", JAVA_LANGUAGE_ID);
            body.put("stdin", stdin == null ? "" : stdin);
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + "/submissions?base64_encoded=false&wait=true"))
                    .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (apiKey != null && !apiKey.isBlank()) b.header("X-RapidAPI-Key", apiKey);
            if (apiHost != null && !apiHost.isBlank()) b.header("X-RapidAPI-Host", apiHost);
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 429) throw ApiException.badRequest("Code runner quota reached for today. Try again tomorrow or run locally.");
            if (res.statusCode() >= 400) throw ApiException.badRequest("Code runner error (" + res.statusCode() + ")");
            JsonNode r = json.readTree(res.body());
            return Map.of("status", r.path("status").path("description").asText(""),
                    "stdout", r.path("stdout").asText(""), "stderr", r.path("stderr").asText(""),
                    "compileOutput", r.path("compile_output").asText(""), "time", r.path("time").asText(""));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("Couldn't reach the code runner. Run the code locally instead.");
        }
    }
}
