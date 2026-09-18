import { Controller } from 'react-hook-form'
import type { Control } from 'react-hook-form'
import { CharacterGridInput } from './CharacterGridInput'
import type { HouseholdFormValues } from '../../utils/validation'

interface AddressFieldsProps {
  control: Control<HouseholdFormValues>
}

const VILLE_LENGTH = 15
const COMMUNE_LENGTH = 15
const QUARTIER_LENGTH = 15
const RUE_LENGTH = 19
const NUMERO_LENGTH = 5
const IMMEUBLE_LENGTH = 15

export function AddressFields({ control }: AddressFieldsProps) {
  return (
    <section className="form-section">
      <h2>Adresse</h2>
      <div className="address-grid">
        <div className="field-row">
          <label className="field-label">Ville</label>
          <Controller
            control={control}
            name="address.ville"
            render={({ field }) => (
              <CharacterGridInput
                length={VILLE_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Ville"
              />
            )}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Commune</label>
          <Controller
            control={control}
            name="address.commune"
            render={({ field }) => (
              <CharacterGridInput
                length={COMMUNE_LENGTH}
                value={field.value}
                onChange={field.onChange}
                pattern={/[A-Z0-9]/}
                ariaLabel="Commune"
              />
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
