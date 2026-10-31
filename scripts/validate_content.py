#!/usr/bin/env python3
"""Validate lesson YAML files. Usage: python3 scripts/validate_content.py [files...]"""
import glob, os, re, sys
import yaml

ROOT = os.path.join(os.path.dirname(__file__), "..", "backend", "src", "main", "resources", "content")
TYPES = {"MCQ", "TRUE_FALSE", "PREDICT_OUTPUT", "SPOT_THE_BUG", "FILL_THE_CODE", "MATCH"}
OPTION_TYPES = {"MCQ", "TRUE_FALSE", "PREDICT_OUTPUT", "SPOT_THE_BUG"}
COMPANIES = {"INFOSYS_SP", "INFOSYS_DSE", "TCS", "WIPRO", "COGNIZANT", "ACCENTURE", "CAPGEMINI", "PRODUCT", "ALL"}
VISUALIZERS = {None, "array", "two-pointers", "sliding-window", "linked-list", "stack-queue", "recursion"}
BANNED = re.compile(r"(actually asked|was asked (at|in|by) (infosys|tcs|wipro|cognizant|accenture|capgemini))", re.I)


def need(cond, errs, msg):
    if not cond:
        errs.append(msg)


def text(v):
    return isinstance(v, str) and v.strip() != ""


