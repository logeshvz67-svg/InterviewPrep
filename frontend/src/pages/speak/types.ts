import type { XpAward } from '../../api/types'

// ------------------------------------------------------------------ speaking (mirrors SpeakDtos.java)

export type SpeakMode = 'PRACTICE' | 'EXPLAIN_ALOUD'
export type InputMethod = 'VOICE' | 'TYPED'
export type Framework = 'STAR' | 'PPF' | 'CEU' | 'NONE'

export interface PromptView {
  id: number; category: string; prompt: string; framework: Framework | string
  timeLimitSeconds: number; tips: string[] | null; sampleAnswer: string | null
}

export interface SessionRequest {
  promptId: number | null; mode: SpeakMode; promptText: string; transcript: string
  durationSeconds: number; inputMethod: InputMethod; longPauses: number; useAi: boolean
}

export interface SessionView {
  id: number; mode: SpeakMode; promptText: string; transcript: string; inputMethod: InputMethod; durationSeconds: number
  wordCount: number; wpm: number; fillerCount: number; fillers: Record<string, number> | null; longPauses: number
  scores: Record<string, number> | null; overallScore: number | null; feedback: string | null; improvedAnswer: string | null
  followUpQuestion: string | null; aiUsed: boolean; localDate: string; createdAt: string
}

export interface SessionSaved { session: SessionView; xp: XpAward | null; notice: string | null }

export interface WeeklyTrend {
  week: string | number; sessions: number; wpm: number | null; fillers: number | null; score: number | null; confidence: number | null
}
export interface Trends { speakingStreak: number; sessionsTotal: number; weekly: WeeklyTrend[] }

export interface Phrase { phrase: string; useWhen?: string }
export interface VocabWord { word: string; meaning: string; example?: string }
export interface PronunciationTerm { term: string; sayItLike: string; note?: string }
export interface Library { phrases: Phrase[]; vocabulary: VocabWord[]; pronunciation: PronunciationTerm[] }

// ------------------------------------------------------------------ mock interview

export type RoundType = 'HR' | 'TECHNICAL' | 'SPRING'

export interface InterviewTurn {
  interviewId: number; interviewerText: string; questionNumber: number; totalQuestions: number
  finished: boolean; aiUsed: boolean; notice: string | null
}

export interface TranscriptEntry { role: 'interviewer' | 'candidate'; text: string; questionId?: number; followUp?: boolean; seconds?: number }

export interface ScorecardQuestion { question: string; score: number; answered: boolean; modelAnswer: string | null; missingPoints: string[] }
export interface AiScorecard {
  overall?: number; communication?: number; technicalDepth?: number; confidence?: number
  strengths?: string[]; improve?: string[]; summary?: string
}
export interface Scorecard { questions: ScorecardQuestion[]; method?: string; ai?: AiScorecard }

export interface InterviewView {
  id: number; roundType: RoundType; status: 'RUNNING' | 'COMPLETED' | string
  transcript: TranscriptEntry[] | null; scorecard: Scorecard | null; overallScore: number | null
  aiUsed: boolean; startedAt: string; endedAt: string | null
}

// ------------------------------------------------------------------ Web Speech API (not in TS DOM lib everywhere)

export interface SRAlternative { transcript: string }
export interface SRResult { isFinal: boolean; length: number; [index: number]: SRAlternative }
export interface SRResultList { length: number; [index: number]: SRResult }
export interface SREvent { resultIndex: number; results: SRResultList }
export interface SRErrorEvent { error: string }
export interface SRInstance {
  lang: string; continuous: boolean; interimResults: boolean; maxAlternatives: number
  onresult: ((e: SREvent) => void) | null
  onerror: ((e: SRErrorEvent) => void) | null
  onend: (() => void) | null
  onstart: (() => void) | null
  start(): void; stop(): void; abort(): void
}
export type SRConstructor = new () => SRInstance
