// Shapes for Phase 2-3 endpoints (mirror the Java DTO records).
import type { XpAward } from './types'

export interface WorldSummary { code: string; name: string; description: string; sortOrder: number; lessonsWritten: number; lessonsCompleted: number; outlineCount: number }
export interface LessonCard {
  slug: string | null; title: string; section: string; order: number; estimatedMinutes: number
  status: 'LOCKED' | 'AVAILABLE' | 'IN_PROGRESS' | 'COMPLETED' | 'OUTLINE'
  bestQuizPercent: number; mastered: boolean; companies: string[]; summary: string; relevant: boolean
}
export interface SectionGroup { section: string; name: string; total: number; completed: number; lessons: LessonCard[] }
export interface WorldDetail { code: string; name: string; description: string; unlockPercent: number; sections: SectionGroup[] }

export type QuestionType = 'MCQ' | 'TRUE_FALSE' | 'PREDICT_OUTPUT' | 'SPOT_THE_BUG' | 'FILL_THE_CODE' | 'MATCH'
export interface QuestionView { id: string; type: QuestionType; prompt: string; code: string | null; options: unknown }
export interface QuestionResult { id: string; correct: boolean; given: unknown; correctAnswer: string; explanation: string }
export interface QuizResult {
  kind: string; correct: number; total: number; percent: number; passed: boolean; passPercent: number
  results: QuestionResult[]; xp: XpAward[]; lessonCompleted: boolean; unlockedNextSlug: string | null; message: string
}

export interface LessonContent {
  slug: string; title: string; why: string; analogy: string
  visual: { diagram: string; caption: string; visualizer: string | null }
  explanation: string
  tinyCode: { code: string; walkthrough: string }
  tryIt: { instructions: string; code: string }
  predict: { code: string; expectedOutput: string; explanation: string }
  commonMistakes: string[]
  interviewBox: { why: string; questions: { q: string; a: string }[] }
  explainBack: { prompt: string; rubric: string[]; modelAnswer: string }
  flashcards: { front: string; back: string }[]
  keyTakeaways: string[]
  infosysRelevance: string; otherCompanies: string; rebuild: boolean
}
export interface LessonView {
  slug: string; title: string; world: string; section: string; order: number; estimatedMinutes: number
  locked: boolean; lockReason: string | null; content: LessonContent | null; quiz: QuestionView[]
  stepsDone: string[]; status: string; bestQuizPercent: number; quizPassed: boolean; topicStatus: string | null
  explainBack: string | null; prevSlug: string | null; nextSlug: string | null; companies: string[]
}
export interface ExplainBackResult { score: number; feedback: string; modelAnswer: string; aiUsed: boolean; xp: XpAward; topicStatus: string | null }

export interface FlashcardView { id: number; front: string; back: string; topic: string | null; sourceType: string; intervalDays: number; repetitions: number; nextReviewOn: string; suspended: boolean }
export interface ReviewResult { card: FlashcardView; reviewedToday: number; dueLeft: number }
export interface WarmupView { cards: FlashcardView[]; questions: QuestionView[]; questionLesson: Record<string, string>; dueTotal: number; reviewedToday: number; rewardedToday: boolean }
export interface RecallComplete { xp: XpAward; reviewedToday: number; message: string }

export interface BankProblem { id: number; slug: string; title: string; url: string; platform: string; difficulty: string; patternCode: string; patternName: string; companies: string[]; why: string; solved: boolean }
export interface Suggestion { problem: BankProblem; reason: string }
export interface PatternCoverage { code: string; name: string; solved: number; total: number }

export interface QuestBoard { daily: import('./types').QuestView[]; weekly: import('./types').QuestView[]; story: import('./types').QuestView[]; weekStart: string }
export interface Badge { code: string; name: string; description: string; icon: string; unlockedAt: string | null }

// AI mentor
export interface MessageView { id: number; role: 'user' | 'assistant'; content: string; fallback: boolean; createdAt: string }
export interface ChatResponse { conversationId: number; reply: MessageView; aiUsed: boolean; notice: string | null }
export interface ConversationView { id: number; mode: string; title: string; topic: string | null; resolved: boolean; hintLevel: number; updatedAt: string; messages: MessageView[] }
export interface HintResponse { level: number; levelName: string; hint: string; aiUsed: boolean; notice: string | null }
export interface GeneratedQuestion { prompt: string; code: string | null; options: string[]; answer: number; explanation: string }
export interface QuizGenResponse { topic: string; questions: GeneratedQuestion[]; aiUsed: boolean; source: string; notice: string | null }
export interface BriefingResponse { kind: string; text: string; aiUsed: boolean }
export interface Diagnostics {
  enabled: boolean; usable: boolean; primaryModel: string | null; fallbackModels: string[]; dailyLimitPerUser: number
  myUsageToday: number; allUsageToday: number; cacheEntries: number; lastError: string | null; lastErrorAt: string | null
  recentCalls: { feature: string; model: string; success: boolean; http_status: number; latency_ms: number; error: string | null; created_at: string }[]
}
export interface TestResult { ok: boolean; model: string | null; latencyMs: number; reply: string | null; error: string | null }
