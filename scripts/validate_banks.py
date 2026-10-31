#!/usr/bin/env python3
"""Validate question bank YAML files in backend/src/main/resources/content/banks."""
import glob, os, re, sys
import yaml

ROOT = os.path.join(os.path.dirname(__file__), "..", "backend", "src", "main", "resources", "content", "banks")
COMPANIES = {"INFOSYS_SP", "INFOSYS_DSE", "TCS", "WIPRO", "COGNIZANT", "ACCENTURE", "CAPGEMINI", "PRODUCT", "ALL"}
PATTERNS = {"COMPLEXITY", "ARRAY_BASICS", "TWO_POINTERS", "SLIDING_WINDOW", "PREFIX_SUM", "HASHING", "STRINGS", "RECURSION",
            "BACKTRACKING", "LINKED_LIST", "STACK_QUEUE", "SORTING", "BINARY_SEARCH", "TREES", "HEAPS", "GRAPHS", "GREEDY", "DP",
            "BIT_MANIPULATION", "MATH"}
CATS = {"JAVA", "OOP", "COLLECTIONS", "EXCEPTIONS", "JAVA8", "MULTITHREADING", "JVM", "SQL", "DBMS", "OS", "CN", "DSA", "SPRING",
        "HR", "BEHAVIORAL", "PROJECT"}
DIFF = {"EASY", "MEDIUM", "HARD"}
BANNED = re.compile(r"(actually asked|was asked (at|in|by) (infosys|tcs|wipro|cognizant|accenture|capgemini))", re.I)
errors = []


def t(v):
    return isinstance(v, str) and v.strip() != ""


def need(c, m):
    if not c:
        errors.append(m)


def mcq(q, where, n_min=2):
    o = q.get("options") or []
    need(isinstance(o, list) and len(o) >= n_min and len(set(map(str, o))) == len(o), f"{where}: options (unique, >= {n_min})")
    a = q.get("answer")
    need(isinstance(a, int) and not isinstance(a, bool) and 0 <= a < len(o), f"{where}: answer index")
    need(t(q.get("explanation")), f"{where}: explanation")
    need(t(q.get("prompt") or q.get("question")), f"{where}: prompt/question")


def comps(c, where):
    need(isinstance(c, list) and c and set(c) <= COMPANIES, f"{where}: companies")


def load(name):
    p = os.path.join(ROOT, name)
    if not os.path.exists(p):
        return None
    with open(p, encoding="utf-8") as f:
        d = yaml.safe_load(f)
    blob = yaml.safe_dump(d)
    need(not BANNED.search(blob), f"{name}: never claim questions were actually asked at a company")
    return d


d = load("problems.yaml")
if d is not None:
    seen = set()
    for i, p in enumerate(d.get("problems", [])):
        w = f"problems[{i}] {p.get('slug')}"
        need(t(p.get("slug")) and p["slug"] not in seen, f"{w}: slug unique")
        seen.add(p.get("slug"))
        need(t(p.get("title")), f"{w}: title")
        need(t(p.get("url")) and p["url"].startswith("https://"), f"{w}: https url")
        need(p.get("platform") in {"LEETCODE", "GEEKSFORGEEKS", "HACKERRANK"}, f"{w}: platform")
        need(p.get("difficulty") in DIFF, f"{w}: difficulty")
        need(p.get("pattern") in PATTERNS, f"{w}: pattern")
        comps(p.get("companies"), w)
        need(t(p.get("why")), f"{w}: why")
    print(f"problems.yaml: {len(d.get('problems', []))} problems")

ids = set()
for f in sorted(glob.glob(os.path.join(ROOT, "interview-*.yaml"))):
    name = os.path.basename(f)
    d = load(name)
    for i, q in enumerate(d.get("questions", [])):
        w = f"{name}[{i}] {q.get('id')}"
        need(t(q.get("id")) and q["id"] not in ids, f"{w}: id unique")
        ids.add(q.get("id"))
        need(q.get("category") in CATS, f"{w}: category")
        need(q.get("difficulty") in DIFF, f"{w}: difficulty")
        comps(q.get("companies"), w)
        need(t(q.get("topic")) and t(q.get("question")) and t(q.get("answer")), f"{w}: topic/question/answer")
        kp = q.get("keyPoints") or []
        need(2 <= len(kp) <= 6, f"{w}: 2-6 keyPoints")
    print(f"{name}: {len(d.get('questions', []))} questions")

