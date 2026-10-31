# Question bank formats (YAML)

All files live in `backend/src/main/resources/content/banks/`. They are loaded into the
database on startup (upsert by `id`/`slug`). Validate with `python3 scripts/validate_banks.py`.

Honesty rule for every bank: items are **practice questions in the style of** a company.
Never write "asked at <company>", "actual question", or claim the current test pattern.

Company codes: INFOSYS_SP, INFOSYS_DSE, TCS, WIPRO, COGNIZANT, ACCENTURE, CAPGEMINI, PRODUCT, ALL

## problems.yaml (curated practice problems on free public sites)
```yaml
problems:
  - slug: two-sum                      # unique kebab-case
    title: "Two Sum"
    url: "https://leetcode.com/problems/two-sum/"   # must be a real, free problem URL
    platform: LEETCODE                 # LEETCODE | GEEKSFORGEEKS | HACKERRANK
    difficulty: EASY                   # EASY | MEDIUM | HARD
    pattern: HASHING                   # one pattern code (see below)
    companies: [ALL]
    why: "One line: what this problem teaches."
```
Pattern codes: COMPLEXITY, ARRAY_BASICS, TWO_POINTERS, SLIDING_WINDOW, PREFIX_SUM, HASHING, STRINGS,
RECURSION, BACKTRACKING, LINKED_LIST, STACK_QUEUE, SORTING, BINARY_SEARCH, TREES, HEAPS, GRAPHS, GREEDY,
DP, BIT_MANIPULATION, MATH

## interview-*.yaml (interview practice Q&A)
```yaml
questions:
  - id: java-001                       # unique across all interview files
    category: JAVA                     # JAVA | OOP | COLLECTIONS | EXCEPTIONS | JAVA8 | MULTITHREADING | JVM |
                                       # SQL | DBMS | OS | CN | DSA | SPRING | HR | BEHAVIORAL | PROJECT
    topic: "String pool"
    difficulty: EASY                   # EASY | MEDIUM | HARD
    companies: [ALL]
    question: "..."
    answer: |                          # simple, spoken-style, 40-150 words; small code allowed
    keyPoints: ["...", "..."]          # 2-5 points a good answer must contain (used for self-scoring)
```

## aptitude.yaml
```yaml
questions:
  - id: apt-001
    section: QUANT                     # QUANT | LOGICAL | VERBAL
    topic: "Percentages"
    difficulty: EASY
    companies: [INFOSYS_SP, INFOSYS_DSE, ALL]
    question: "..."
    options: ["...", "...", "...", "..."]
    answer: 2                          # 0-based index
    explanation: "step-by-step, simple"
```

## spring.yaml
```yaml
annotations:
  - name: "@Component"
    category: "Core"                   # Core | Web | Data | Validation | Config | Testing
    meaning: "..."
    analogy: "..."
    snippet: |
    quiz: { prompt: "...", options: ["..."], answer: 0, explanation: "..." }
topics:                                # the Spring concept map (World 5 / Spring track)
  - code: IOC_DI
    title: "IoC and Dependency Injection"
    order: 1
    group: "Core"                      # Core | Web | Data | Security | Testing | Ops
    summary: "..."
    checklist: ["...", "..."]          # 3-6 "I can..." statements
    quiz:                              # exactly 3 MCQs
      - { prompt: "...", options: ["..."], answer: 0, explanation: "..." }
    explainBack: { prompt: "...", rubric: ["...", "...", "..."] }
debugChallenges:
  - id: dbg-001
    title: "NoSuchBeanDefinitionException"
    error: |                           # the error/log text
    code: |                            # the buggy code
    question: "What is the cause?"
    options: ["..."]
    answer: 1
    explanation: "..."
    fix: |                             # corrected code or steps
projects:
  - code: TASK_MANAGER
    title: "Task Manager API"
    description: "..."
    milestones: ["...", "..."]         # 5-8 milestones
```

## speaking.yaml
```yaml
prompts:
  - id: spk-001
    category: SELF_INTRO               # SELF_INTRO | TECHNICAL | HR | BEHAVIORAL | PROJECT | WHY_COMPANY
    prompt: "Introduce yourself"
    framework: PPF                     # PPF (Present-Past-Future) | STAR | CEU (Concept-Example-Use case) | NONE
    timeLimitSeconds: 90
    tips: ["...", "..."]
    sampleAnswer: |
phrases:     [{ phrase: "...", useWhen: "..." }]
vocabulary:  [{ word: "...", meaning: "...", example: "..." }]
pronunciation: [{ term: "Hibernate", sayItLike: "HY-ber-nate", note: "..." }]
```

## world-outlines.yaml (titled outlines for worlds not fully written yet)
```yaml
worlds:
  - world: SQL
    lessons: [{ title: "...", summary: "..." }]
```
