// Villes de la RDC et leurs communes officielles — champs "Ville"/"Commune" du formulaire papier.
// Source : Wikipédia (listes des villes/communes de la RDC, structure provinciale 2015) + jeu de
// données DRC.Geo (github.com/Jonathan-S24/drc-geo) pour la liste des villes par province.
// Liste non garantie exhaustive à 100% (des villes existent sans liste de communes vérifiée
// ci-dessous) — d'où l'option "Autre" en secours dans les deux champs.
export interface VilleEntry {
  name: string
  province: string
  communes: string[]
}

export const DRC_VILLES: VilleEntry[] = [
  { name: 'Kinshasa', province: 'Kinshasa', communes: [
    'Bandalungwa', 'Barumbu', 'Bumbu', 'Gombe', 'Kalamu', 'Kasa-Vubu', 'Kimbanseke', 'Kinshasa',
    'Kintambo', 'Kisenso', 'Lemba', 'Limete', 'Lingwala', 'Makala', 'Maluku', 'Masina', 'Matete',
    'Mont-Ngafula', 'Ndjili', 'Ngaba', 'Ngaliema', 'Ngiri-Ngiri', 'Nsele', 'Selembao',
  ] },
  { name: 'Buta', province: 'Bas-Uele', communes: ['Babade', 'Dobea', 'Finant', 'Tepatondele'] },
  { name: 'Aketi', province: 'Bas-Uele', communes: [] },
  { name: 'Dingila', province: 'Bas-Uele', communes: [] },
  { name: 'Mbandaka', province: 'Équateur', communes: ['Mbandaka', 'Wangata'] },
  { name: 'Likasi', province: 'Haut-Katanga', communes: ['Kikula', 'Likasi', 'Panda', 'Shituru'] },
  { name: 'Lubumbashi', province: 'Haut-Katanga', communes: [
    'Annexe', 'Kamalondo', 'Kampemba', 'Katuba', 'Kenya', 'Lubumbashi', 'Ruashi',
  ] },
  { name: 'Kipushi', province: 'Haut-Katanga', communes: [] },
  { name: 'Kamina', province: 'Haut-Lomami', communes: ['Dimayi', 'Kamina', 'Sobongo', 'Kupa'] },
  { name: 'Isiro', province: 'Haut-Uele', communes: ['Mambaya', 'Mendambo', 'Mbunya'] },
  { name: 'Bunia', province: 'Ituri', communes: ['Nyakasanza', 'Mbunya', 'Shari'] },
  { name: 'Ariwara', province: 'Ituri', communes: [] },
  { name: 'Mongwalu', province: 'Ituri', communes: [] },
  { name: 'Tshikapa', province: 'Kasaï', communes: ['Dibumba I', 'Dibumba II', 'Kanzala', 'Mabondo', 'Mbumba'] },
  { name: 'Kananga', province: 'Kasaï-Central', communes: ['Kananga', 'Katoka', 'Lukonga', 'Ndesha', 'Nganza'] },
  { name: 'Tshimbulu', province: 'Kasaï-Central', communes: [] },
  { name: 'Mbuji-Mayi', province: 'Kasaï-Oriental', communes: ['Bipemba', 'Dibindi', 'Diulu', 'Kanshi', 'Muya'] },
  { name: 'Boma', province: 'Kongo-Central', communes: ['Kabondo', 'Kalamu', 'Nzadi'] },
  { name: 'Matadi', province: 'Kongo-Central', communes: ['Matadi', 'Mvuzi', 'Nzanza'] },
  { name: 'Mbanza-Ngungu', province: 'Kongo-Central', communes: [] },
  { name: 'Inkisi', province: 'Kongo-Central', communes: [] },
  { name: 'Kenge', province: 'Kwango', communes: ['Cinq-Mai', 'Laurent-Désiré-Kabila', 'Manonga', 'Masikita', 'Mavula'] },
  { name: 'Bandundu', province: 'Kwilu', communes: ['Basoko', 'Disasi', 'Mayoyo'] },
  { name: 'Kikwit', province: 'Kwilu', communes: ['Kazamba', 'Kikwit', 'Lukemi', 'Lukolela', 'Nzinda'] },
  { name: 'Kabinda', province: 'Lomami', communes: ['Kabondo', 'Kabuela-Buela', 'Kajiba', 'Mudingayi'] },
  { name: 'Mwene-Ditu', province: 'Lomami', communes: ['Bondoyi', 'Musadi', 'Mwene-Ditu'] },
  { name: 'Kolwezi', province: 'Lualaba', communes: ['Dilala', 'Kolwezi', 'Manika'] },
  { name: 'Kasaji', province: 'Lualaba', communes: [] },
  { name: 'Inongo', province: 'Mai-Ndombe', communes: ['Bonse', 'Inongo', 'Mpolo', 'Mpongonzoli'] },
  { name: 'Nioki', province: 'Mai-Ndombe', communes: [] },
  { name: 'Kindu', province: 'Maniema', communes: ['Alunguli', 'Kasuku', 'Kindu', 'Mikelenge'] },
  { name: 'Kalima', province: 'Maniema', communes: [] },
  { name: 'Lisala', province: 'Mongala', communes: ['Bolikango', 'Lisala', 'Mongala'] },
  { name: 'Bumba', province: 'Mongala', communes: [] },
  { name: 'Gbadolite', province: 'Nord-Ubangi', communes: ['Gbadolite', 'Molegbe', 'Nganza'] },
  { name: 'Beni', province: 'Nord-Kivu', communes: ['Beu', 'Bungulu', 'Mulekera', 'Ruwenzori'] },
  { name: 'Butembo', province: 'Nord-Kivu', communes: ['Bulengera', 'Butembo', 'Kimemi', 'Mususa', 'Vulamba'] },
  { name: 'Goma', province: 'Nord-Kivu', communes: ['Goma', 'Karisimbi'] },
  { name: 'Lusambo', province: 'Sankuru', communes: ['Kabondo', 'Lupembe', 'Lusambo', 'Tusuanganyi'] },
  { name: 'Wembo-Nyama', province: 'Sankuru', communes: ['Ewango', 'Lumumbaville', 'Wembo-Nyama'] },
  { name: 'Bukavu', province: 'Sud-Kivu', communes: ['Bagira', 'Ibanda', 'Kadutu'] },
  { name: 'Baraka', province: 'Sud-Kivu', communes: [] },
  { name: 'Kamituga', province: 'Sud-Kivu', communes: [] },
  { name: 'Uvira', province: 'Sud-Kivu', communes: [] },
  { name: 'Gemena', province: 'Sud-Ubangi', communes: ['Gbazubu', 'Labo', 'Lac-Ntumba', 'Mont-Gila'] },
  { name: 'Zongo', province: 'Sud-Ubangi', communes: [] },
  { name: 'Kalemie', province: 'Tanganyika', communes: ['Kalemie', 'Lac', 'Lukuga'] },
  { name: 'Kisangani', province: 'Tshopo', communes: ['Kabondo', 'Kisangani', 'Lubunga', 'Makiso', 'Mangobo', 'Tshopo'] },
  { name: 'Yangambi', province: 'Tshopo', communes: [] },
  { name: 'Boende', province: 'Tshuapa', communes: ['Boende', 'Tshuapa'] },
]

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
