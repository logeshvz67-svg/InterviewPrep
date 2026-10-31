package com.codequest.content;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Every shipped lesson must parse and pass the same validator the importer uses. */
class ContentFilesTest {
    @Test
    void allLessonFilesAreValidAndSlugsAreUnique() throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath*:content/lessons/**/*.yaml");
        assertThat(files.length).isGreaterThanOrEqualTo(153);
        LessonContentValidator v = new LessonContentValidator();
        Set<String> slugs = new HashSet<>();
        for (Resource r : files) {
            try (InputStream in = r.getInputStream()) {
                JsonNode doc = ContentSeeder.YAML.readTree(in);
                assertThat(v.validate(doc)).as(r.getFilename()).isEmpty();
                assertThat(slugs.add(doc.path("slug").asText())).as("duplicate slug " + r.getFilename()).isTrue();
                assertThat(r.getFilename()).isEqualTo(doc.path("slug").asText() + ".yaml");
            }
        }
    }

    @Test
    void matchQuestionsKeepAnswersServerSide() throws Exception {
        JsonNode q = ContentSeeder.YAML.readTree("""
                id: q4
                type: MATCH
                prompt: Match
                pairs:
                  - {left: a, right: "1"}
                  - {left: b, right: "2"}
                  - {left: c, right: "3"}
                explanation: x
                """);
        var qq = ContentSeeder.toQuestion(1L, q, 0);
        assertThat(qq.getOptions().path("left").size()).isEqualTo(3);
        assertThat(qq.getOptions().path("right").size()).isEqualTo(3);
        assertThat(qq.getOptions().toString()).doesNotContain("pairs");
        assertThat(qq.getAnswer().path("pairs").size()).isEqualTo(3);
    }
}
