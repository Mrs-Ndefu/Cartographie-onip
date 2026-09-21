import { Controller } from 'react-hook-form'
import type { Control, FieldPath } from 'react-hook-form'
import { CharacterGridInput } from './CharacterGridInput'
import { DateInput } from './DateInput'
import { SexeToggle } from './SexeToggle'
import { NAME_FIELD_LENGTH, RELATION_OPTIONS } from '../../types/household'
import type { HouseholdFormValues } from '../../utils/validation'

type PersonPath = 'chef' | `membres.${number}`

interface PersonFieldsProps {
  control: Control<HouseholdFormValues>
  namePrefix: PersonPath
  title: string
  requiredNom?: boolean
  showRelation?: boolean
  onRemove?: () => void
}

export function PersonFields({
  control,
  namePrefix,
  title,
  requiredNom = true,
  showRelation = false,
  onRemove,
}: PersonFieldsProps) {
  const path = (field: string) => `${namePrefix}.${field}` as FieldPath<HouseholdFormValues>

  return (
    <fieldset className="person-fields">
      <div className="person-fields-header">
        <legend>{title}</legend>
        {onRemove && (
          <button type="button" className="remove-member-btn" onClick={onRemove}>
            × Retirer
          </button>
        )}
      </div>

      <div className="field-row">
        <label className="field-label">{requiredNom ? '1. Nom*' : '1. Nom'}</label>
        <Controller
          control={control}
          name={path('nom')}
          render={({ field, fieldState }) => (
            <CharacterGridInput
              length={NAME_FIELD_LENGTH}
              value={(field.value as string) ?? ''}
              onChange={field.onChange}
              pattern={/[A-Z]/}
              error={fieldState.error?.message}
              ariaLabel={`${title} - Nom`}
            />
          )}
        />
      </div>

      <div className="field-row">
        <label className="field-label">2. Postnom</label>
        <Controller
          control={control}
          name={path('postnom')}
          render={({ field }) => (
            <CharacterGridInput
              length={NAME_FIELD_LENGTH}
              value={(field.value as string) ?? ''}
              onChange={field.onChange}
              pattern={/[A-Z]/}
              ariaLabel={`${title} - Postnom`}
            />
          )}
        />
      </div>

      <div className="field-row">
        <label className="field-label">3. Prénom</label>
        <Controller
          control={control}
          name={path('prenom')}
          render={({ field }) => (
            <CharacterGridInput
              length={NAME_FIELD_LENGTH}
              value={(field.value as string) ?? ''}
              onChange={field.onChange}
              pattern={/[A-Z]/}
              ariaLabel={`${title} - Prénom`}
            />
          )}
        />
      </div>

      <div className="field-row date-sexe-row">
        <label className="field-label">4. Date de naissance</label>
        <Controller
          control={control}
          name={path('dateNaissance')}
          render={({ field, fieldState }) => (
            <DateInput
              value={(field.value as string) ?? ''}
              onChange={field.onChange}
              error={fieldState.error?.message}
            />
          )}
        />
        <Controller
          control={control}
          name={path('sexe')}
          render={({ field, fieldState }) => (
            <SexeToggle
              value={field.value as 'M' | 'F' | null}
              onChange={field.onChange}
              error={fieldState.error?.message}
            />
          )}
        />
      </div>

      <div className="field-row">
        <label className="field-label">5. Lieu de naissance</label>
        <Controller
          control={control}
          name={path('lieuNaissance')}
          render={({ field }) => (
            <input
              type="text"
              className="text-input"
              value={(field.value as string) ?? ''}
              onChange={(e) => field.onChange(e.target.value.toUpperCase())}
              aria-label={`${title} - Lieu de naissance`}
            />
          )}
        />
      </div>

      {showRelation && (
        <div className="field-row">
          <label className="field-label">Lien de parenté avec le chef</label>
          <Controller
            control={control}
            name={path('relation')}
            render={({ field }) => (
              <select className="text-input" {...field} value={(field.value as string) ?? ''}>
                <option value="">—</option>
                {RELATION_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            )}
          />
        </div>
      )}
    </fieldset>
  )
}
