/**
 * Colors point at CSS variables. The actual values live in src/theme/gameConfig.ts,
 * so restyling the whole app = editing that ONE file.
 */
const v = (name) => `rgb(var(--c-${name}) / <alpha-value>)`

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        bg: v('bg'), surface: v('surface'), raised: v('raised'), line: v('line'),
        ink: v('ink'), muted: v('muted'),
        primary: v('primary'), accent: v('accent'), gold: v('gold'),
        good: v('good'), warn: v('warn'), bad: v('bad'),
      },
      fontFamily: {
        sans: ['var(--font-body)'],
        display: ['var(--font-display)'],
        mono: ['var(--font-mono)'],
      },
      borderRadius: { card: 'var(--radius)' },
    },
  },
  plugins: [],
}
