package com.codequest.service;

import com.codequest.dto.GymDtos.PatternStat;
import com.codequest.dto.PageResponse;
import com.codequest.entity.Pattern;
import com.codequest.entity.ProblemBankItem;
import com.codequest.entity.ProblemSolved;
import com.codequest.repository.PatternRepository;
import com.codequest.repository.ProblemBankRepository;
import com.codequest.repository.ProblemSolvedRepository;
import com.codequest.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Curated practice problems + "what should I solve next?".
 * Suggestions follow your Pattern Map: shaky patterns first, then the next new core pattern,
 * with difficulty matched to how strong you are in that pattern. Already-solved problems are skipped.
 */
@Service
public class ProblemBankService {
    private final ProblemBankRepository bank;
    private final ProblemSolvedRepository solved;
    private final PatternRepository patterns;
    private final ProfileRepository profiles;
    private final GymService gym;
    private final TimeService time;

    public ProblemBankService(ProblemBankRepository bank, ProblemSolvedRepository solved, PatternRepository patterns,
                              ProfileRepository profiles, GymService gym, TimeService time) {
        this.bank = bank;
        this.solved = solved;
        this.patterns = patterns;
        this.profiles = profiles;
        this.gym = gym;
        this.time = time;
    }

    public record BankProblem(Long id, String slug, String title, String url, String platform, String difficulty,
                              String patternCode, String patternName, List<String> companies, String why, boolean solved) {}

    public record Suggestion(BankProblem problem, String reason) {}

    public record PatternCoverage(String code, String name, int solved, int total) {}

    @Transactional(readOnly = true)
    public PageResponse<BankProblem> browse(Long userId, String pattern, String difficulty, String company, String status, int page, int size) {
        Set<Long> done = solvedIds(userId);
        Map<String, String> names = patternNames();
        List<BankProblem> all = bank.findAllByOrderBySortOrder().stream()
                .filter(p -> pattern == null || pattern.isBlank() || p.getPatternCode().equals(pattern))
                .filter(p -> difficulty == null || difficulty.isBlank() || p.getDifficulty().equals(difficulty))
                .filter(p -> p.relevantTo(company))
                .map(p -> view(p, done, names))
                .filter(v -> status == null || status.isBlank() || ("SOLVED".equals(status) == v.solved()))
                .toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResponse<>(all.subList(from, to), page, size, all.size(), (all.size() + size - 1) / size);
    }

    @Transactional(readOnly = true)
    public List<PatternCoverage> coverage(Long userId) {
        Set<Long> done = solvedIds(userId);
        Map<String, List<ProblemBankItem>> byPattern = bank.findAllByOrderBySortOrder().stream().collect(Collectors.groupingBy(ProblemBankItem::getPatternCode));
        return patterns.findAllByOrderBySortOrder().stream().map(p -> {
            List<ProblemBankItem> items = byPattern.getOrDefault(p.getCode(), List.of());
            return new PatternCoverage(p.getCode(), p.getName(), (int) items.stream().filter(i -> done.contains(i.getId())).count(), items.size());
        }).toList();
    }

    /** Up to 3 suggestions for today. Stable all day so the list doesn't keep changing. */
    @Transactional(readOnly = true)
    public List<Suggestion> suggestions(Long userId) {
        String company = profiles.findByUserId(userId).map(p -> p.getPrimaryCompany()).orElse("INFOSYS_SP");
        Set<Long> done = solvedIds(userId);
        Map<String, String> names = patternNames();
        List<PatternStat> stats = gym.patternMap(userId).patterns();
        Random rnd = new Random(userId * 7919L + time.today(userId).toEpochDay());

        // Order patterns: weak ones you started, then core patterns not started, then OK ones, then the rest.
        List<PatternStat> ordered = new ArrayList<>();
        stats.stream().filter(s -> "WEAK".equals(s.strength())).forEach(ordered::add);
        stats.stream().filter(s -> "NOT_STARTED".equals(s.strength()) && s.spCore()).forEach(ordered::add);
        stats.stream().filter(s -> "OK".equals(s.strength())).forEach(ordered::add);
        stats.stream().filter(s -> !ordered.contains(s)).forEach(ordered::add);

        Map<String, List<ProblemBankItem>> byPattern = bank.findAllByOrderBySortOrder().stream()
                .filter(p -> !done.contains(p.getId()))
                .collect(Collectors.groupingBy(ProblemBankItem::getPatternCode));

        List<Suggestion> out = new ArrayList<>();
        for (PatternStat s : ordered) {
            if (out.size() >= 3) break;
            List<ProblemBankItem> candidates = byPattern.getOrDefault(s.code(), List.of());
            if (candidates.isEmpty()) continue;
            List<String> wanted = switch (s.strength()) {
                case "STRONG" -> List.of("MEDIUM", "HARD", "EASY");
                case "OK" -> List.of("MEDIUM", "EASY", "HARD");
                default -> List.of("EASY", "MEDIUM", "HARD");
            };
            ProblemBankItem pick = null;
            for (String d : wanted) {
                List<ProblemBankItem> level = candidates.stream().filter(c -> c.getDifficulty().equals(d)).toList();
                List<ProblemBankItem> forCompany = level.stream().filter(c -> c.relevantTo(company)).toList();
                List<ProblemBankItem> pool = forCompany.isEmpty() ? level : forCompany;
                if (!pool.isEmpty()) {
                    // Prefer the earliest in the curated ladder, with a little daily variety among the first three.
                    pick = pool.get(rnd.nextInt(Math.min(3, pool.size())));
                    break;
                }
            }
            if (pick == null) continue;
            String reason = switch (s.strength()) {
                case "WEAK" -> "You've started " + s.name() + " but it's still shaky. This one builds it up.";
                case "NOT_STARTED" -> "Next core pattern on your path: " + s.name() + ". Start easy.";
                case "OK" -> s.name() + " is getting solid. Level up with a slightly harder one.";
                default -> "Keep " + s.name() + " sharp.";
            };
            out.add(new Suggestion(view(pick, done, names), reason));
        }
        return out;
    }

    private BankProblem view(ProblemBankItem p, Set<Long> done, Map<String, String> names) {
        return new BankProblem(p.getId(), p.getSlug(), p.getTitle(), p.getUrl(), p.getPlatform(), p.getDifficulty(), p.getPatternCode(),
                names.getOrDefault(p.getPatternCode(), p.getPatternCode()), p.getCompanies(), p.getWhy(), done.contains(p.getId()));
    }

    /** A bank problem counts as solved if you logged it from the bank OR logged a problem with the same title. */
    private Set<Long> solvedIds(Long userId) {
        Set<Long> ids = new HashSet<>(solved.solvedBankIds(userId));
        Set<String> titles = solved.findAllByUserId(userId).stream().map(ProblemSolved::getTitle).map(t -> t.trim().toLowerCase()).collect(Collectors.toSet());
        if (!titles.isEmpty()) {
            bank.findAll().stream().filter(b -> titles.contains(b.getTitle().trim().toLowerCase())).forEach(b -> ids.add(b.getId()));
        }
        return ids;
    }

    private Map<String, String> patternNames() {
        return patterns.findAll().stream().collect(Collectors.toMap(Pattern::getCode, Pattern::getName));
    }
}