d = load("aptitude.yaml")
if d is not None:
    aids = set()
    for i, q in enumerate(d.get("questions", [])):
        w = f"aptitude[{i}] {q.get('id')}"
        need(t(q.get("id")) and q["id"] not in aids, f"{w}: id unique")
        aids.add(q.get("id"))
        need(q.get("section") in {"QUANT", "LOGICAL", "VERBAL"}, f"{w}: section")
        need(q.get("difficulty") in DIFF, f"{w}: difficulty")
        comps(q.get("companies"), w)
        need(t(q.get("topic")), f"{w}: topic")
        mcq(q, w, 4)
    print(f"aptitude.yaml: {len(d.get('questions', []))} questions")

d = load("spring.yaml")
if d is not None:
    for i, a in enumerate(d.get("annotations", [])):
        w = f"annotations[{i}] {a.get('name')}"
        need(t(a.get("name")) and a["name"].startswith("@"), f"{w}: name")
        for k in ["category", "meaning", "analogy", "snippet"]:
            need(t(a.get(k)), f"{w}: {k}")
        mcq(a.get("quiz") or {}, f"{w}.quiz")
    codes = set()
    for i, tp in enumerate(d.get("topics", [])):
        w = f"topics[{i}] {tp.get('code')}"
        need(t(tp.get("code")) and tp["code"] not in codes and re.fullmatch(r"[A-Z0-9_]+", tp["code"]), f"{w}: code")
        codes.add(tp.get("code"))
        for k in ["title", "group", "summary"]:
            need(t(tp.get(k)), f"{w}: {k}")
        need(isinstance(tp.get("order"), int), f"{w}: order")
        need(3 <= len(tp.get("checklist") or []) <= 6, f"{w}: checklist 3-6")
        qz = tp.get("quiz") or []
        need(len(qz) == 3, f"{w}: exactly 3 quiz questions")
        for j, q in enumerate(qz):
            mcq(q, f"{w}.quiz[{j}]")
        eb = tp.get("explainBack") or {}
        need(t(eb.get("prompt")) and len(eb.get("rubric") or []) >= 3, f"{w}: explainBack")
    for i, c in enumerate(d.get("debugChallenges", [])):
        w = f"debug[{i}] {c.get('id')}"
        for k in ["id", "title", "error", "code", "question", "fix"]:
            need(t(c.get(k)), f"{w}: {k}")
        mcq(c, w)
    for i, p in enumerate(d.get("projects", [])):
        w = f"projects[{i}]"
        need(t(p.get("code")) and t(p.get("title")) and t(p.get("description")), f"{w}: code/title/description")
        need(5 <= len(p.get("milestones") or []) <= 10, f"{w}: 5-10 milestones")
    print(f"spring.yaml: {len(d.get('annotations', []))} annotations, {len(d.get('topics', []))} topics, "
          f"{len(d.get('debugChallenges', []))} debug challenges, {len(d.get('projects', []))} projects")

d = load("speaking.yaml")
if d is not None:
    sids = set()
    for i, p in enumerate(d.get("prompts", [])):
        w = f"prompts[{i}] {p.get('id')}"
        need(t(p.get("id")) and p["id"] not in sids, f"{w}: id")
        sids.add(p.get("id"))
        need(p.get("category") in {"SELF_INTRO", "TECHNICAL", "HR", "BEHAVIORAL", "PROJECT", "WHY_COMPANY"}, f"{w}: category")
        need(p.get("framework") in {"PPF", "STAR", "CEU", "NONE"}, f"{w}: framework")
        need(isinstance(p.get("timeLimitSeconds"), int), f"{w}: timeLimitSeconds")
        need(t(p.get("prompt")) and t(p.get("sampleAnswer")) and len(p.get("tips") or []) >= 2, f"{w}: prompt/sampleAnswer/tips")
    for k, fields in [("phrases", ["phrase", "useWhen"]), ("vocabulary", ["word", "meaning", "example"]), ("pronunciation", ["term", "sayItLike"])]:
        for i, x in enumerate(d.get(k, [])):
            for fld in fields:
                need(t(x.get(fld)), f"{k}[{i}]: {fld}")
    print(f"speaking.yaml: {len(d.get('prompts', []))} prompts, {len(d.get('phrases', []))} phrases, "
          f"{len(d.get('vocabulary', []))} words, {len(d.get('pronunciation', []))} pronunciations")

d = load("world-outlines.yaml")
if d is not None:
    for i, wv in enumerate(d.get("worlds", [])):
        need(t(wv.get("world")) and len(wv.get("lessons") or []) >= 3, f"worlds[{i}]")
        for j, l in enumerate(wv.get("lessons") or []):
            need(t(l.get("title")) and t(l.get("summary")), f"worlds[{i}].lessons[{j}]")
    print(f"world-outlines.yaml: {len(d.get('worlds', []))} worlds")

if errors:
    print("\nERRORS:")
    for e in errors[:200]:
        print(" -", e)
    print(f"{len(errors)} errors")
    sys.exit(1)
print("\nall banks valid")
