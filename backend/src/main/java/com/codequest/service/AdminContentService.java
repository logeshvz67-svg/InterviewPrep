package com.codequest.service;

import com.codequest.content.ContentSeeder;
import com.codequest.entity.Lesson;
import com.codequest.entity.QuizQuestion;
import com.codequest.exception.ApiException;
import com.codequest.repository.LessonRepository;
import com.codequest.repository.QuizQuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Admin lesson editor: edit a lesson as YAML, validated before saving. */
@Service
public class AdminContentService {
    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;
    private final ContentSeeder seeder;

    public AdminContentService(LessonRepository lessons, QuizQuestionRepository questions, ContentSeeder seeder) {
        this.lessons = lessons;
        this.questions = questions;
        this.seeder = seeder;
    }

    public record LessonRow(String slug, String world, String section, int order, String title, int version, boolean adminEdited) {}

    @Transactional(readOnly = true)
    public List<LessonRow> list() {
        return lessons.findAllByOrderByWorldCodeAscSectionAscSortOrderAsc().stream()
                .map(l -> new LessonRow(l.getSlug(), l.getWorldCode(), l.getSection(), l.getSortOrder(), l.getTitle(), l.getContentVersion(), l.isAdminEdited()))
                .toList();
    }

    /** Rebuild the full YAML (content + quiz with answers) for editing. */
    @Transactional(readOnly = true)
    public Map<String, String> getYaml(String slug) {
        Lesson l = lessons.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Lesson"));
        ObjectNode doc = ((ObjectNode) l.getContent()).deepCopy();
        ArrayNode quiz = doc.putArray("quiz");
        for (QuizQuestion q : questions.findByLessonIdOrderBySortOrder(l.getId())) {
            ObjectNode n = quiz.addObject();
            n.put("id", q.getExtId());
            n.put("type", q.getType());
            n.put("prompt", q.getPrompt());
            if (q.getCode() != null) n.put("code", q.getCode());
            switch (q.getType()) {
                case "FILL_THE_CODE" -> n.set("acceptedAnswers", q.getAnswer().path("accepted"));
                case "MATCH" -> n.set("pairs", q.getAnswer().path("pairs"));
                default -> {
                    n.set("options", q.getOptions());
                    n.put("answer", q.getAnswer().path("index").asInt());
                }
            }
            n.put("explanation", q.getExplanation());
        }
        try {
            return Map.of("slug", slug, "yaml", ContentSeeder.YAML.writeValueAsString(doc));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public Map<String, String> saveYaml(String slug, String yaml) {
        JsonNode doc;
        try {
            doc = ContentSeeder.YAML.readTree(yaml);
        } catch (Exception e) {
            throw ApiException.badRequest("YAML could not be read: " + e.getMessage());
        }
        if (!slug.equals(doc.path("slug").asText())) throw ApiException.badRequest("The slug inside the YAML must stay '" + slug + "'");
        try {
            seeder.importLesson(doc, true);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        return getYaml(slug);
    }
}
