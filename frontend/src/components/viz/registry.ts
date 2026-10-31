import type { VizInfo } from './types'

/** All step-by-step visualizers. The ids are used by lessons (<Visualizer id="..." />) and by the URL /visualizers/:id. */
export const VISUALIZERS: VizInfo[] = [
  {
    id: 'array', icon: '📦', title: 'Array operations', lessonSlug: 'arrays-1d-2d',
    description: 'Visit every box, insert (values shift right), delete (values shift left) and linear search.',
  },
  {
    id: 'two-pointers', icon: '👉👈', title: 'Two pointers', lessonSlug: 'two-pointers',
    description: 'Find a pair with a target sum in a sorted array, and reverse an array in place.',
  },
  {
    id: 'sliding-window', icon: '🪟', title: 'Sliding window', lessonSlug: 'sliding-window',
    description: 'Max sum of k boxes in a row, and the longest text part with no repeated letter.',
  },
  {
    id: 'linked-list', icon: '🔗', title: 'Linked list', lessonSlug: null,
    description: 'Insert at head or position, delete a node, and reverse with prev / curr / next.',
  },
  {
    id: 'stack-queue', icon: '🥞', title: 'Stack and queue', lessonSlug: 'queue-deque',
    description: 'push / pop / peek, enqueue / dequeue, and checking balanced brackets with a stack.',
  },
  {
    id: 'recursion', icon: '🌳', title: 'Recursion', lessonSlug: 'recursion-basics',
    description: 'See the call stack and the call tree for factorial(n) and fibonacci(n).',
  },
]

export function findVisualizer(id: string): VizInfo | undefined {
  return VISUALIZERS.find((v) => v.id === id)
}
