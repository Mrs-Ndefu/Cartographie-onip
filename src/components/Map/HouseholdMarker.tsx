import { Marker, Popup } from 'react-leaflet'
import { statusIcon } from './markerIcons'
import type { Household, HouseholdStatus } from '../../types/household'

interface HouseholdMarkerProps {
  household: Household
  onSelect: () => void
}

const STATUS_LABELS: Record<HouseholdStatus, string> = {
  complet: 'Complet',
  brouillon: 'Brouillon',
  a_verifier: 'À vérifier',
}

export function HouseholdMarker({ household, onSelect }: HouseholdMarkerProps) {
  if (!household.location) return null
  const { latitude, longitude } = household.location

  return (
    <Marker position={[latitude, longitude]} icon={statusIcon(household.status)}>
      <Popup>
        <div className="household-popup">
          <p className="household-popup-code">{household.codeMenage || '(sans code)'}</p>
          <p>
            Chef : {household.chef.prenom} {household.chef.nom}
          </p>
          <p>
            {household.membres.length + 1} personne(s) · {STATUS_LABELS[household.status]}
          </p>
          <button type="button" onClick={onSelect}>
            Modifier
          </button>
        </div>
      </Popup>
    </Marker>
  )
}
