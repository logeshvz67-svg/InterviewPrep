# Lesson content format (YAML)

Lessons live in `backend/src/main/resources/content/lessons/<world>/<slug>.yaml`.
On startup the backend loads them into the database (`ContentSeeder`). A file is
re-imported only when its `version` is higher than the database copy **and** the lesson
was not edited on the admin page. After editing a file, bump `version`.

Validate before committing:

```bash
python3 scripts/validate_content.py
```

## Teaching flow (every lesson)

Why → Analogy → Visual → Explanation → Tiny code → Try it yourself (type it) →
Predict the output → Quiz (≥5 questions, 80% to pass) → Explain it back → flashcards.

## Fields

```yaml
slug: how-java-works          # unique, kebab-case. Java lessons: SAME as the java_topics slug
world: JAVA_CORE              # JAVA_CORE | DSA
section: A                    # Java: A..J. DSA: "DSA"
order: 1                      # order inside the section (lessons unlock in this order)
title: "How Java works: JDK, JRE, JVM"
version: 1
estimatedMinutes: 25
rebuild: true                 # "you may have seen this; we rebuild it properly"
patternCode: null             # DSA lessons: a pattern code (TWO_POINTERS...), else null
companies: [ALL]              # company tags (INFOSYS_SP, INFOSYS_DSE, TCS, WIPRO, COGNIZANT, ACCENTURE, CAPGEMINI, PRODUCT, ALL)
infosysRelevance: "..."       # our prep judgement; never claim the actual test contents
otherCompanies: "..."
why: |                        # markdown: why this matters (2-5 short paragraphs / bullets)
analogy: |                    # markdown: one strong everyday analogy
visual:
  diagram: |                  # monospace ASCII diagram (rendered in a <pre>), max ~70 chars wide
  caption: "plain-English caption"
  visualizer: null            # optional id of an interactive visualizer: array | two-pointers |
                              # sliding-window | linked-list | stack-queue | recursion
explanation: |                # markdown: the full lesson body. Use ### subheadings, short
                              # paragraphs, tables, fenced ```java code. Define every term in plain words.
tinyCode:
  code: |                     # small, complete, runnable Java
  walkthrough: |              # markdown, line-by-line in simple words
tryIt:
  instructions: "..."         # what to type and why
  code: |                     # the code the learner must TYPE (paste is disabled); 5-25 lines
predict:
  code: |                     # snippet
  expectedOutput: |           # exact output
  explanation: |              # why
commonMistakes:               # 3-6 bullets
  - "..."
interviewBox:
  why: "Why interviewers ask this: ..."
  questions:                  # 2-5 practice questions (labelled practice, never "actually asked")
    - q: "..."
      a: "..."               # simple, spoken-style answer
quiz:                         # >= 5 questions; mix types; ids unique within lesson
  - id: q1
    type: MCQ                 # MCQ | TRUE_FALSE | PREDICT_OUTPUT | SPOT_THE_BUG | FILL_THE_CODE | MATCH
    prompt: "..."
    code: null                # optional code block (required for PREDICT_OUTPUT / SPOT_THE_BUG / FILL_THE_CODE)
    options: ["a", "b", "c", "d"]   # MCQ/TRUE_FALSE/PREDICT_OUTPUT/SPOT_THE_BUG
    answer: 1                 # 0-based index into options
    explanation: "..."        # shown after answering: why right, why the others are wrong
  - id: q2
    type: TRUE_FALSE
    prompt: "..."
    options: ["True", "False"]
    answer: 0
    explanation: "... (the reason)"
  - id: q3
    type: FILL_THE_CODE
    prompt: "..."
    code: |                   # contains ____ exactly once
    acceptedAnswers: ["equals", "equals()"]   # compared ignoring spaces
    explanation: "..."
  - id: q4
    type: MATCH
    prompt: "Match each term to its meaning"
    pairs:                    # 3-5 pairs; the UI shuffles the right side
      - left: "JDK"
        right: "Tools to write and compile Java"
    explanation: "..."
explainBack:
  prompt: "Explain ... to a friend who doesn't code"
  rubric:                     # 3-5 checks the learner ticks honestly (non-AI fallback)
    - "I said ..."
  modelAnswer: |              # a good 60-120 word spoken-style answer
flashcards:                   # 3-6 cards, created when the lesson is completed
  - front: "..."
    back: "..."
keyTakeaways:                 # 3-6 bullets
  - "..."
```

## Rules

- Simple language. Every technical term gets a one-line plain explanation the first time.
- Code must compile and the stated output must be exactly right. Use Java 17+.
- At least one PREDICT_OUTPUT or SPOT_THE_BUG question per lesson, and at least 3 different types.
- Never say a question was "asked at Infosys/TCS/...". Use "practice question" / "common in interviews".
- Never state the current Infosys test pattern.
