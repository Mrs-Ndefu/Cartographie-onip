import type { GeoLocation, HouseholdStatus } from './household'

export interface RegistrationCounts {
  today: number
  thisMonth: number
  thisYear: number
}

export interface MonthlyCount {
  label: string
  count: number
}

export interface DashboardStats {
  total: number
  countComplet: number
  countBrouillon: number
  countAVerifier: number
  registrationCounts: RegistrationCounts
  sexDistribution: Record<string, number>
  monthlyRegistrations: MonthlyCount[]
}

export interface AdminHousehold {
  id: string
  codeMenage: string | null
  nombreMembres: number | null
  chef: { nom: string | null; postnom: string | null; prenom: string | null } | null
  address: { commune: string | null; quartier: string | null } | null
  location: GeoLocation | null
  status: HouseholdStatus
  agentUsername: string | null
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  last: boolean
}
