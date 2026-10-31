import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from 'react'
import { Spinner } from '../ui'

/**
 * Show one visualizer by id, e.g. <Visualizer id="two-pointers" />.
 * Each visualizer is loaded only when needed (smaller first page load).
 */
const COMPONENTS: Record<string, LazyExoticComponent<ComponentType>> = {
  'array': lazy(() => import('./ArrayViz')),
  'two-pointers': lazy(() => import('./TwoPointersViz')),
  'sliding-window': lazy(() => import('./SlidingWindowViz')),
  'linked-list': lazy(() => import('./LinkedListViz')),
  'stack-queue': lazy(() => import('./StackQueueViz')),
  'recursion': lazy(() => import('./RecursionViz')),
}

export function Visualizer({ id }: { id: string }) {
  const Component = COMPONENTS[id]
  if (!Component) {
    return (
      <div className="card text-muted" role="note">
        We could not find the visualizer "<span className="font-mono">{id}</span>". It may have a different name.
      </div>
    )
  }
  return (
    <Suspense fallback={<Spinner label="Loading the visualizer…" />}>
      <Component />
    </Suspense>
  )
}
