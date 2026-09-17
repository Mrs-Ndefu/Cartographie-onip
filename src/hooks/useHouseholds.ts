import { useLiveQuery } from 'dexie-react-hooks'
import db from '../utils/db'
import { generateHouseholdId } from '../utils/idGenerator'
import type { Household } from '../types/household'

export function useHouseholds() {
  const households = useLiveQuery(() => db.households.toArray(), [], undefined)

  async function addHousehold(
    household: Omit<Household, 'id' | 'createdAt' | 'updatedAt' | 'syncedAt'>,
  ): Promise<Household> {
    const now = new Date().toISOString()
    const record: Household = {
      ...household,
      id: generateHouseholdId(),
      createdAt: now,
      updatedAt: now,
      syncedAt: null,
    }
    await db.households.add(record)
    return record
  }

  async function updateHousehold(id: string, changes: Partial<Household>): Promise<void> {
    await db.households.update(id, { ...changes, updatedAt: new Date().toISOString(), syncedAt: null })
  }

  async function markSynced(id: string, syncedAt: string): Promise<void> {
    await db.households.update(id, { syncedAt })
  }

  async function deleteHousehold(id: string): Promise<void> {
    await db.households.delete(id)
  }

  async function getHousehold(id: string): Promise<Household | undefined> {
    return db.households.get(id)
  }

  return {
    households: households ?? [],
    isLoading: households === undefined,
    addHousehold,
    updateHousehold,
    deleteHousehold,
    getHousehold,
    markSynced,
  }
}
