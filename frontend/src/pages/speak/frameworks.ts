/** The three speaking frameworks taught in the Communication Gym. */
export interface FrameworkStep { name: string; hint: string; example: string }
export interface FrameworkDef { code: 'STAR' | 'PPF' | 'CEU'; title: string; icon: string; useFor: string; steps: FrameworkStep[] }

export const FRAMEWORKS: FrameworkDef[] = [
  {
    code: 'STAR', title: 'STAR', icon: '⭐',
    useFor: 'Stories about your experience: "Tell me about a time when…", conflicts, mistakes, teamwork.',
    steps: [
      { name: 'Situation', hint: 'Where and when? Set the scene in one sentence.', example: 'At my data-entry job, our team had to clean 5,000 customer records in one week.' },
      { name: 'Task', hint: 'What was YOUR job or goal?', example: 'My task was to find and fix duplicate records before the deadline.' },
      { name: 'Action', hint: 'What did YOU do? Use "I", not "we". 2-3 steps.', example: 'I sorted the sheet by phone number, wrote a simple Excel formula to flag duplicates, and checked each flagged row.' },
      { name: 'Result', hint: 'What happened? A number if possible. What did you learn?', example: 'We finished one day early with zero duplicates, and I learned that a small tool can save hours.' },
    ],
  },
  {
    code: 'PPF', title: 'Present → Past → Future', icon: '🧭',
    useFor: '"Tell me about yourself", "Walk me through your resume", "Why this role?"',
    steps: [
      { name: 'Present', hint: 'Who are you right now? What do you do?', example: 'Right now I work as a data-entry associate, and I spend my evenings learning Java.' },
      { name: 'Past', hint: 'How did you get here? 1-2 proof points.', example: 'Over the last year I studied OOP, collections and SQL, and built a small Spring Boot REST API.' },
      { name: 'Future', hint: 'What do you want next, and why this role?', example: 'Next, I want to start my career as a developer, and this role lets me learn from a strong team.' },
    ],
  },
  {
    code: 'CEU', title: 'Concept → Example → Use case', icon: '💡',
    useFor: 'Technical questions: "What is a HashMap?", "Explain polymorphism."',
    steps: [
      { name: 'Concept', hint: 'What is it? One simple sentence, no jargon.', example: 'A HashMap stores data as key-value pairs, so you can find a value quickly by its key.' },
      { name: 'Example', hint: 'A tiny, concrete example.', example: 'For example, I can store student roll numbers as keys and their names as values.' },
      { name: 'Use case', hint: 'Where is it used in real projects? Why is it useful?', example: 'It is used for caches and lookups, because getting a value by key is very fast on average.' },
    ],
  },
]

export const frameworkByCode = (code: string | null | undefined) => FRAMEWORKS.find((f) => f.code === code) ?? null
