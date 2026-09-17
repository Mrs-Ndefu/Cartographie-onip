import { v4 as uuidv4 } from 'uuid'
import { CODE_MENAGE_LENGTH } from '../types/household'

export function generateHouseholdId(): string {
  return uuidv4()
}

const CODE_MENAGE_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'

// Génère un code ménage aléatoire (majuscules/chiffres, longueur du formulaire papier).
// Reste modifiable dans le formulaire si l'agent doit reprendre un code déjà imprimé.
export function generateCodeMenage(): string {
  let code = ''
  for (let i = 0; i < CODE_MENAGE_LENGTH; i++) {
    code += CODE_MENAGE_CHARS[Math.floor(Math.random() * CODE_MENAGE_CHARS.length)]
  }
  return code
}
