// Shapes for the Interview Colosseum + Spring Track (mirror InterviewDtos / SpringTrackService records).
import type { XpAward } from '../../api/types'

export type { XpAward }

// ---- readiness
export type ReadinessComponent = 'JAVA' | 'DSA' | 'SQL' | 'APTITUDE' | 'MOCKS' | 'COMMUNICATION'
export interface ComponentScore { component: ReadinessComponent; weight: number; value: number; tracked: boolean }
export interface ReadinessView {
  companyCode: string; score: number; trackedWeightPercent: number
  components: ComponentScore[]; improveNext: ReadinessComponent; improveNextTip: string
}

// ---- question bank
export interface BankQuestion {
  id: number; extId: string; category: string; topic: string; difficulty: string; companies: string[]
  question: string; answer: string; keyPoints: string[]; myBestScore: number | null
}
export interface PracticeResult { score: number; practisedToday: number; xp: XpAward }

// ---- aptitude
export interface AptitudeItem { id: number; section: string; topic: string; difficulty: string; question: string; options: string[] }
export interface AptitudeAnswer { questionId: number; chosen: number | null; seconds: number }
export interface AptitudeResult { questionId: number; correct: boolean; chosen: number | null; answer: number; explanation: string }
export interface AptitudeGraded { correct: number; total: number; percent: number; results: AptitudeResult[]; xp: XpAward }
export interface TopicAccuracy { section: string; topic: string; attempts: number; correct: number }

// ---- exam patterns
export type SectionKind = 'APTITUDE' | 'TECH_MCQ' | 'CODING'
export interface PatternSection { name: string; kind: SectionKind; questions: number; minutes: number; cutoffPercent: number | null }
export interface ExamPatternRequest {
  companyCode: string; name: string; sections: PatternSection[]; sourceNote: string | null; verifiedOn: string | null
}
export interface ExamPatternView {
  id: number; companyCode: string; name: string; sections: PatternSection[]; sourceNote: string | null; verifiedOn: string | null; totalMinutes: number
}

// ---- mock tests
export interface AptitudeMockItem { key: string; id: number; section: string; topic: string; difficulty: string; question: string; options: string[]; answer?: number; explanation?: string }
export interface TechMockItem { key: string; type: string; prompt: string; code: string | null; options: string[] | null; answer?: number; explanation?: string }
export interface CodingMockItem { key: string; title: string; url: string | null; difficulty: string; pattern: string | null; slug: string | null }
export type MockItem = AptitudeMockItem | TechMockItem | CodingMockItem

export interface MockSection { name: string; kind: SectionKind; minutes: number; cutoffPercent: number | null; items: MockItem[] }
export interface SectionScore { name: string; kind: SectionKind; score: number; total: number; percent: number; passedCutoff?: boolean; note?: string }
export type CodingStatus = 'SOLVED' | 'PARTIAL' | 'NOT_ATTEMPTED'
export interface CodingAnswer { status: CodingStatus; minutes: number }
export type MockAnswer = number | CodingAnswer
export interface MockResult { scorePercent: number; late: boolean; sections: SectionScore[]; answers: Record<string, MockAnswer> }
export interface MockTestView {
  id: number; title: string; companyCode: string; status: 'RUNNING' | 'SUBMITTED' | 'EXPIRED'
  startedAt: string; endsAt: string; serverNow: string; totalMinutes: number; sections: MockSection[]; result: MockResult | null
}
export interface MockSummary { id: number; title: string; companyCode: string; status: string; startedAt: string; score: number | null }

// ---- diary
export interface DiaryRequest {
  company: string; round: string; interviewDate: string; question: string; myAnswer: string; betterAnswer: string; tags: string
}
export interface DiaryView extends Omit<DiaryRequest, 'myAnswer' | 'betterAnswer' | 'tags'> {
  id: number; myAnswer: string | null; betterAnswer: string | null; tags: string | null; createdAt: string
}

// ---- Spring track
export type SpringStatus = 'NOT_STARTED' | 'LEARNING' | 'UNDERSTOOD' | 'CAN_EXPLAIN'
export interface TopicCard {
  id: number; code: string; title: string; group: string; order: number; summary: string; status: SpringStatus
  checklistDone: number; checklistTotal: number; bestQuizPercent: number
}
export interface QuizQuestion { prompt: string; options: string[] }
export interface TopicDetail {
  card: TopicCard; checklist: string[]; checklistDone: number[]; quiz: QuizQuestion[]
  explainPrompt: string; rubric: string[]; explainBack: string | null
}
export interface QuizResultItem { correct: boolean; answer: number; explanation: string }
export interface QuizOutcome { correct: number; total: number; percent: number; passed: boolean; results: QuizResultItem[]; xp: XpAward; status: SpringStatus }
export interface ExplainOutcome { score: number; feedback: string; aiUsed: boolean; xp: XpAward; status: SpringStatus }

export interface McqQuiz { prompt: string; options: string[]; answer: number; explanation: string }
export interface AnnotationCard { name: string; category: string; meaning: string; analogy: string; snippet: string; quiz?: McqQuiz }
export interface DebugChallenge {
  id: string; title: string; error: string; code: string; question: string; options: string[]; answer: number; explanation: string; fix: string
}

export type ProjectStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE'
export interface MilestoneView { id: number; title: string; order: number; done: boolean }
export type LogType = 'LOG' | 'BLOCKER' | 'LEARNED'
export interface BuildLog { id: number; entry_type: LogType; text: string; resolved: boolean; local_date: string | number; created_at: string | number }
export interface ProjectView {
  id: number; code: string; title: string; description: string; githubUrl: string | null; status: ProjectStatus
  milestones: MilestoneView[]; logs: BuildLog[]
}
