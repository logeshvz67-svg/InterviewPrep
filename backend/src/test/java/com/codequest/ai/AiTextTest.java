package com.codequest.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiTextTest {
    final ObjectMapper m = new ObjectMapper();

    @Test
    void stripsThinkBlocks() {
        assertThat(AiText.stripReasoning("<think>long hidden reasoning</think>\nA HashMap stores key-value pairs.")).isEqualTo("A HashMap stores key-value pairs.");
        assertThat(AiText.stripReasoning("<think>never closed...")).isEmpty();
        assertThat(AiText.stripReasoning(null)).isEmpty();
    }

    @Test
    void scrubsObviousSecrets() {
        String s = AiText.scrubSecrets("my key sk-or-v1-abcdefghijklmnopqrstuvwxyz123456 and password=hunter2 and ghp_abcdefghijklmnopqrstuvwxyz1234");
        assertThat(s).doesNotContain("abcdefghijklmnop").doesNotContain("hunter2").contains("[removed-secret]");
        assertThat(AiText.scrubSecrets("what is a HashMap?")).isEqualTo("what is a HashMap?");
    }

    @Test
    void extractsJsonFromFencesAndChatter() {
        assertThat(AiText.extractJson("Sure! ```json\n{\"a\": 1}\n``` hope that helps", m)).get().matches(n -> n.path("a").asInt() == 1);
        assertThat(AiText.extractJson("Here: {\"q\": [1,2]} done", m)).get().matches(n -> n.path("q").size() == 2);
        assertThat(AiText.extractJson("[1,2,3]", m)).get().matches(n -> n.size() == 3);
        assertThat(AiText.extractJson("no json at all", m)).isEmpty();
        assertThat(AiText.extractJson("{broken: json", m)).isEmpty();
    }
}
