import { useCallback, useEffect, useRef, useState } from 'react'
import { useHouseholds } from './useHouseholds'
import { syncHouseholds } from '../utils/syncClient'
import { ApiError } from '../utils/apiClient'
import db from '../utils/db'
import type { AuthSession } from '../types/agent'

export type SyncStatus = 'idle' | 'syncing' | 'error' | 'offline'

const SYNC_INTERVAL_MS = 2 * 60 * 1000

export function useSync(session: AuthSession | null, onAuthExpired: () => void) {
  const { households, markSynced } = useHouseholds()
  const [status, setStatus] = useState<SyncStatus>(navigator.onLine ? 'idle' : 'offline')
  const [lastSyncedAt, setLastSyncedAt] = useState<string | null>(null)
  const syncingRef = useRef(false)

  const runSync = useCallback(async () => {
    if (!session || syncingRef.current || !navigator.onLine) return

    const pending = households.filter((h) => !h.syncedAt || h.syncedAt < h.updatedAt)
    if (pending.length === 0) {
      setStatus('idle')
      return
    }

    syncingRef.current = true
    setStatus('syncing')
    try {
      const result = await syncHouseholds(pending, session.token)
      const now = new Date().toISOString()
      await Promise.all(result.accepted.map((id) => markSynced(id, now)))
      setLastSyncedAt(now)
      setStatus(result.rejected.length > 0 ? 'error' : 'idle')
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        onAuthExpired()
      }
      setStatus('error')
    } finally {
      syncingRef.current = false
    }
  }, [session, households, markSynced, onAuthExpired])

  // Renvoie tous les ménages locaux au serveur, indépendamment de leur syncedAt.
  // Utile si le serveur a perdu des données déjà marquées "synchronisées" côté client
  // (ex. base ré-initialisée) : sans ça, l'app ne les renverrait jamais, faute de le savoir.
  const forceSyncAll = useCallback(async () => {
    if (!session || syncingRef.current || !navigator.onLine) return
    syncingRef.current = true
    setStatus('syncing')
    try {
      const all = await db.households.toArray()
      if (all.length === 0) {
        setStatus('idle')
        return
      }
      const result = await syncHouseholds(all, session.token)
      const now = new Date().toISOString()
      await Promise.all(result.accepted.map((id) => markSynced(id, now)))
      setLastSyncedAt(now)
      setStatus(result.rejected.length > 0 ? 'error' : 'idle')
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        onAuthExpired()
      }
      setStatus('error')
    } finally {
      syncingRef.current = false
    }
  }, [session, markSynced, onAuthExpired])

  useEffect(() => {
    function handleOnline() {
      setStatus('idle')
      void runSync()
    }
    function handleOffline() {
      setStatus('offline')
    }
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [runSync])

  useEffect(() => {
    if (!navigator.onLine) return
    void runSync()
    const interval = setInterval(() => void runSync(), SYNC_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [runSync])

  return { status, lastSyncedAt, syncNow: runSync, forceSyncAll }
}
