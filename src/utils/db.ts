import Dexie, { type EntityTable } from 'dexie'
import type { Household } from '../types/household'

const db = new Dexie('facm01') as Dexie & {
  households: EntityTable<Household, 'id'>
}

db.version(1).stores({
  households: 'id, codeMenage, status, createdAt',
})

export default db
