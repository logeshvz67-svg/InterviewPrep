-- =====================================================================
-- V5: Java Core Island curriculum index (sections A..J).
-- Full lesson text is added in Phase 2+ (lessons table links to these).
-- tags: company codes; 'ALL' means relevant everywhere.
-- rebuild = TRUE means "you may have seen this before; we rebuild it properly".
-- infosys_relevance is OUR prep judgment, not a claim about the actual test.
-- =====================================================================
WITH t(section, section_name, sort_order, slug, title, summary, rebuild, infosys_relevance, tags) AS (VALUES
 -- A. Foundations
 ('A','Foundations', 1,'how-java-works','How Java works: JDK, JRE, JVM','Source code -> javac -> bytecode -> JVM. Why "write once, run anywhere" works.',TRUE,'Core theory for SP and DSE technical rounds','ALL'),
 ('A','Foundations', 2,'setup-first-program','Setup and your first program','Install JDK, write main(), compile and run from the terminal.',TRUE,'Foundation for every coding round','ALL'),
 ('A','Foundations', 3,'variables-data-types','Variables and data types','Primitive types, sizes, defaults, and when to use long vs int.',TRUE,'Common in MCQs and output questions','ALL'),
 ('A','Foundations', 4,'type-casting','Type casting','Widening vs narrowing, implicit vs explicit, overflow surprises.',TRUE,'Frequent in predict-the-output practice','ALL'),
 ('A','Foundations', 5,'operators','Operators','Arithmetic, relational, logical, bitwise, ternary, i++ vs ++i, precedence.',TRUE,'Frequent in predict-the-output practice','ALL'),
 ('A','Foundations', 6,'input-scanner-bufferedreader','Input: Scanner and BufferedReader','Reading input fast and correctly in coding tests.',TRUE,'Essential for timed coding tests','INFOSYS_SP,INFOSYS_DSE,TCS,WIPRO,COGNIZANT,ACCENTURE,CAPGEMINI'),
 ('A','Foundations', 7,'if-switch','Decisions: if and switch','if/else ladders, switch rules, fall-through.',TRUE,'Basic logic used in every problem','ALL'),
 ('A','Foundations', 8,'loops','Loops: for, while, do-while','Choosing the right loop, dry-running loops.',TRUE,'Basic logic used in every problem','ALL'),
 ('A','Foundations', 9,'break-continue-labels','break, continue and labeled loops','Jumping out of nested loops cleanly.',TRUE,'Appears in output-tracing questions','ALL'),
 ('A','Foundations',10,'methods-varargs','Methods and varargs','Parameters, return values, overloading preview, variable arguments.',TRUE,'Foundation for clean coding solutions','ALL'),
 ('A','Foundations',11,'recursion-basics','Recursion basics','Base case + smaller problem; the call stack explained.',TRUE,'Important for SP-level DSA','INFOSYS_SP,INFOSYS_DSE,PRODUCT,TCS'),
 ('A','Foundations',12,'arrays-1d-2d','Arrays: 1D and 2D','Declaring, iterating, matrix problems, default values.',TRUE,'Heavily used in coding rounds','ALL'),
 ('A','Foundations',13,'arrays-utility','The Arrays utility class','sort, binarySearch, fill, copyOf, asList, toString.',TRUE,'Saves time in timed coding tests','ALL'),
 -- B. Strings
 ('B','Strings', 1,'string-immutability','String immutability','Why Strings never change and why that is useful.',TRUE,'Classic technical interview question','ALL'),
 ('B','Strings', 2,'string-pool','String pool, new vs literal','== vs equals, intern(), where strings live in memory.',TRUE,'Classic predict-the-output topic','ALL'),
 ('B','Strings', 3,'stringbuilder-stringbuffer','StringBuilder vs StringBuffer','Mutable strings, thread-safety, performance in loops.',TRUE,'Common comparison question','ALL'),
 ('B','Strings', 4,'string-methods','Common String methods','charAt, substring, indexOf, split, toCharArray, compareTo and more.',TRUE,'Needed for most string coding problems','ALL'),
 ('B','Strings', 5,'string-problems','String interview problems','Reverse, palindrome, anagram, character frequency, first non-repeating char.',TRUE,'Very common in coding rounds','ALL'),
 -- C. OOP
 ('C','OOP', 1,'class-object','Class and object','Blueprint vs real thing; fields, methods, the new keyword.',TRUE,'Core OOP theory for every round','ALL'),
 ('C','OOP', 2,'constructors','Constructors and chaining','Default, parameterized, this() and super() chaining.',TRUE,'Common OOP question','ALL'),
 ('C','OOP', 3,'this-keyword','The this keyword','Referring to the current object; resolving name clashes.',TRUE,'Basic OOP question','ALL'),
 ('C','OOP', 4,'static','static: variables, methods, blocks','Belongs to the class, not the object; when static blocks run.',TRUE,'Frequent in output and theory questions','ALL'),
 ('C','OOP', 5,'final','final keyword','final variables, methods and classes.',TRUE,'Common comparison question (final/finally/finalize)','ALL'),
 ('C','OOP', 6,'init-order','Instance blocks and initialization order','Static blocks, instance blocks, constructors: who runs first.',TRUE,'Popular tricky output question','INFOSYS_SP,INFOSYS_DSE,PRODUCT,TCS'),
 ('C','OOP', 7,'inheritance-super','Inheritance and super','Reusing code with extends; calling the parent with super.',TRUE,'Core OOP theory','ALL'),
 ('C','OOP', 8,'overloading-overriding','Overloading vs overriding','Same name, different parameters vs same signature in child class.',TRUE,'Top-asked OOP comparison','ALL'),
 ('C','OOP', 9,'polymorphism','Polymorphism and casting','Compile-time vs runtime, upcasting and downcasting.',TRUE,'Core OOP theory','ALL'),
 ('C','OOP',10,'abstraction-interfaces','Abstraction: abstract class vs interface','Default, static and private interface methods; when to pick which.',TRUE,'Top-asked OOP comparison','ALL'),
 ('C','OOP',11,'encapsulation-access','Encapsulation, access modifiers, packages','private/default/protected/public and organizing code.',TRUE,'Core OOP theory','ALL'),
 ('C','OOP',12,'nested-classes','Inner, nested and anonymous classes','Static nested vs inner vs local vs anonymous.',FALSE,'Depth topic for SP','INFOSYS_SP,PRODUCT'),
 ('C','OOP',13,'enums-records','Enums and records','Fixed sets of constants; compact data carriers.',FALSE,'Modern Java knowledge','INFOSYS_SP,INFOSYS_DSE,PRODUCT'),
 ('C','OOP',14,'object-class','The Object class','equals, hashCode, toString, clone and their contracts.',TRUE,'Key for collections questions','ALL'),
 ('C','OOP',15,'wrappers-autoboxing','Wrapper classes and autoboxing','Integer cache, == traps, unboxing nulls.',TRUE,'Popular tricky output question','ALL'),
 ('C','OOP',16,'pass-by-value','Java is pass-by-value','What really gets copied when you pass objects.',TRUE,'Classic interview trap','ALL'),
 ('C','OOP',17,'immutable-class','Designing an immutable class','final class, private final fields, defensive copies.',FALSE,'Common design question','INFOSYS_SP,INFOSYS_DSE,PRODUCT,TCS'),
 -- D. Exceptions
 ('D','Exceptions', 1,'exception-hierarchy','Exception hierarchy','Throwable, Error, Exception, RuntimeException.',FALSE,'Core theory','ALL'),
 ('D','Exceptions', 2,'checked-unchecked','Checked vs unchecked','Compiler-forced handling vs runtime bugs.',FALSE,'Top-asked comparison','ALL'),
 ('D','Exceptions', 3,'try-catch-finally','try, catch, finally','Flow of control, multi-catch, ordering of catch blocks.',FALSE,'Common output questions','ALL'),
 ('D','Exceptions', 4,'throw-throws-custom','throw vs throws, custom exceptions','Raising and declaring exceptions; writing your own.',FALSE,'Common comparison question','ALL'),
 ('D','Exceptions', 5,'try-with-resources','try-with-resources','Auto-closing resources with AutoCloseable.',FALSE,'Modern Java knowledge','ALL'),
 ('D','Exceptions', 6,'exception-traps','finally edge cases and traps','return in finally, System.exit, swallowed exceptions.',FALSE,'Tricky output questions','INFOSYS_SP,INFOSYS_DSE,PRODUCT'),
 -- E. Generics and Collections
 ('E','Generics and Collections', 1,'generics','Generics','Type-safe containers, generic methods and classes.',FALSE,'Core for collections','ALL'),
 ('E','Generics and Collections', 2,'wildcards-erasure','Wildcards and type erasure','? extends / ? super (PECS), what erasure removes at runtime.',FALSE,'Depth topic for SP','INFOSYS_SP,PRODUCT'),
 ('E','Generics and Collections', 3,'collections-hierarchy','Collections hierarchy','Collection, List, Set, Queue, Map and their main classes.',FALSE,'Top-asked topic','ALL'),
 ('E','Generics and Collections', 4,'list-implementations','ArrayList vs LinkedList vs Vector vs Stack','Internals and when to use each.',FALSE,'Top-asked comparison','ALL'),
 ('E','Generics and Collections', 5,'queue-deque','Queue, Deque, ArrayDeque','FIFO, double-ended queues, why ArrayDeque beats Stack.',FALSE,'Used in BFS and stack problems','ALL'),
 ('E','Generics and Collections', 6,'hashmap-internals','HashMap internals','Hashing, buckets, collisions, treeification, resize.',FALSE,'Very common deep-dive question','ALL'),
 ('E','Generics and Collections', 7,'sets-linked-tree','HashSet, LinkedHashMap, TreeMap, TreeSet','Ordering guarantees and how they are built.',FALSE,'Common comparison question','ALL'),
 ('E','Generics and Collections', 8,'priorityqueue','PriorityQueue','Heap-based queue for top-K problems.',FALSE,'Important for SP DSA','INFOSYS_SP,PRODUCT,TCS'),
 ('E','Generics and Collections', 9,'concurrent-collections','ConcurrentHashMap, fail-fast vs fail-safe','Safe iteration and concurrent maps.',FALSE,'Depth topic','INFOSYS_SP,PRODUCT,TCS'),
 ('E','Generics and Collections',10,'iterator-comparators','Iterator, Comparable vs Comparator','Custom sorting and safe removal while iterating.',FALSE,'Top-asked comparison','ALL'),
 ('E','Generics and Collections',11,'collections-utility-complexity','Collections utility class and Big-O cheat sheet','sort, reverse, unmodifiable lists; time complexity of each structure.',FALSE,'Needed for optimization talk','ALL'),
 -- F. Java 8+
 ('F','Java 8+', 1,'lambdas-functional-interfaces','Lambdas and functional interfaces','Passing behaviour as data; Predicate, Function, Consumer, Supplier.',FALSE,'Frequently asked modern Java','ALL'),
 ('F','Java 8+', 2,'method-references','Method references','Shorter lambdas with ::.',FALSE,'Modern Java knowledge','ALL'),
 ('F','Java 8+', 3,'streams-api','Stream API','map, filter, reduce, collect, groupingBy, flatMap.',FALSE,'Frequently asked; coding with streams','ALL'),
 ('F','Java 8+', 4,'optional','Optional','Avoiding null checks safely.',FALSE,'Modern Java knowledge','ALL'),
 ('F','Java 8+', 5,'date-time-api','Date/Time API','LocalDate, Instant, ZonedDateTime, Duration.',FALSE,'Practical knowledge','INFOSYS_SP,INFOSYS_DSE,PRODUCT'),
 ('F','Java 8+', 6,'modern-java-features','var, switch expressions, records, sealed classes, text blocks','Newer Java features in plain words.',FALSE,'Shows you are up to date','INFOSYS_SP,PRODUCT'),
 -- G. Multithreading
 ('G','Multithreading', 1,'process-thread-lifecycle','Process vs thread, thread lifecycle','NEW, RUNNABLE, BLOCKED, WAITING, TERMINATED.',FALSE,'Common theory question','ALL'),
 ('G','Multithreading', 2,'creating-threads','Thread vs Runnable vs Callable','Three ways to run work in parallel.',FALSE,'Common comparison question','ALL'),
 ('G','Multithreading', 3,'synchronization','synchronized, volatile, wait/notify','Race conditions and how to prevent them.',FALSE,'Depth topic for SP','INFOSYS_SP,INFOSYS_DSE,PRODUCT,TCS'),
 ('G','Multithreading', 4,'deadlock','Deadlock','How it happens and how to avoid it.',FALSE,'Common theory question','ALL'),
 ('G','Multithreading', 5,'executors-completablefuture','Thread pools, ExecutorService, CompletableFuture','Reusing threads and async pipelines.',FALSE,'Depth topic','INFOSYS_SP,PRODUCT'),
 ('G','Multithreading', 6,'atomic-locks','Atomic classes and locks','AtomicInteger, ReentrantLock, ReadWriteLock.',FALSE,'Depth topic','INFOSYS_SP,PRODUCT'),
 ('G','Multithreading', 7,'concurrency-questions','Concurrency interview questions','Practice questions in simple language.',FALSE,'Interview practice','INFOSYS_SP,PRODUCT,TCS'),
 -- H. JVM and memory
 ('H','JVM and Memory', 1,'stack-heap','Stack vs heap','Where variables and objects live; pass-by-value visual.',FALSE,'Common theory question','ALL'),
 ('H','JVM and Memory', 2,'class-loading','Class loading','Bootstrap, platform and application class loaders.',FALSE,'Depth topic','INFOSYS_SP,PRODUCT'),
 ('H','JVM and Memory', 3,'garbage-collection','Garbage collection basics','How unused objects are cleaned up; generations.',FALSE,'Common theory question','ALL'),
 ('H','JVM and Memory', 4,'memory-leaks-references','Memory leaks and reference types','Strong, weak, soft, phantom references.',FALSE,'Depth topic','INFOSYS_SP,PRODUCT'),
 -- I. IO and misc
 ('I','IO and Design', 1,'file-io','File IO','Reading and writing files with java.io and java.nio.',FALSE,'Practical knowledge','INFOSYS_DSE,TCS,WIPRO,COGNIZANT'),
 ('I','IO and Design', 2,'serialization','Serialization','Turning objects into bytes; transient and serialVersionUID.',FALSE,'Common theory question','ALL'),
 ('I','IO and Design', 3,'jdbc-basics','JDBC basics','Connecting Java to a database; PreparedStatement.',FALSE,'Connects to SQL and Spring','INFOSYS_DSE,TCS,WIPRO,COGNIZANT,ACCENTURE,CAPGEMINI'),
 ('I','IO and Design', 4,'reflection-annotations','Reflection and annotations','How Spring reads your @Annotations at runtime.',FALSE,'Bridge to Spring Boot','INFOSYS_SP,INFOSYS_DSE,PRODUCT'),
 ('I','IO and Design', 5,'design-patterns','Design basics: Singleton, Factory, Builder, Observer','Four patterns explained with everyday analogies.',FALSE,'Common design question','ALL'),
 ('I','IO and Design', 6,'solid','SOLID in simple language','Five rules for code that is easy to change.',FALSE,'Common design question','ALL'),
 -- J. Interview layer
 ('J','Interview Layer', 1,'java-interview-150','Top 150 Java practice questions','Practice questions in simple language, labeled by topic.',FALSE,'Interview practice (practice questions, not actual test questions)','ALL'),
 ('J','Interview Layer', 2,'predict-output-puzzles','Predict-the-output puzzles','Tricky snippets to sharpen your mental compiler.',FALSE,'Interview practice','ALL'),
 ('J','Interview Layer', 3,'spot-the-bug','Spot-the-bug challenges','Find the bug before the compiler or tester does.',FALSE,'Interview practice','ALL')
),
ins AS (
    INSERT INTO java_topics (section, section_name, sort_order, slug, title, summary, rebuild, infosys_relevance)
    SELECT section, section_name, sort_order, slug, title, summary, rebuild, infosys_relevance FROM t
    RETURNING id, slug
)
INSERT INTO java_topic_companies (topic_id, company_code)
SELECT ins.id, trim(code)
FROM ins
JOIN t ON t.slug = ins.slug
CROSS JOIN LATERAL unnest(string_to_array(t.tags, ',')) AS code;

