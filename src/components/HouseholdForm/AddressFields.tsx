import { useState } from 'react'
import { Controller, useWatch } from 'react-hook-form'
import type { Control, UseFormSetValue } from 'react-hook-form'
import { CharacterGridInput } from './CharacterGridInput'
import { DRC_VILLES, DRC_PROVINCES, AUTRE_VILLE, communesForVille, provinceForVille } from '../../data/drcLocations'
import type { HouseholdFormValues } from '../../utils/validation'

interface AddressFieldsProps {
  control: Control<HouseholdFormValues>
  setValue: UseFormSetValue<HouseholdFormValues>
}

const QUARTIER_LENGTH = 15
const RUE_LENGTH = 19
const NUMERO_LENGTH = 5
const IMMEUBLE_LENGTH = 15

const SORTED_VILLES = [...DRC_VILLES].sort((a, b) => a.name.localeCompare(b.name))

export function AddressFields({ control, setValue }: AddressFieldsProps) {
  const province = useWatch({ control, name: 'address.province' }) ?? ''
  const ville = useWatch({ control, name: 'address.ville' }) ?? ''
  const commune = useWatch({ control, name: 'address.commune' }) ?? ''
  const [villeIsOther, setVilleIsOther] = useState(ville !== '' && !SORTED_VILLES.some((v) => v.name === ville))
  // Villes de la province choisie (toutes tant qu'aucune province n'est choisie).
  const villeOptions = province
    ? SORTED_VILLES.filter((v) => v.province.toUpperCase() === province)
    : SORTED_VILLES
  const communeOptions = villeIsOther ? [] : communesForVille(ville)
  const [communeIsOther, setCommuneIsOther] = useState(
    commune !== '' && !communeOptions.some((c) => c === commune),
  )

  return (
    <section className="form-section">
      <h2>Adresse</h2>
      <div className="address-grid">
        <div className="field-row">
          <label className="field-label">Province</label>
          <Controller
            control={control}
            name="address.province"
            render={({ field }) => (
              <select
                className="text-input"
                value={field.value ?? ''}
                onChange={(e) => {
                  // Changer de province vide la ville et la commune (elles appartenaient à
                  // l'ancienne province).
                  field.onChange(e.target.value)
                  setVilleIsOther(false)
                  setCommuneIsOther(false)
                  setValue('address.ville', '')
                  setValue('address.commune', '')
                }}
              >
                <option value="">—</option>
                {DRC_PROVINCES.map((p) => (
                  <option key={p} value={p.toUpperCase()}>
                    {p}
                  </option>
                ))}
              </select>
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Ville</label>
          <Controller
            control={control}
            name="address.ville"
            render={({ field }) => (
              <>
                <select
                  className="text-input"
                  value={villeIsOther ? AUTRE_VILLE : field.value}
                  onChange={(e) => {
                    const value = e.target.value
                    if (value === AUTRE_VILLE) {
                      setVilleIsOther(true)
                      field.onChange('')
                    } else {
                      setVilleIsOther(false)
                      field.onChange(value)
                      // Une ville de la liste impose sa province.
                      const villeProvince = provinceForVille(value)
                      if (villeProvince) setValue('address.province', villeProvince)
                    }
                    setCommuneIsOther(false)
                    setValue('address.commune', '')
                  }}
                >
                  <option value="">—</option>
                  {villeOptions.map((v) => (
                    <option key={v.name} value={v.name}>
                      {v.name}
                    </option>
                  ))}
                  <option value={AUTRE_VILLE}>Autre (saisie libre)</option>
                </select>
                {villeIsOther && (
                  <input
                    type="text"
                    className="text-input"
                    placeholder="Ville / territoire"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                  />
                )}
              </>
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Commune</label>
          <Controller
            control={control}
            name="address.commune"
            render={({ field }) => (
              <>
                <select
                  className="text-input"
                  value={communeIsOther ? AUTRE_VILLE : field.value}
                  disabled={ville === '' && !villeIsOther}
                  onChange={(e) => {
                    const value = e.target.value
                    if (value === AUTRE_VILLE) {
                      setCommuneIsOther(true)
                      field.onChange('')
                    } else {
                      setCommuneIsOther(false)
                      field.onChange(value)
                    }
                  }}
                >
                  <option value="">—</option>
                  {communeOptions.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                  <option value={AUTRE_VILLE}>Autre (saisie libre)</option>
                </select>
                {communeIsOther && (
                  <input
                    type="text"
                    className="text-input"
                    placeholder="Commune"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                  />
                )}
              </>
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Quartier</label>
          <Controller
            control={control}
            name="address.quartier"
            render={({ field }) => (
              <CharacterGridInput
                length={QUARTIER_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Quartier"
              />
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Rue / Avenue</label>
          <Controller
            control={control}
            name="address.rue"
            render={({ field }) => (
              <CharacterGridInput
                length={RUE_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Rue / Avenue"
              />
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">N°</label>
          <Controller
            control={control}
            name="address.numero"
            render={({ field }) => (
              <CharacterGridInput
                length={NUMERO_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Numéro"
              />
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Immeuble / Référence</label>
          <Controller
            control={control}
            name="address.immeuble"
            render={({ field }) => (
              <CharacterGridInput
                length={IMMEUBLE_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Immeuble / Référence"
              />
            )}
          />
        </div>
      </div>
    </section>
  )
}
