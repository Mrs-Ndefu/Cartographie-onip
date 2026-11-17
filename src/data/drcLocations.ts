// Référentiel géographique de la RDC : provinces -> villes/territoires -> communes/cités/secteurs
// -> quartiers. Même fichier que le backend (backend/src/main/resources/geo/drc-geo.json) et
// l'app Android (assets/drc-geo.json), généré à partir de « Subdivision ADM-RDC » (Carto-Begins)
// fusionné avec les anciennes listes. Non exhaustif : les champs gardent l'option « Autre ».
import geo from './drc-geo.json'

export interface VilleEntry {
  name: string
  province: string
  communes: string[]
  /** commune -> quartiers (absente si non renseignés) */
  quartiers: Record<string, string[]>
}

interface GeoFile {
  villes: { name: string; province: string; communes: { name: string; quartiers: string[] }[] }[]
}

export const DRC_VILLES: VilleEntry[] = (geo as GeoFile).villes.map((v) => ({
  name: v.name,
  province: v.province,
  communes: v.communes.map((c) => c.name),
  quartiers: Object.fromEntries(
    v.communes.filter((c) => c.quartiers.length > 0).map((c) => [c.name, c.quartiers]),
  ),
}))

/** Quartiers d'une commune de la liste ([] si inconnue ou non renseignés). */
export function quartiersFor(ville: string, commune: string): string[] {
  if (!ville || !commune) return []
  const entry = DRC_VILLES.find((v) => v.name.toUpperCase() === ville.toUpperCase())
  if (!entry) return []
  const key = Object.keys(entry.quartiers).find((c) => c.toUpperCase() === commune.toUpperCase())
  return key ? entry.quartiers[key] : []
}

export const AUTRE_VILLE = 'AUTRE'

export function communesForVille(ville: string): string[] {
  return DRC_VILLES.find((v) => v.name.toUpperCase() === ville.toUpperCase())?.communes ?? []
}

// Les 26 provinces de la RDC — champ "Province" du formulaire, placé avant la ville.
export const DRC_PROVINCES: string[] = [
  'Bas-Uele', 'Équateur', 'Haut-Katanga', 'Haut-Lomami', 'Haut-Uele', 'Ituri', 'Kasaï',
  'Kasaï-Central', 'Kasaï-Oriental', 'Kinshasa', 'Kongo-Central', 'Kwango', 'Kwilu', 'Lomami',
  'Lualaba', 'Mai-Ndombe', 'Maniema', 'Mongala', 'Nord-Kivu', 'Nord-Ubangi', 'Sankuru',
  'Sud-Kivu', 'Sud-Ubangi', 'Tanganyika', 'Tshopo', 'Tshuapa',
]

/** Province (en majuscules, comme le reste de la saisie) d'une ville de la liste, ou '' pour une ville saisie librement. */
export function provinceForVille(ville: string): string {
  return DRC_VILLES.find((v) => v.name.toUpperCase() === ville.toUpperCase())?.province.toUpperCase() ?? ''
}

// Numéro de la province sur 2 chiffres (01 = Kinshasa, puis les 25 autres par ordre
// alphabétique) — même convention que backend/.../DrcProvinces.numberOf, répliquée ici car le
// code ménage se génère côté client (cf. idGenerator.ts), sans appel au serveur.
const DRC_PROVINCES_BY_LETTER: string[] = [
  'Kinshasa',
  ...DRC_PROVINCES.filter((p) => p.toUpperCase() !== 'KINSHASA'),
]

export function provinceCode(province: string): string {
  const index = DRC_PROVINCES_BY_LETTER.findIndex((p) => p.toUpperCase() === province.toUpperCase())
  return index < 0 ? '00' : String(index + 1).padStart(2, '0')
}
