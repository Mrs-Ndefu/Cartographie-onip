import { useCallback, useEffect, useState } from 'react'
import { apiClient } from '../utils/apiClient'
import type { AdminHousehold, PageResponse } from '../types/dashboard'
import type { AuthSession } from '../types/agent'

const PAGE_SIZE = 25

export function useAdminHouseholds(session: AuthSession) {
  const [households, setHouseholds] = useState<AdminHousehold[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(
    async (targetPage: number) => {
      setLoading(true)
      setError(null)
      try {
        const data = await apiClient.get<PageResponse<AdminHousehold>>(
          `/api/households?page=${targetPage}&size=${PAGE_SIZE}`,
          session.token,
        )
        setHouseholds(data.content)
        setPage(data.number)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
      } catch {
        setError('Impossible de charger les ménages.')
      } finally {
        setLoading(false)
      }
    },
    [session.token],
  )

  useEffect(() => {
    void load(0)
  }, [load])

  async function remove(id: string) {
    await apiClient.delete(`/api/households/${id}`, session.token)
    setHouseholds((current) => current.filter((h) => h.id !== id))
    setTotalElements((n) => n - 1)
  }

  return {
    households,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    goToPage: load,
    remove,
  }
}
