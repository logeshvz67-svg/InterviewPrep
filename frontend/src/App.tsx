import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { Spinner } from './components/ui'
import { WakeGate } from './components/WakeGate'
import { useApi } from './lib/hooks'
import { useReminder } from './lib/reminder'
import type { ProfileView } from './api/types'
import AuthPage from './pages/AuthPage'
import OnboardingPage from './pages/OnboardingPage'
import CommandCenterPage from './pages/CommandCenterPage'

// Pages load on demand (code splitting) so the first screen stays small and fast.
const DoThisNowPage = lazy(() => import('./pages/DoThisNowPage'))
const FocusPage = lazy(() => import('./pages/FocusPage'))
const GymPage = lazy(() => import('./pages/GymPage'))
const CurriculumPage = lazy(() => import('./pages/CurriculumPage'))
const RoadmapPage = lazy(() => import('./pages/RoadmapPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))
const LearnPage = lazy(() => import('./pages/learn/LearnPage'))
const WorldPage = lazy(() => import('./pages/learn/WorldPage'))
const LessonPage = lazy(() => import('./pages/learn/LessonPage'))
const ProveItPage = lazy(() => import('./pages/learn/ProveItPage'))
const RecallPage = lazy(() => import('./pages/recall/RecallPage'))
const QuestsPage = lazy(() => import('./pages/progress/QuestsPage'))
const RecordsPage = lazy(() => import('./pages/progress/RecordsPage'))
const MentorPage = lazy(() => import('./pages/mentor/MentorPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const MorePage = lazy(() => import('./pages/MorePage'))
const VisualizersPage = lazy(() => import('./pages/viz/VisualizersPage'))
const SpeakPage = lazy(() => import('./pages/speak/SpeakPage'))
const MockInterviewPage = lazy(() => import('./pages/speak/MockInterviewPage'))
const ColosseumPage = lazy(() => import('./pages/colosseum/ColosseumPage'))
const MockTestPage = lazy(() => import('./pages/colosseum/MockTestPage'))
const SpringPage = lazy(() => import('./pages/colosseum/SpringPage'))

function LoggedIn() {
  const profile = useApi<ProfileView>('/api/profile')
  useReminder(profile.data?.settings)
  if (profile.loading && !profile.data) return <Spinner />
  if (profile.data && !profile.data.onboardingComplete) {
    return <OnboardingPage onDone={() => void profile.reload()} />
  }
  return (
    <Suspense fallback={<Spinner />}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<CommandCenterPage />} />
          <Route path="now" element={<DoThisNowPage />} />
          <Route path="focus" element={<FocusPage />} />
          <Route path="gym/*" element={<GymPage />} />
          <Route path="curriculum" element={<CurriculumPage />} />
          <Route path="roadmap" element={<RoadmapPage />} />
          <Route path="learn" element={<LearnPage />} />
          <Route path="learn/:code" element={<WorldPage />} />
          <Route path="lesson/:slug" element={<LessonPage />} />
          <Route path="lesson/:slug/prove-it" element={<ProveItPage />} />
          <Route path="recall" element={<RecallPage />} />
          <Route path="quests" element={<QuestsPage />} />
          <Route path="records" element={<RecordsPage />} />
          <Route path="mentor" element={<MentorPage />} />
          <Route path="visualizers" element={<VisualizersPage />} />
          <Route path="visualizers/:id" element={<VisualizersPage />} />
          <Route path="speak" element={<SpeakPage />} />
          <Route path="interview" element={<ColosseumPage />} />
          <Route path="interview/mock" element={<MockInterviewPage />} />
          <Route path="interview/mock-test/:id" element={<MockTestPage />} />
          <Route path="spring" element={<SpringPage />} />
          <Route path="admin" element={<AdminPage />} />
          <Route path="more" element={<MorePage />} />
          <Route path="settings" element={<SettingsPage onProfileChanged={() => void profile.reload()} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  )
}

export default function App() {
  return (
    <WakeGate>
      <Inner />
    </WakeGate>
  )
}

function Inner() {
  const { user, loading } = useAuth()
  if (loading) return <Spinner label="Loading your adventure…" />
  return user ? <LoggedIn /> : <AuthPage />
}
