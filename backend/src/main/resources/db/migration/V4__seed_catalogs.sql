-- =====================================================================
-- V4: seed shared catalogs
-- =====================================================================

INSERT INTO company_tags (code, name, sort_order) VALUES
 ('INFOSYS_SP',  'Infosys SP',          1),
 ('INFOSYS_DSE', 'Infosys DSE',         2),
 ('TCS',         'TCS',                 3),
 ('WIPRO',       'Wipro',               4),
 ('COGNIZANT',   'Cognizant',           5),
 ('ACCENTURE',   'Accenture',           6),
 ('CAPGEMINI',   'Capgemini',           7),
 ('PRODUCT',     'Product companies',   8),
 ('ALL',         'All companies',       9);

INSERT INTO worlds (code, name, description, sort_order) VALUES
 ('JAVA_CORE',   'Java Core Island',            'Java from zero: foundations, strings, OOP, exceptions, collections, Java 8+, threads, JVM.', 1),
 ('DSA',         'DSA Kingdom',                 'Problem-solving patterns: two pointers, sliding window, hashing, recursion, trees, graphs, DP.', 2),
 ('SQL',         'SQL Caves',                   'Joins, GROUP BY, subqueries, window functions, indexes, normalization, transactions.', 3),
 ('CS_CORE',     'CS Core Temple',              'Operating systems, DBMS, networking basics and design basics.', 4),
 ('SPRING',      'Spring Boot Forge',           'IoC, beans, REST, JPA, validation, security with JWT, testing, Docker.', 5),
 ('INTERVIEW',   'Interview Colosseum',         'Practice question banks, timed mocks and mock-interview boss fights.', 6),
 ('APTITUDE',    'Aptitude and Reasoning Arena','Quant, logical reasoning and verbal basics in the Infosys style.', 7);

INSERT INTO patterns (code, name, description, sort_order, sp_core) VALUES
 ('COMPLEXITY',     'Complexity analysis',      'Counting how work grows with input size (Big-O).', 1, TRUE),
 ('ARRAY_BASICS',   'Array traversal',          'Walk the array once, track what you need.', 2, TRUE),
 ('TWO_POINTERS',   'Two pointers',             'Two indexes moving toward each other or together.', 3, TRUE),
 ('SLIDING_WINDOW', 'Sliding window',           'Keep a window of elements and slide it instead of restarting.', 4, TRUE),
 ('PREFIX_SUM',     'Prefix sum',               'Pre-add totals so any range sum is one subtraction.', 5, TRUE),
 ('HASHING',        'Hashing (HashMap/HashSet)','Remember what you have seen for O(1) lookups.', 6, TRUE),
 ('STRINGS',        'String manipulation',      'Reverse, count characters, palindromes, anagrams.', 7, TRUE),
 ('RECURSION',      'Recursion',                'A function that solves a smaller copy of the same problem.', 8, TRUE),
 ('BACKTRACKING',   'Backtracking',             'Try a choice, recurse, undo the choice.', 9, FALSE),
 ('LINKED_LIST',    'Linked list',              'Pointer juggling: reverse, find middle, detect cycle.', 10, TRUE),
 ('STACK_QUEUE',    'Stack / Queue',            'Last-in-first-out and first-in-first-out helpers, monotonic stack.', 11, TRUE),
 ('SORTING',        'Sorting',                  'Bubble, merge, quick and using sort as a tool.', 12, TRUE),
 ('BINARY_SEARCH',  'Binary search',            'Halve the search space each step on sorted data or answers.', 13, TRUE),
 ('TREES',          'Trees / BST',              'Traversals, height, BST insert/search.', 14, TRUE),
 ('HEAPS',          'Heaps / PriorityQueue',    'Always get the smallest or largest quickly; top-K problems.', 15, FALSE),
 ('GRAPHS',         'Graphs (BFS/DFS)',         'Nodes and edges; explore level by level or deep first.', 16, TRUE),
 ('GREEDY',         'Greedy',                   'Take the best local choice when it is provably safe.', 17, FALSE),
 ('DP',             'Dynamic programming',      'Remember answers to sub-problems so you never recompute.', 18, TRUE),
 ('BIT_MANIPULATION','Bit manipulation',        'Use binary tricks: XOR, masks, shifts.', 19, FALSE),
 ('MATH',           'Math / number theory',     'Primes, GCD, digits, modulo arithmetic.', 20, FALSE);

INSERT INTO mistake_categories (code, name, tip) VALUES
 ('MISREAD',        'Misread the problem',       'Re-read the problem and write the input/output in your own words before coding.'),
 ('EDGE_CASE',      'Missed an edge case',       'Always test: empty input, one element, all same, negatives, max size.'),
 ('WRONG_COMPLEXITY','Wrong complexity',          'Estimate Big-O before coding; check it fits the input limits.'),
 ('OFF_BY_ONE',     'Off-by-one',                'Check loop bounds with a tiny example: does i reach the last index? one too far?'),
 ('NO_DRY_RUN',     'Skipped the dry run',       'Trace your code by hand on one small example before submitting.'),
 ('SYNTAX_API',     'Syntax / Java API slip',    'Write the API call in a scratch file first; note it on a flashcard.'),
 ('NULL_HANDLING',  'Null / empty handling',     'Ask "what if this is null or empty?" for every input and every map lookup.'),
 ('OVERFLOW',       'Integer overflow',          'If numbers can exceed ~2 billion use long; watch multiplication.'),
 ('CONCEPT_GAP',    'Concept gap',               'Go back to the lesson, then explain the concept out loud in 3 sentences.'),
 ('PANIC_TIME',     'Time pressure / panic',     'Use the framework card: brute force first, then optimize.'),
 ('OTHER',          'Other',                     'Describe it in your own words; patterns appear after a few entries.');

INSERT INTO quests (code, kind, title, description, trigger_type, target, gold_reward) VALUES
 ('FOCUS_25',      'DAILY', 'One deep-work block',            'Finish one 25-minute focus session on any topic.',                   'FOCUS_25',         1, 15),
 ('TOPIC_STEP',    'DAILY', 'Move a Java topic forward',      'Change one Java topic status (for example Learning to Understood).', 'TOPIC_PROGRESS',   1, 15),
 ('LOG_PROBLEM',   'DAILY', 'Solve and explain',              'Log one problem with your approach written in your own words.',     'PROBLEM_LOGGED',   1, 20),
 ('RECORD_MISTAKE','DAILY', 'Honest bug report',              'Write down one mistake and what you learned from it.',             'MISTAKE_RECORDED', 1, 10),
 ('REVISIT',       'DAILY', 'Rematch an old monster',         'Re-solve one problem from your revisit queue from scratch.',       'REVISIT_DONE',     1, 25),
 ('COMMIT',        'DAILY', 'Plan tomorrow',                  'Pick your 3 tasks for tomorrow.',                                  'DAILY_COMMITMENT', 1, 10),
 ('MVD',           'DAILY', 'Minimum Viable Day',             'Just 20 focused minutes. That keeps your streak alive.',           'MVD_DONE',         1, 10),
 ('COMEBACK_EASY', 'DAILY', 'Welcome back, adventurer',       'Start small: one 10-minute focus session. No pressure.',           'FOCUS_ANY',        1, 20);
