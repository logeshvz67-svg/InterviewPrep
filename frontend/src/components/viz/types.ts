/** Shared types for the step-by-step visualizers. */

/** Color "mood" of a box in a drawing. Mapped to theme colors in draw.tsx. */
export type Tone = 'normal' | 'active' | 'good' | 'bad' | 'done' | 'muted' | 'gold' | 'empty'

/** Color of a pointer label (L, R, i, curr, top...). */
export type PointerTone = 'primary' | 'accent' | 'gold' | 'good' | 'bad'

/** A multiple-choice "what happens next?" question, used by Predict mode. */
export interface Prediction {
  question: string
  options: string[]
  /** Index into `options` of the right answer. */
  answer: number
  /** Short reason shown after the learner answers. */
  explain?: string
}

/**
 * Every visualizer turns its input into a list of steps.
 * A step = a full picture of the data at one moment + a caption.
 * `predict` (optional) is a question about THIS step, asked while the learner is still on the step before it.
 * `line` (optional) highlights a line of the code panel.
 */
export interface BaseStep {
  caption: string
  predict?: Prediction
  line?: number
}

/** A label drawn under a box in an array drawing, e.g. "L" under index 0. */
export interface Pointer {
  index: number
  label: string
  tone?: PointerTone
}

export interface VizInfo {
  id: string
  title: string
  description: string
  icon: string
  /** Slug of the related lesson (link goes to /lesson/<slug>), or null when there is none. */
  lessonSlug?: string | null
}
