package com.codequest.content;

import com.codequest.entity.*;
import com.codequest.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loads lessons and question banks from YAML files (src/main/resources/content) into the database
 * when the app starts. Runs AFTER Flyway created the tables.
 *
 * Lessons: inserted if new; updated only if the file's `version` is higher AND nobody edited the
 * lesson on the admin page. Banks: upserted by id/slug every start (they're read-only reference data).
 * Bad files are logged and skipped: content mistakes must never stop the app from starting.
 */
@Component
public class ContentSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ContentSeeder.class);
    public static final YAMLMapper YAML = new YAMLMapper();

    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;
    private final JavaTopicRepository topics;
    private final ProblemBankRepository problems;
    private final PatternRepository patterns;
    private final QuestionBankRepository questionBank;
    private final AptitudeQuestionRepository aptitude;
    private final SpeakingPromptRepository speaking;
    private final SpringTopicRepository springTopics;
    private final LessonContentValidator validator;
    private final TransactionTemplate tx;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ContentSeeder(LessonRepository lessons, QuizQuestionRepository questions, JavaTopicRepository topics,
                         ProblemBankRepository problems, PatternRepository patterns, QuestionBankRepository questionBank,
                         AptitudeQuestionRepository aptitude, SpeakingPromptRepository speaking, SpringTopicRepository springTopics,
                         LessonContentValidator validator, TransactionTemplate tx, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.lessons = lessons;
        this.questions = questions;
        this.topics = topics;
        this.problems = problems;
        this.patterns = patterns;
        this.questionBank = questionBank;
        this.aptitude = aptitude;
        this.speaking = speaking;
        this.springTopics = springTopics;
        this.validator = validator;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        long start = System.currentTimeMillis();
        int imported = 0;
        try {
            for (Resource r : resolver.getResources("classpath*:content/lessons/**/*.yaml")) {
                try (InputStream in = r.getInputStream()) {
                    JsonNode doc = YAML.readTree(in);
                    if (tx.execute(s -> importLesson(doc, false))) imported++;
                } catch (Exception e) {
                    log.warn("Skipping lesson file {}: {}", r.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Could not list lesson files: {}", e.getMessage());
        }
        seedBank("problems.yaml", this::seedProblems);
        seedBank("interview-*.yaml", this::seedQuestionBank);
        seedBank("aptitude.yaml", this::seedAptitude);
        seedBank("speaking.yaml", this::seedSpeaking);
        seedBank("spring.yaml", this::seedSpringTopics);
        log.info("Content seeding done: {} lessons imported/updated in {} ms", imported, System.currentTimeMillis() - start);
    }

    /**
     * Insert or update one lesson. Returns true if the DB changed.
     * force = true is used by the admin editor (always overwrite, mark as edited).
     */
    public boolean importLesson(JsonNode doc, boolean force) {
        List<String> errors = validator.validate(doc);
        if (!errors.isEmpty()) throw new IllegalArgumentException(doc.path("slug").asText("?") + ": " + String.join("; ", errors));

        String slug = doc.path("slug").asText();
        int version = doc.path("version").asInt(1);
        Lesson lesson = lessons.findBySlug(slug).orElse(null);
        if (lesson != null && !force && (lesson.isAdminEdited() || lesson.getContentVersion() >= version)) return false;
        if (lesson == null) {
            lesson = new Lesson();
            lesson.setSlug(slug);
        }
        lesson.setWorldCode(doc.path("world").asText());
        lesson.setSection(doc.path("section").asText());
        lesson.setSortOrder(doc.path("order").asInt());
        lesson.setTitle(doc.path("title").asText());
        lesson.setEstimatedMinutes(doc.path("estimatedMinutes").asInt(20));
        lesson.setPatternCode(doc.path("patternCode").isTextual() ? doc.path("patternCode").asText() : null);
        lesson.setTopicId("JAVA_CORE".equals(lesson.getWorldCode())
                ? topics.findAllByOrderBySectionAscSortOrderAsc().stream().filter(t -> t.getSlug().equals(slug)).map(BaseEntity::getId).findFirst().orElse(null)
                : null);
        ObjectNode content = ((ObjectNode) doc).deepCopy();
        content.remove("quiz"); // answers live only in quiz_questions
        lesson.setContent(content);
        lesson.setContentVersion(force && lesson.getId() != null ? lesson.getContentVersion() + 1 : version);
        lesson.setAdminEdited(force || lesson.isAdminEdited());
        Set<String> comps = new HashSet<>();
        doc.path("companies").forEach(c -> comps.add(c.asText()));
        lesson.setCompanyCodes(comps);
        lessons.save(lesson);

        questions.deleteByLessonId(lesson.getId());
        questions.flush();
        int i = 0;
        for (JsonNode q : doc.path("quiz")) {
            questions.save(toQuestion(lesson.getId(), q, i++));
        }
        return true;
    }

    static QuizQuestion toQuestion(Long lessonId, JsonNode q, int order) {
        JsonNodeFactory f = JsonNodeFactory.instance;
        QuizQuestion qq = new QuizQuestion();
        qq.setLessonId(lessonId);
        qq.setExtId(q.path("id").asText());
        qq.setSortOrder(order);
        String type = q.path("type").asText();
        qq.setType(type);
        qq.setPrompt(q.path("prompt").asText());
        qq.setCode(q.path("code").isTextual() ? q.path("code").asText() : null);
        qq.setExplanation(q.path("explanation").asText());
        ObjectNode answer = f.objectNode();
        switch (type) {
            case "FILL_THE_CODE" -> {
                ArrayNode acc = answer.putArray("accepted");
                q.path("acceptedAnswers").forEach(a -> acc.add(a.asText()));
            }
            case "MATCH" -> {
                ArrayNode pairs = answer.putArray("pairs");
                ArrayNode lefts = f.arrayNode();
                List<String> rights = new ArrayList<>();
                for (JsonNode p : q.path("pairs")) {
                    pairs.add(f.objectNode().put("left", p.path("left").asText()).put("right", p.path("right").asText()));
                    lefts.add(p.path("left").asText());
                    rights.add(p.path("right").asText());
                }
                // Options shown to the learner: lefts in order, rights shuffled (deterministically).
                Collections.shuffle(rights, new Random(q.path("id").asText().hashCode()));
                ObjectNode opts = f.objectNode();
                opts.set("left", lefts);
                ArrayNode r = opts.putArray("right");
                rights.forEach(r::add);
                qq.setOptions(opts);
            }
            default -> {
                answer.put("index", q.path("answer").asInt());
                ArrayNode opts = f.arrayNode();
                q.path("options").forEach(o -> opts.add(o.asText()));
                qq.setOptions(opts);
            }
        }
        qq.setAnswer(answer);
        return qq;
    }

    // ------------------------------------------------------------------ banks

    private interface BankSeeder { int seed(JsonNode doc); }

    private void seedBank(String pattern, BankSeeder seeder) {
        try {
            for (Resource r : resolver.getResources("classpath*:content/banks/" + pattern)) {
                try (InputStream in = r.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    String hash = com.codequest.security.JwtService.sha256(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                    List<String> known = jdbc.queryForList("select sha256 from content_imports where name = ?", String.class, r.getFilename());
                    if (!known.isEmpty() && known.get(0).equals(hash)) continue; // unchanged since last import
                    JsonNode doc = YAML.readTree(bytes);
                    Integer n = tx.execute(s -> seeder.seed(doc));
                    jdbc.update("insert into content_imports (name, sha256) values (?, ?) on conflict (name) do update set sha256 = excluded.sha256, updated_at = now()",
                            r.getFilename(), hash);
                    log.info("Bank {}: {} items", r.getFilename(), n);
                } catch (Exception e) {
                    log.warn("Skipping bank file {}: {}", r.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Could not list bank files {}: {}", pattern, e.getMessage());
        }
    }

    private int seedProblems(JsonNode doc) {
        Set<String> known = new HashSet<>();
        patterns.findAll().forEach(p -> known.add(p.getCode()));
        Map<String, ProblemBankItem> existing = new HashMap<>();
        problems.findAll().forEach(p -> existing.put(p.getSlug(), p));
        int i = 0;
        for (JsonNode p : doc.path("problems")) {
            String pattern = p.path("pattern").asText();
            if (!known.contains(pattern)) continue;
            ProblemBankItem item = existing.getOrDefault(p.path("slug").asText(), new ProblemBankItem());
            item.setSlug(p.path("slug").asText());
            item.setTitle(p.path("title").asText());
            item.setUrl(p.path("url").asText());
            item.setPlatform(p.path("platform").asText());
            item.setDifficulty(p.path("difficulty").asText());
            item.setPatternCode(pattern);
            item.setCompanies(strings(p.path("companies")));
            item.setWhy(p.path("why").asText());
            item.setSortOrder(i++);
            problems.save(item);
        }
        return i;
    }

    private int seedQuestionBank(JsonNode doc) {
        int n = 0;
        for (JsonNode q : doc.path("questions")) {
            QuestionBankItem item = questionBank.findByExtId(q.path("id").asText()).orElseGet(QuestionBankItem::new);
            item.setExtId(q.path("id").asText());
            item.setCategory(q.path("category").asText());
            item.setTopic(q.path("topic").asText());
            item.setDifficulty(q.path("difficulty").asText());
            item.setCompanies(strings(q.path("companies")));
            item.setQuestion(q.path("question").asText());
            item.setAnswer(q.path("answer").asText());
            item.setKeyPoints(strings(q.path("keyPoints")));
            questionBank.save(item);
            n++;
        }
        return n;
    }

    private int seedAptitude(JsonNode doc) {
        int n = 0;
        for (JsonNode q : doc.path("questions")) {
            AptitudeQuestion item = aptitude.findByExtId(q.path("id").asText()).orElseGet(AptitudeQuestion::new);
            item.setExtId(q.path("id").asText());
            item.setSection(q.path("section").asText());
            item.setTopic(q.path("topic").asText());
            item.setDifficulty(q.path("difficulty").asText());
            item.setCompanies(strings(q.path("companies")));
            item.setQuestion(q.path("question").asText());
            item.setOptions(strings(q.path("options")));
            item.setAnswer(q.path("answer").asInt());
            item.setExplanation(q.path("explanation").asText());
            aptitude.save(item);
            n++;
        }
        return n;
    }

    private int seedSpeaking(JsonNode doc) {
        int n = 0;
        for (JsonNode p : doc.path("prompts")) {
            SpeakingPrompt item = speaking.findByExtId(p.path("id").asText()).orElseGet(SpeakingPrompt::new);
            item.setExtId(p.path("id").asText());
            item.setCategory(p.path("category").asText());
            item.setPrompt(p.path("prompt").asText());
            item.setFramework(p.path("framework").asText("NONE"));
            item.setTimeLimitSeconds(p.path("timeLimitSeconds").asInt(90));
            item.setTips(strings(p.path("tips")));
            item.setSampleAnswer(p.path("sampleAnswer").asText());
            speaking.save(item);
            n++;
        }
        return n;
    }

    private int seedSpringTopics(JsonNode doc) {
        int n = 0;
        for (JsonNode t : doc.path("topics")) {
            SpringTopic item = springTopics.findByCode(t.path("code").asText()).orElseGet(SpringTopic::new);
            item.setCode(t.path("code").asText());
            item.setTitle(t.path("title").asText());
            item.setTopicGroup(t.path("group").asText("Core"));
            item.setSortOrder(t.path("order").asInt(n + 1));
            item.setContent(t);
            springTopics.save(item);
            n++;
        }
        return n;
    }

    static List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(x -> out.add(x.asText()));
        return out;
    }

    public static ObjectMapper yaml() { return YAML; }
}
