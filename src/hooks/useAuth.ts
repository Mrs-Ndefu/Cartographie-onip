import { useState } from 'react'
import { apiClient } from '../utils/apiClient'
import type { AuthSession } from '../types/agent'

const STORAGE_KEY = 'facm01.auth'

function loadSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthSession) : null
  } catch {
    return null
  }
}

function saveSession(session: AuthSession | null) {
  try {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // stockage indisponible (navigation privée, quota…) — la session ne survivra pas au rechargement
  }
}

interface LoginResponseBody {
  token: string
  expiresInMinutes: number
  agent: AuthSession['agent']
}

export function useAuth() {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession())
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function login(username: string, password: string): Promise<boolean> {
    setLoading(true)
    setError(null)
    try {
      const body = await apiClient.post<LoginResponseBody>('/api/auth/login', { username, password })
      const newSession: AuthSession = {
        token: body.token,
        expiresInMinutes: body.expiresInMinutes,
        agent: body.agent,
        loggedInAt: new Date().toISOString(),
      }
      setSession(newSession)
      saveSession(newSession)
      return true
    } catch {
      setError("Identifiants invalides, ou serveur injoignable — vérifiez votre connexion et réessayez.")
      return false
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    setSession(null)
    saveSession(null)
  }

  function updateAgent(agent: AuthSession['agent']) {
    setSession((current) => {
      if (!current) return current
      const next = { ...current, agent }
      saveSession(next)
      return next
    })
  }

  function requireReauth() {
    setError('Session expirée — reconnectez-vous pour synchroniser vos données.')
    setSession(null)
    saveSession(null)
  }

  return {
    session,
    isAuthenticated: session !== null,
    login,
    logout,
    updateAgent,
    requireReauth,
    loginError: error,
    loginLoading: loading,
  }
}
