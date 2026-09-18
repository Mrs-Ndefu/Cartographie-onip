import { useCallback, useEffect, useState } from 'react'
import { apiClient } from '../utils/apiClient'
import type { Agent, AgentRole, AuthSession } from '../types/agent'

export function useAgents(session: AuthSession) {
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await apiClient.get<Agent[]>('/api/agents', session.token)
      setAgents(data)
    } catch {
      setError('Impossible de charger les agents.')
    } finally {
      setLoading(false)
    }
  }, [session.token])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function createAgent(input: { username: string; password: string; fullName: string; role: AgentRole }) {
    setActionError(null)
    try {
      await apiClient.post<Agent>('/api/agents', input, session.token)
      await refresh()
      return true
    } catch {
      setActionError("Création refusée (identifiant déjà utilisé ou données invalides).")
      return false
    }
  }

  async function setActive(agent: Agent, active: boolean) {
    setActionError(null)
    try {
      const updated = await apiClient.patch<Agent>(`/api/agents/${agent.id}/active`, { active }, session.token)
      setAgents((current) => current.map((a) => (a.id === updated.id ? updated : a)))
    } catch {
      setActionError('Action refusée.')
    }
  }

  async function changeRole(agent: Agent, role: AgentRole) {
    setActionError(null)
    try {
      const updated = await apiClient.patch<Agent>(`/api/agents/${agent.id}/role`, { role }, session.token)
      setAgents((current) => current.map((a) => (a.id === updated.id ? updated : a)))
    } catch {
      setActionError('Changement de rôle refusé.')
    }
  }

  async function resetPassword(agent: Agent, newPassword: string) {
    setActionError(null)
    try {
      await apiClient.post<Agent>(`/api/agents/${agent.id}/reset-password`, { newPassword }, session.token)
      return true
    } catch {
      setActionError('Échec de la réinitialisation du mot de passe.')
      return false
    }
  }

  return { agents, loading, error, actionError, clearActionError: () => setActionError(null), createAgent, setActive, changeRole, resetPassword }
}
