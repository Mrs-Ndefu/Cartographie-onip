import { apiClient } from './apiClient'
import type { Household } from '../types/household'

export interface SyncRejection {
  id: string
  reason: string
}

export interface SyncResponseBody {
  accepted: string[]
  rejected: SyncRejection[]
}

export function syncHouseholds(households: Household[], token: string): Promise<SyncResponseBody> {
  return apiClient.post<SyncResponseBody>('/api/households/sync', { households }, token)
}
