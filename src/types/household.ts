export const CODE_MENAGE_LENGTH = 18
export const NAME_FIELD_LENGTH = 19 // Nom / Postnom / Prénom — compté sur le scan (chef et membres)
export const MAX_MEMBRES = 14

export type Sexe = 'M' | 'F'

export interface Person {
  nom: string
  postnom: string
  prenom: string
  dateNaissance: string // format JJ/MM/AAAA
  sexe: Sexe | null
}

export type HouseholdStatus = 'brouillon' | 'complet' | 'a_verifier'

export interface GeoLocation {
  latitude: number
  longitude: number
  precision?: number // précision GPS en mètres, si disponible
  saisieManuelle: boolean // true si pointage manuel sur la carte plutôt que GPS auto
}

export interface FormMeta {
  faitA: string
  dateEncodage: string // JJ/MM/AAAA
  nombreFiches: string // ex. "1/2"
  agentCarthographe: string
  renseignant: string
}

export interface Address {
  ville: string
  commune: string
  quartier: string
  rue: string
  numero: string
  immeuble: string
}

export interface Household {
  id: string
  codeMenage: string
  nombreMembres: number // déclaré sur la fiche
  chef: Person
  membres: Person[] // 0 à 14 entrées
  address: Address
  location: GeoLocation | null
  meta: FormMeta
  status: HouseholdStatus
  createdAt: string // ISO timestamp
  updatedAt: string // ISO timestamp
  syncedAt: string | null // ISO timestamp de la dernière synchronisation réussie avec le serveur
}

export function createEmptyPerson(): Person {
  return {
    nom: '',
    postnom: '',
    prenom: '',
    dateNaissance: '',
    sexe: null,
  }
}

export function createEmptyFormMeta(): FormMeta {
  return {
    faitA: '',
    dateEncodage: '',
    nombreFiches: '1/1',
    agentCarthographe: '',
    renseignant: '',
  }
}

export function createEmptyAddress(): Address {
  return {
    ville: '',
    commune: '',
    quartier: '',
    rue: '',
    numero: '',
    immeuble: '',
  }
}
