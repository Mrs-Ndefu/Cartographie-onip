import { useEffect, useState } from 'react'
import { apiClient } from '../utils/apiClient'
import type { AdminHousehold, PageResponse } from '../types/dashboard'
import type { AuthSession } from '../types/agent'

// Une seule page large suffit pour l'instant (volumes modestes par agent/jour) — pas besoin de
// paginer une carte comme on pagine un tableau.
const MAP_PAGE_SIZE = 500

export function useMapHouseholds(session: AuthSession) {
  const [households, setHouseholds] = useState<AdminHousehold[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await apiClient.get<PageResponse<AdminHousehold>>(
          `/api/households?page=0&size=${MAP_PAGE_SIZE}`,
          session.token,
        )
        if (!cancelled) setHouseholds(data.content)
      } catch {
        if (!cancelled) setError('Impossible de charger les ménages.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [session.token])

  return { households, loading, error }
}
