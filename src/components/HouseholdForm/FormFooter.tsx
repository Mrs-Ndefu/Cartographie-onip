import { Controller } from 'react-hook-form'
import type { Control } from 'react-hook-form'
import { CharacterGridInput } from './CharacterGridInput'
import { DateInput } from './DateInput'
import type { HouseholdFormValues } from '../../utils/validation'

interface FormFooterProps {
  control: Control<HouseholdFormValues>
}

const FAIT_A_LENGTH = 20

export function FormFooter({ control }: FormFooterProps) {
  return (
    <fieldset className="form-footer">
      <div className="footer-row">
        <label className="field-label">Fait à</label>
        <Controller
          control={control}
          name="faitA"
          render={({ field }) => (
            <CharacterGridInput
              length={FAIT_A_LENGTH}
              value={field.value}
              onChange={field.onChange}
              pattern={/[A-Z]/}
              ariaLabel="Fait à"
            />
          )}
        />

        <label className="field-label">Le</label>
        <Controller
          control={control}
          name="dateEncodage"
          render={({ field, fieldState }) => (
            <DateInput value={field.value} onChange={field.onChange} error={fieldState.error?.message} />
          )}
        />

        <label className="field-label">Nombre de fiches</label>
        <Controller
          control={control}
          name="nombreFiches"
          render={({ field }) => (
            <input className="text-input small" {...field} placeholder="1/1" />
          )}
        />
      </div>

      <div className="footer-signatures">
        <div className="signature-block">
          <label className="field-label">Signature de l'Agent Cartographe</label>
          <Controller
            control={control}
            name="agentCarthographe"
            render={({ field }) => (
              <input className="text-input" {...field} placeholder="Nom de l'agent" />
            )}
          />
        </div>
        <div className="signature-block">
          <label className="field-label">Signature du Renseignant</label>
          <Controller
            control={control}
            name="renseignant"
            render={({ field }) => (
              <input className="text-input" {...field} placeholder="Nom du renseignant" />
            )}
          />
        </div>
      </div>
    </fieldset>
  )
}
