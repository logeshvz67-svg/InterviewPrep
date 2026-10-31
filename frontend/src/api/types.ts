// Shapes returned by the backend (mirror the Java DTO records).

export interface UserView { id: number; email: string; displayName: string; role: 'USER' | 'ADMIN' }
export interface TokenResponse { accessToken: string; refreshToken: string; expiresInSeconds: number; user: UserView }

export interface ProfileView {
  characterName: string; avatar: string; primaryCompany: string; laterCompanies: string[]
  testDate: string | null; timezone: string; dailyMinutes: number; whyStatement: string | null
  selfAssessment: Record<string, number>; onboardingComplete: boolean; totalXp: number; gold: number; soundEnabled: boolean
}

export interface RoadmapWeek { week: number; startDate: string; phase: string; title: string; focus: string[]; laterLane: boolean }
export interface RoadmapView { startDate: string; endDate: string; totalWeeks: number; currentWeek: number; weeks: RoadmapWeek[] }

export interface LevelView {
  totalXp: number; level: number; levelStartXp: number; nextLevelXp: number; levelProgressPercent: number
  rank: string; nextRank: string | null; nextRankXp: number | null; gold: number
}
export type StreakStatus = 'NOT_STARTED' | 'DONE_TODAY' | 'KEEP_IT_GOING' | 'FREEZE_WILL_COVER' | 'PAUSED'
export interface StreakView {
  current: number; longest: number; freezes: number; maxFreezes: number; lastActiveDate: string | null
  status: StreakStatus; comebackPending: boolean; daysAway: number
}
export interface QuestView { id: number; code: string; title: string; description: string; progress: number; target: number; goldReward: number; completed: boolean }
export interface XpAward { awarded: boolean; xp: number; gold: number; reason: string | null }
export interface XpEventView { action: string; amount: number; gold: number; note: string | null; localDate: string; createdAt: string }

export interface FocusView {
  id: number; mode: 'POMODORO' | 'MVD'; topic: string; plannedMinutes: number; startedAt: string; endsAt: string
  status: 'RUNNING' | 'COMPLETED' | 'CANCELLED'; actualMinutes: number | null; serverNow: string
}
export interface FocusCompleted { session: FocusView; xp: XpAward; streakCounted: boolean }
export interface FocusStats { days: number; totalMinutes: number; minutesByTopic: Record<string, number>; minutesByDay: Record<string, number> }
export interface NextAction { type: string; title: string; detail: string; route: string }
export interface CommitmentTask { text: string; done: boolean }
export interface CommitmentView { forDate: string; tasks: CommitmentTask[] }
export interface DistractionInsights {
  days: number; total: number; byCategory: Record<string, number>; byHour: Record<string, number>
  topCategory: string | null; worstHour: number | null; tip: string
}

export interface ComponentScore { component: string; weight: number; value: number; tracked: boolean }
export interface ReadinessView { companyCode: string; score: number; trackedWeightPercent: number; components: ComponentScore[]; improveNext: string; improveNextTip: string }
export interface RadarPoint { area: string; selfRated: number | null; measured: number | null }

export interface CommandCenter {
  characterName: string; avatar: string; whyStatement: string | null
  level: LevelView; streak: StreakView; todayQuests: QuestView[]
  recallQueueCount: number; daysToTest: number | null; radar: RadarPoint[]; readiness: ReadinessView
  activeFocus: FocusView | null; nextAction: NextAction; todayXp: number; currentWeek: number
  currentWeekTitle: string | null; welcomeMessage: string
}

export interface PatternView { code: string; name: string; description: string; spCore: boolean }
export interface ProblemView {
  id: number; title: string; link: string | null; platform: string; difficulty: string; patterns: PatternView[]
  timeTakenMinutes: number; attempts: number; hintLevel: string; attemptedBeforeHint: boolean; confidence: number
  approach: string; code: string | null; solvedOn: string; createdAt: string
}
export interface ProblemSaved { problem: ProblemView; approachXp: XpAward; attemptXp: XpAward; revisitDates: string[] }
export interface PatternStat { code: string; name: string; spCore: boolean; solved: number; avgConfidence: number; unaidedRate: number; strength: string }
export interface PatternMap { patterns: PatternStat[]; recommendedNext: PatternView | null; recommendationReason: string }
export interface RevisitView { id: number; problemId: number; problemTitle: string; link: string | null; round: number; dueDate: string; overdue: boolean }
export interface MistakeView { id: number; categoryCode: string; categoryName: string; problemId: number | null; topic: string | null; whatHappened: string; lessonLearned: string; resolved: boolean; createdAt: string }
export interface MistakeCategoryView { code: string; name: string; tip: string }
export interface MistakeTrend { categoryCode: string; categoryName: string; count: number; tip: string }

export interface Page<T> { items: T[]; page: number; size: number; totalItems: number; totalPages: number }

export interface TopicView {
  id: number; section: string; sortOrder: number; slug: string; title: string; summary: string; rebuild: boolean
  infosysRelevance: string; companies: string[]; status: string; masteryScore: number; lastReviewed: string | null
}
export interface SectionView { section: string; name: string; total: number; understoodOrBetter: number; topics: TopicView[] }
export interface CurriculumView { companyFilter: string | null; totalTopics: number; understoodOrBetter: number; sections: SectionView[] }
export interface CompanyView { code: string; name: string }
export interface WorldView { code: string; name: string; description: string; sortOrder: number; unlockQuizPercent: number }