def check(path):
    errs = []
    try:
        with open(path, encoding="utf-8") as f:
            d = yaml.safe_load(f)
    except Exception as e:  # noqa
        return [f"YAML parse error: {e}"]
    if not isinstance(d, dict):
        return ["top level must be a mapping"]
    slug = d.get("slug")
    need(text(slug) and re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", slug or ""), errs, "slug must be kebab-case")
    need(os.path.basename(path) == f"{slug}.yaml", errs, "file name must be <slug>.yaml")
    need(d.get("world") in {"JAVA_CORE", "DSA"}, errs, "world must be JAVA_CORE or DSA")
    need(isinstance(d.get("order"), int) and d["order"] >= 1, errs, "order must be int >= 1")
    need(isinstance(d.get("version"), int) and d["version"] >= 1, errs, "version must be int >= 1")
    need(isinstance(d.get("estimatedMinutes"), int), errs, "estimatedMinutes must be int")
    for k in ["section", "title", "infosysRelevance", "otherCompanies", "why", "analogy", "explanation"]:
        need(text(d.get(k)), errs, f"{k} is required")
    comps = d.get("companies") or []
    need(isinstance(comps, list) and comps and set(comps) <= COMPANIES, errs, f"companies must be a non-empty subset of {sorted(COMPANIES)}")
    v = d.get("visual") or {}
    need(text(v.get("diagram")) and text(v.get("caption")), errs, "visual.diagram and visual.caption required")
    need(v.get("visualizer") in VISUALIZERS, errs, f"visual.visualizer must be one of {VISUALIZERS}")
    tc = d.get("tinyCode") or {}
    need(text(tc.get("code")) and text(tc.get("walkthrough")), errs, "tinyCode.code and walkthrough required")
    ti = d.get("tryIt") or {}
    need(text(ti.get("instructions")) and text(ti.get("code")), errs, "tryIt.instructions and code required")
    if text(ti.get("code")):
        n = len([l for l in ti["code"].splitlines() if l.strip()])
        need(3 <= n <= 30, errs, f"tryIt.code should be 3-30 non-empty lines (has {n})")
    p = d.get("predict") or {}
    need(text(p.get("code")) and p.get("expectedOutput") is not None and text(p.get("explanation")), errs, "predict.code/expectedOutput/explanation required")
    cm = d.get("commonMistakes") or []
    need(isinstance(cm, list) and 3 <= len(cm) <= 8, errs, "commonMistakes: 3-8 items")
    ib = d.get("interviewBox") or {}
    need(text(ib.get("why")), errs, "interviewBox.why required")
    iq = ib.get("questions") or []
    need(2 <= len(iq) <= 6 and all(text(x.get("q")) and text(x.get("a")) for x in iq), errs, "interviewBox.questions: 2-6 with q and a")
    quiz = d.get("quiz") or []
    need(len(quiz) >= 5, errs, f"quiz needs >= 5 questions (has {len(quiz)})")
    ids, types = set(), set()
    for i, q in enumerate(quiz):
        where = f"quiz[{i}]"
        qid = q.get("id")
        need(text(qid) and qid not in ids, errs, f"{where}: id missing or duplicate")
        ids.add(qid)
        t = q.get("type")
        need(t in TYPES, errs, f"{where}: bad type {t}")
        types.add(t)
        need(text(q.get("prompt")), errs, f"{where}: prompt required")
        need(text(q.get("explanation")), errs, f"{where}: explanation required")
        if t in {"PREDICT_OUTPUT", "SPOT_THE_BUG", "FILL_THE_CODE"}:
            need(text(q.get("code")), errs, f"{where}: code required for {t}")
        if t in OPTION_TYPES:
            opts = q.get("options") or []
            need(isinstance(opts, list) and len(opts) >= 2 and all(isinstance(o, (str, int, float, bool)) for o in opts), errs, f"{where}: options list required")
            need(len(set(map(str, opts))) == len(opts), errs, f"{where}: options must be unique")
            a = q.get("answer")
            need(isinstance(a, int) and not isinstance(a, bool) and 0 <= a < len(opts), errs, f"{where}: answer must be a valid index")
            if t == "TRUE_FALSE":
                need([str(o) for o in opts] == ["True", "False"], errs, f"{where}: TRUE_FALSE options must be [True, False]")
        if t == "FILL_THE_CODE":
            need("____" in (q.get("code") or ""), errs, f"{where}: code must contain ____")
            acc = q.get("acceptedAnswers") or []
            need(isinstance(acc, list) and acc and all(text(str(x)) for x in acc), errs, f"{where}: acceptedAnswers required")
        if t == "MATCH":
            pairs = q.get("pairs") or []
            need(3 <= len(pairs) <= 6 and all(text(x.get("left")) and text(x.get("right")) for x in pairs), errs, f"{where}: 3-6 pairs with left/right")
            need(len({x.get("right") for x in pairs}) == len(pairs), errs, f"{where}: rights must be unique")
    need(len(types) >= 3, errs, "quiz should use at least 3 different question types")
    need(types & {"PREDICT_OUTPUT", "SPOT_THE_BUG"}, errs, "quiz needs a PREDICT_OUTPUT or SPOT_THE_BUG question")
    eb = d.get("explainBack") or {}
    need(text(eb.get("prompt")) and text(eb.get("modelAnswer")) and 3 <= len(eb.get("rubric") or []) <= 6, errs, "explainBack: prompt, 3-6 rubric items, modelAnswer")
    fc = d.get("flashcards") or []
    need(3 <= len(fc) <= 8 and all(text(x.get("front")) and text(x.get("back")) for x in fc), errs, "flashcards: 3-8 with front/back")
    kt = d.get("keyTakeaways") or []
    need(3 <= len(kt) <= 8, errs, "keyTakeaways: 3-8 items")
    blob = yaml.safe_dump(d)
    need(not BANNED.search(blob), errs, "never claim a question was actually asked at a company")
    return errs


def main():
    files = sys.argv[1:] or sorted(glob.glob(os.path.join(ROOT, "lessons", "*", "*.yaml")))
    bad = 0
    slugs = {}
    for f in files:
        errs = check(f)
        with open(f, encoding="utf-8") as fh:
            try:
                s = (yaml.safe_load(fh) or {}).get("slug")
            except Exception:
                s = None
        if s in slugs:
            errs.append(f"duplicate slug also in {slugs[s]}")
        slugs[s] = f
        if errs:
            bad += 1
            print(f"FAIL {os.path.relpath(f)}")
            for e in errs:
                print(f"   - {e}")
        else:
            print(f"ok   {os.path.relpath(f)}")
    print(f"\n{len(files) - bad}/{len(files)} valid")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
