package com.codequest.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Small read-only reference content kept in memory (it never changes at runtime):
 * Spring annotation cards, debug challenges, project templates, speaking phrases/vocabulary/
 * pronunciation, world outlines. Loaded once when the bean is created.
 */
@Component
public class ContentCatalog {
    private static final Logger log = LoggerFactory.getLogger(ContentCatalog.class);
    private final JsonNode spring;
    private final JsonNode speaking;
    private final JsonNode outlines;

    public ContentCatalog() {
        this.spring = load("content/banks/spring.yaml");
        this.speaking = load("content/banks/speaking.yaml");
        this.outlines = load("content/banks/world-outlines.yaml");
    }

    private static JsonNode load(String path) {
        ClassPathResource r = new ClassPathResource(path);
        if (!r.exists()) return JsonNodeFactory.instance.objectNode();
        try (InputStream in = r.getInputStream()) {
            return ContentSeeder.YAML.readTree(in);
        } catch (Exception e) {
            log.warn("Could not read {}: {}", path, e.getMessage());
            return JsonNodeFactory.instance.objectNode();
        }
    }

    public JsonNode springAnnotations() { return spring.path("annotations"); }
    public JsonNode debugChallenges() { return spring.path("debugChallenges"); }
    public JsonNode projectTemplates() { return spring.path("projects"); }
    public JsonNode phrases() { return speaking.path("phrases"); }
    public JsonNode vocabulary() { return speaking.path("vocabulary"); }
    public JsonNode pronunciation() { return speaking.path("pronunciation"); }

    public JsonNode outlineFor(String world) {
        for (JsonNode w : outlines.path("worlds")) {
            if (world.equals(w.path("world").asText())) return w.path("lessons");
        }
        return JsonNodeFactory.instance.arrayNode();
    }
}
