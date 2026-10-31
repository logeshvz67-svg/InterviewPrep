import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { applyTheme, type ThemeName } from './theme/gameConfig'
import './index.css'

let saved: ThemeName = 'quest-dark'
try { saved = (localStorage.getItem('cq.theme') as ThemeName) ?? 'quest-dark' } catch { /* ignore */ }
applyTheme(saved)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
