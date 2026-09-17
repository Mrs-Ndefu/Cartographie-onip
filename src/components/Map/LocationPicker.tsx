import type { GeoLocation } from '../../types/household'

interface LocationPickerProps {
  location: GeoLocation | null
  gpsLoading: boolean
  gpsError: string | null
  onUseGps: () => void
  onConfirm: () => void
  onCancel: () => void
}

export function LocationPicker({
  location,
  gpsLoading,
  gpsError,
  onUseGps,
  onConfirm,
  onCancel,
}: LocationPickerProps) {
  return (
    <div className="location-picker">
      <p className="location-picker-title">Position du ménage</p>
      <div className="location-picker-actions">
        <button type="button" className="add-member-btn" onClick={onUseGps} disabled={gpsLoading}>
          {gpsLoading ? 'Recherche GPS…' : '📍 Utiliser ma position GPS'}
        </button>
        <span className="hint">ou cliquez directement sur la carte pour pointer manuellement</span>
      </div>
      {gpsError && <p className="field-error">{gpsError}</p>}
      {location && (
        <p className="location-picker-coords">
          {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
          {location.saisieManuelle
            ? ' — pointage manuel'
            : location.precision
              ? ` — précision ±${Math.round(location.precision)} m`
              : ''}
        </p>
      )}
      <div className="location-picker-buttons">
        <button type="button" className="cancel-btn" onClick={onCancel}>
          Annuler
        </button>
        <button type="button" className="submit-btn" onClick={onConfirm} disabled={!location}>
          Continuer vers le formulaire
        </button>
      </div>
    </div>
  )
}
