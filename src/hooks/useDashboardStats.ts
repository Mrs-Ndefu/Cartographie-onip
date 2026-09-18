import { useCallback, useEffect, useState } from 'react'
import { apiClient } from '../utils/apiClient'
import type { DashboardStats } from '../types/dashboard'
import type { AuthSession } from '../types/agent'

export function useDashboardStats(session: AuthSession) {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await apiClient.get<DashboardStats>('/api/dashboard/stats', session.token)
      setStats(data)
    } catch {
      setError('Impossible de charger les statistiques.')
    } finally {
      setLoading(false)
    }
  }, [session.token])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return { stats, loading, error, refresh }
}
