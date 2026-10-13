import { v4 as uuidv4 } from 'uuid'
import { provinceCode } from '../data/drcLocations'

export function generateHouseholdId(): string {
  return uuidv4()
}

const CODE_MENAGE_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
const RANDOM_SUFFIX_LENGTH = 6

// Génère un code foyer "VC00-XXXXXX" : préfixe géographique lisible (1ère lettre de la ville +
// 1ère lettre de la commune + numéro de la province sur 2 chiffres, cf. provinceCode — 01 =
// Kinshasa), puis un tiret et 6 caractères aléatoires (majuscules/chiffres). Reste modifiable
// dans le formulaire si l'agent doit reprendre un code déjà imprimé.
export function generateCodeMenage(province: string, ville: string, commune: string): string {
  let suffix = ''
  for (let i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
    suffix += CODE_MENAGE_CHARS[Math.floor(Math.random() * CODE_MENAGE_CHARS.length)]
  }
  // 'X' si la ville/commune n'est pas encore renseignée : le code reste valide (CODE_MENAGE_PATTERN
  // exige 2 lettres) plutôt que de bloquer la génération — l'agent remplit l'adresse de toute façon
  // avant l'enregistrement, ce préfixe n'est qu'indicatif.
  const villeInitial = ville.trim().charAt(0).toUpperCase() || 'X'
  const communeInitial = commune.trim().charAt(0).toUpperCase() || 'X'
  return `${villeInitial}${communeInitial}${provinceCode(province)}-${suffix}`
}
