import { Controller } from 'react-hook-form'
import type { Control } from 'react-hook-form'
import type { HouseholdFormValues } from '../../utils/validation'

interface AddressFieldsProps {
  control: Control<HouseholdFormValues>
}

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
            render={({ field }) => <input className="text-input" {...field} />}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Commune</label>
          <Controller
            control={control}
            name="address.commune"
            render={({ field }) => <input className="text-input" {...field} />}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Quartier</label>
          <Controller
            control={control}
            name="address.quartier"
            render={({ field }) => <input className="text-input" {...field} />}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Rue / Avenue</label>
          <Controller
            control={control}
            name="address.rue"
            render={({ field }) => <input className="text-input" {...field} />}
          />
        </div>

        <div className="field-row">
          <label className="field-label">N°</label>
          <Controller
            control={control}
            name="address.numero"
            render={({ field }) => <input className="text-input small" {...field} />}
          />
        </div>

        <div className="field-row">
          <label className="field-label">Immeuble / Référence</label>
          <Controller
            control={control}
            name="address.immeuble"
            render={({ field }) => <input className="text-input" {...field} />}
          />
        </div>
      </div>
    </section>
  )
}
