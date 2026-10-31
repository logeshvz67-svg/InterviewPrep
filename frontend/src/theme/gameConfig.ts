/**
 * ============================================================
 *  THE ONE FILE TO RESTYLE THE GAME.
 *  Colors, fonts, labels, rank names, world names, avatars.
 *  Colors are "R G B" strings (so Tailwind can add opacity).
 * ============================================================
 */

export type ThemeName = 'quest-dark' | 'parchment-light'

export interface Palette {
  bg: string; surface: string; raised: string; line: string
  ink: string; muted: string
  primary: string; accent: string; gold: string
  good: string; warn: string; bad: string
}

export const themes: Record<ThemeName, { label: string; dark: boolean; palette: Palette }> = {
  'quest-dark': {
    label: 'Night Quest (dark)',
    dark: true,
    palette: {
      bg: '15 18 34', surface: '24 28 50', raised: '34 39 68', line: '52 58 96',
      ink: '236 238 250', muted: '158 164 199',
      primary: '139 123 255', accent: '56 214 196', gold: '255 204 77',
      good: '74 222 128', warn: '251 191 36', bad: '248 113 113',
    },
  },
  'parchment-light': {
    label: 'Parchment (light)',
    dark: false,
    palette: {
      bg: '248 245 238', surface: '255 255 255', raised: '241 236 225', line: '222 214 196',
      ink: '33 31 46', muted: '104 100 122',
      primary: '98 76 230', accent: '13 148 136', gold: '202 138 4',
      good: '22 163 74', warn: '217 119 6', bad: '220 38 38',
    },
  },
}

export const fonts = {
  body: "'Inter', system-ui, sans-serif",
  display: "'Space Grotesk', 'Inter', sans-serif",
  mono: "'JetBrains Mono', ui-monospace, monospace",
}

export const radius = '14px'

export const game = {
  title: 'Code Quest',
  tagline: 'Your road to the Offer Letter Castle',
  finalDestination: 'Offer Letter Castle',
  currency: 'gold',
  xpLabel: 'XP',
}

/** Server sends rank codes; the names and icons shown are decided here. */
export const ranks: Record<string, { name: string; icon: string }> = {
  INTERN: { name: 'Intern', icon: '🌱' },
  TRAINEE: { name: 'Trainee', icon: '🛡️' },
  DEVELOPER: { name: 'Developer', icon: '⚔️' },
  SENIOR_DEV: { name: 'Senior Dev', icon: '🔮' },
  TECH_LEAD: { name: 'Tech Lead', icon: '🦅' },
  ARCHITECT: { name: 'Architect', icon: '🏰' },
}

export const worlds: Record<string, { icon: string }> = {
  JAVA_CORE: { icon: '🏝️' },
  DSA: { icon: '👑' },
  SQL: { icon: '🕳️' },
  CS_CORE: { icon: '🛕' },
  SPRING: { icon: '🔥' },
  INTERVIEW: { icon: '🏟️' },
  APTITUDE: { icon: '🎯' },
  SYSTEM_DESIGN: { icon: '🗼' },
}

export const avatars = [
  { id: 'knight', icon: '🛡️', label: 'Knight' },
  { id: 'mage', icon: '🧙', label: 'Mage' },
  { id: 'ranger', icon: '🏹', label: 'Ranger' },
  { id: 'robot', icon: '🤖', label: 'Robot' },
  { id: 'fox', icon: '🦊', label: 'Fox' },
  { id: 'owl', icon: '🦉', label: 'Owl' },
]

/** Plain-English labels so there is no jargon on screen. */
export const labels = {
  questBoard: "Today's quests",
  streak: 'Streak',
  freezes: 'Streak freezes',
  recallQueue: 'Recall queue',
  readiness: 'Readiness',
  mvd: 'Minimum Viable Day (20 min)',
  doThisNow: 'Do this now',
  gym: 'Problem Gym',
  mistakes: 'Mistake Notebook',
  focus: 'Focus',
  curriculum: 'Java Core Island',
  roadmap: 'Roadmap',
  settings: 'Settings',
}

export const topicStatusLabels: Record<string, { label: string; tone: string }> = {
  NOT_STARTED: { label: 'Not started', tone: 'text-muted' },
  LEARNING: { label: 'Learning', tone: 'text-warn' },
  UNDERSTOOD: { label: 'Understood', tone: 'text-accent' },
  CAN_EXPLAIN: { label: 'Can explain', tone: 'text-primary' },
  MASTERED: { label: 'Mastered', tone: 'text-gold' },
}

export const streakStatusText: Record<string, string> = {
  NOT_STARTED: 'Start today',
  DONE_TODAY: 'Done for today ✓',
  KEEP_IT_GOING: 'Keep it going today',
  FREEZE_WILL_COVER: 'A freeze will cover yesterday',
  PAUSED: 'Paused. Start fresh anytime',
}

export function applyTheme(name: ThemeName) {
  const t = themes[name] ?? themes['quest-dark']
  const root = document.documentElement
  for (const [k, val] of Object.entries(t.palette)) root.style.setProperty(`--c-${k}`, val)
  root.style.setProperty('--font-body', fonts.body)
  root.style.setProperty('--font-display', fonts.display)
  root.style.setProperty('--font-mono', fonts.mono)
  root.style.setProperty('--radius', radius)
  root.classList.toggle('dark', t.dark)
  root.style.colorScheme = t.dark ? 'dark' : 'light'
}
