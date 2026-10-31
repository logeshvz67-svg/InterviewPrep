import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { Spinner } from './components/ui'
import { WakeGate } from './components/WakeGate'
import { useApi } from './lib/hooks'
import type { ProfileView } from './api/types'
import AuthPage from './pages/AuthPage'
import OnboardingPage from './pages/OnboardingPage'
import CommandCenterPage from './pages/CommandCenterPage'
import DoThisNowPage from './pages/DoThisNowPage'
import FocusPage from './pages/FocusPage'
import GymPage from './pages/GymPage'
import CurriculumPage from './pages/CurriculumPage'
import RoadmapPage from './pages/RoadmapPage'
import SettingsPage from './pages/SettingsPage'

function LoggedIn() {
  const profile = useApi<ProfileView>('/api/profile')
  if (profile.loading && !profile.data) return <Spinner />
  if (profile.data && !profile.data.onboardingComplete) {
    return <OnboardingPage onDone={() => void profile.reload()} />
  }
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CommandCenterPage />} />
        <Route path="now" element={<DoThisNowPage />} />
        <Route path="focus" element={<FocusPage />} />
        <Route path="gym/*" element={<GymPage />} />
        <Route path="curriculum" element={<CurriculumPage />} />
        <Route path="roadmap" element={<RoadmapPage />} />
        <Route path="settings" element={<SettingsPage onProfileChanged={() => void profile.reload()} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
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
