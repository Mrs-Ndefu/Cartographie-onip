import { v4 as uuidv4 } from 'uuid'

export function generateHouseholdId(): string {
  return uuidv4()
}
