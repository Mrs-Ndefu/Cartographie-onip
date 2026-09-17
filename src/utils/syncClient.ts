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

export function deleteHouseholdOnServer(id: string, token: string): Promise<void> {
  return apiClient.delete<void>(`/api/households/${id}`, token)
}
