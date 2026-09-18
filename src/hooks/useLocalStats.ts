import { useMemo } from 'react'
import type { Household } from '../types/household'
import type { DashboardStats } from '../types/dashboard'

function isSameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

/**
 * Statistiques calculées côté client à partir des ménages stockés localement (IndexedDB) — pas
 * d'appel serveur. Reflète donc uniquement ce que CET appareil a enregistré, cohérent avec le
 * reste de l'app terrain (carte, formulaire) qui fonctionne déjà exclusivement sur les données
 * locales. Fonctionne hors ligne.
 */
export function useLocalStats(households: Household[]): Pick<DashboardStats, 'registrationCounts' | 'sexDistribution' | 'monthlyRegistrations'> {
  return useMemo(() => {
    const now = new Date()

    let today = 0
    let thisMonth = 0
    let thisYear = 0
    let male = 0
    let female = 0

    const monthBuckets = new Map<string, number>()
    const monthOrder: { key: string; label: string }[] = []
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      monthBuckets.set(key, 0)
      monthOrder.push({ key, label: `${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}` })
    }

    for (const h of households) {
      const created = new Date(h.createdAt)

      if (isSameDay(created, now)) today++
      if (created.getFullYear() === now.getFullYear() && created.getMonth() === now.getMonth()) thisMonth++
      if (created.getFullYear() === now.getFullYear()) thisYear++

      const monthKey = `${created.getFullYear()}-${String(created.getMonth() + 1).padStart(2, '0')}`
      if (monthBuckets.has(monthKey)) {
        monthBuckets.set(monthKey, (monthBuckets.get(monthKey) ?? 0) + 1)
      }

      for (const person of [h.chef, ...h.membres]) {
        if (person.sexe === 'M') male++
        else if (person.sexe === 'F') female++
      }
    }

    return {
      registrationCounts: { today, thisMonth, thisYear },
      sexDistribution: { M: male, F: female },
      monthlyRegistrations: monthOrder.map(({ key, label }) => ({ label, count: monthBuckets.get(key) ?? 0 })),
    }
  }, [households])
}
