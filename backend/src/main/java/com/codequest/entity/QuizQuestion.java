package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One quiz question. `answer` stays on the server:
 *  - option types: {"index": 2}
 *  - FILL_THE_CODE: {"accepted": ["equals", "equals()"]}
 *  - MATCH: {"pairs": [{"left": "...", "right": "..."}]}
 */
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion extends BaseEntity {
    @Column(name = "lesson_id", nullable = false) private Long lessonId;
    @Column(name = "ext_id", nullable = false) private String extId;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String prompt;
    private String code;
    @JdbcTypeCode(SqlTypes.JSON) private JsonNode options;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode answer;
    @Column(nullable = false) private String explanation;

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long v) { lessonId = v; }
    public String getExtId() { return extId; }
    public void setExtId(String v) { extId = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { sortOrder = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String v) { prompt = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public JsonNode getOptions() { return options; }
    public void setOptions(JsonNode v) { options = v; }
    public JsonNode getAnswer() { return answer; }
    public void setAnswer(JsonNode v) { answer = v; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String v) { explanation = v; }
}
