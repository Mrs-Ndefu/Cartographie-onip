import { z } from 'zod'
import { CODE_MENAGE_LENGTH, MAX_MEMBRES, createEmptyAddress } from '../types/household'
import type { Sexe, Address } from '../types/household'

export interface PersonFormValues {
  nom: string
  postnom: string
  prenom: string
  dateNaissance: string
  sexe: Sexe | null
}

export type AddressFormValues = Address

export interface HouseholdFormValues {
  codeMenage: string
  nombreMembres: string
  chef: PersonFormValues
  membres: PersonFormValues[]
  address: AddressFormValues
  faitA: string
  dateEncodage: string
  nombreFiches: string
  agentCarthographe: string
  renseignant: string
}

function isValidCalendarDate(value: string): boolean {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value)
  if (!match) return false
  const day = Number(match[1])
  const month = Number(match[2])
  const year = Number(match[3])
  if (year < 1900 || year > new Date().getFullYear()) return false
  if (month < 1 || month > 12) return false
  return day >= 1 && day <= new Date(year, month, 0).getDate()
}

const dateField = z
  .string()
  .refine((v) => v === '' || isValidCalendarDate(v), 'Date invalide (JJ/MM/AAAA)')

const addressSchema: z.ZodType<AddressFormValues> = z.object({
  ville: z.string(),
  commune: z.string(),
  quartier: z.string(),
  rue: z.string(),
  numero: z.string(),
  immeuble: z.string(),
})

const personSchema: z.ZodType<PersonFormValues> = z.object({
  nom: z.string().trim().min(1, 'Le nom est obligatoire'),
  postnom: z.string(),
  prenom: z.string(),
  dateNaissance: dateField,
  sexe: z.union([z.literal('M'), z.literal('F'), z.null()]).refine((v) => v !== null, {
    message: 'Sexe obligatoire (M ou F)',
  }),
})

export const householdSchema: z.ZodType<HouseholdFormValues> = z
  .object({
    codeMenage: z
      .string()
      .length(CODE_MENAGE_LENGTH, `Le code ménage doit comporter exactement ${CODE_MENAGE_LENGTH} caractères`)
      .regex(/^[A-Z0-9]+$/, 'Majuscules et chiffres uniquement'),
    nombreMembres: z
      .string()
      .min(1, 'Nombre de membres obligatoire')
      .regex(/^\d+$/, 'Nombre invalide'),
    chef: personSchema,
    membres: z
      .array(personSchema)
      .max(MAX_MEMBRES, `Maximum ${MAX_MEMBRES} membres — utiliser une fiche complémentaire au-delà`),
    address: addressSchema,
    faitA: z.string(),
    dateEncodage: dateField,
    nombreFiches: z.string(),
    agentCarthographe: z.string(),
    renseignant: z.string(),
  })
  .refine((data) => Number(data.nombreMembres) === data.membres.length + 1, {
    message: 'Le nombre de membres déclaré ne correspond pas au nombre de personnes saisies (chef inclus)',
    path: ['nombreMembres'],
  })

export function createEmptyPersonForm(): PersonFormValues {
  return { nom: '', postnom: '', prenom: '', dateNaissance: '', sexe: null }
}

export function createDefaultHouseholdFormValues(): HouseholdFormValues {
  return {
    codeMenage: '',
    nombreMembres: '',
    chef: createEmptyPersonForm(),
    membres: [],
    address: createEmptyAddress(),
    faitA: '',
    dateEncodage: '',
    nombreFiches: '1/1',
    agentCarthographe: '',
    renseignant: '',
  }
}
