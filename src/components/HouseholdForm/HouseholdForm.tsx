import { useForm, useFieldArray, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { OfficialHeader } from './OfficialHeader'
import { CodeMenageInput } from './CodeMenageInput'
import { AddressFields } from './AddressFields'
import { PersonFields } from './PersonFields'
import {
  householdSchema,
  createDefaultHouseholdFormValues,
  createEmptyPersonForm,
} from '../../utils/validation'
import type { HouseholdFormValues } from '../../utils/validation'
import { MAX_MEMBRES, createEmptyAddress } from '../../types/household'
import type { Household, GeoLocation } from '../../types/household'
import { useHouseholds } from '../../hooks/useHouseholds'
import { generateCodeMenage } from '../../utils/idGenerator'
import './HouseholdForm.css'

interface HouseholdFormProps {
  location?: GeoLocation | null
  household?: Household | null
  onSaved?: (household: Household) => void
  onCancel?: () => void
  onDeleted?: (household: Household) => void
}

function toHousehold(
  values: HouseholdFormValues,
  location: GeoLocation | null,
): Omit<Household, 'id' | 'createdAt' | 'updatedAt' | 'syncedAt'> {
  return {
    codeMenage: values.codeMenage,
    nombreMembres: Number(values.nombreMembres),
    chef: values.chef,
    membres: values.membres,
    address: values.address,
    location,
    meta: {
      faitA: values.faitA,
      dateEncodage: values.dateEncodage,
      nombreFiches: values.nombreFiches,
      agentCarthographe: values.agentCarthographe,
      renseignant: values.renseignant,
    },
    status: 'complet',
  }
}

function toFormValues(household: Household): HouseholdFormValues {
  return {
    codeMenage: household.codeMenage,
    nombreMembres: String(household.nombreMembres),
    chef: household.chef,
    membres: household.membres,
    address: household.address ?? createEmptyAddress(),
    faitA: household.meta.faitA,
    dateEncodage: household.meta.dateEncodage,
    nombreFiches: household.meta.nombreFiches,
    agentCarthographe: household.meta.agentCarthographe,
    renseignant: household.meta.renseignant,
  }
}

export function HouseholdForm({ location = null, household = null, onSaved, onCancel, onDeleted }: HouseholdFormProps) {
  const { addHousehold, updateHousehold, deleteHousehold } = useHouseholds()

  const {
    control,
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<HouseholdFormValues>({
    resolver: zodResolver(householdSchema),
    defaultValues: household ? toFormValues(household) : createDefaultHouseholdFormValues(),
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'membres' })

  async function onSubmit(values: HouseholdFormValues) {
    if (household) {
      const changes = toHousehold(values, location)
      await updateHousehold(household.id, changes)
      onSaved?.({ ...household, ...changes })
    } else {
      // Code ménage non saisi par l'agent : généré automatiquement au moment de l'enregistrement.
      const finalValues = values.codeMenage ? values : { ...values, codeMenage: generateCodeMenage() }
      const saved = await addHousehold(toHousehold(finalValues, location))
      onSaved?.(saved)
    }
  }

  async function handleDelete() {
    if (!household) return
    if (!window.confirm(`Supprimer le ménage ${household.codeMenage || '(sans code)'} ? Cette action est irréversible.`)) {
      return
    }
    await deleteHousehold(household.id)
    onDeleted?.(household)
  }

  return (
    <form className="household-form" onSubmit={handleSubmit(onSubmit)}>
      <OfficialHeader />
      <div className="form-header">
        <div className="header-field">
          <label className="field-label">Code Ménage*</label>
          <Controller
            control={control}
            name="codeMenage"
            render={({ field, fieldState }) => (
              <CodeMenageInput
                value={field.value}
                onChange={field.onChange}
                error={fieldState.error?.message}
              />
            )}
          />
          {!household && (
            <button
              type="button"
              className="generate-code-btn"
              onClick={() => setValue('codeMenage', generateCodeMenage(), { shouldValidate: true })}
            >
              Générer un code
            </button>
          )}
          {errors.codeMenage && <span className="field-error">{errors.codeMenage.message}</span>}
          {!household && (
            <span className="hint">Laisser vide : un code sera généré automatiquement à l'enregistrement.</span>
          )}
        </div>
        <div className="header-field">
          <label className="field-label">Nombre de Membres*</label>
          <input
            className="text-input small"
            type="text"
            inputMode="numeric"
            maxLength={3}
            {...register('nombreMembres')}
          />
          {errors.nombreMembres && (
            <span className="field-error">{errors.nombreMembres.message}</span>
          )}
        </div>
      </div>

      <section className="form-section">
        <h2>Identité du chef de ménage</h2>
        <PersonFields control={control} namePrefix="chef" title="Chef de ménage" />
      </section>

      <AddressFields control={control} />

      <section className="form-section">
        <div className="section-header-row">
          <h2>Identités des membres du ménage</h2>
          <button
            type="button"
            className="add-member-btn"
            onClick={() => append(createEmptyPersonForm())}
            disabled={fields.length >= MAX_MEMBRES}
          >
            + Ajouter un membre
          </button>
        </div>
        {fields.length >= MAX_MEMBRES && (
          <p className="hint">
            Maximum {MAX_MEMBRES} membres par fiche — utiliser une fiche complémentaire au-delà.
          </p>
        )}
        {errors.membres?.root && (
          <p className="field-error">{errors.membres.root.message}</p>
        )}

        <div className="members-grid">
          {fields.map((field, index) => (
            <PersonFields
              key={field.id}
              control={control}
              namePrefix={`membres.${index}`}
              title={`Membre ${index + 1}`}
              showRelation
              onRemove={() => remove(index)}
            />
          ))}
        </div>
      </section>

      <div className="form-actions">
        {household ? (
          <button type="button" className="delete-btn" onClick={handleDelete}>
            Supprimer le ménage
          </button>
        ) : (
          <span />
        )}
        <div className="form-actions-right">
          {onCancel && (
            <button type="button" className="cancel-btn" onClick={onCancel}>
              Annuler
            </button>
          )}
          <button type="submit" className="submit-btn" disabled={isSubmitting}>
            {household ? 'Mettre à jour le ménage' : 'Enregistrer le ménage'}
          </button>
        </div>
      </div>
    </form>
  )
}
