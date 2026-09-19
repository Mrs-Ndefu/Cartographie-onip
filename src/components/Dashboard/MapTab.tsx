import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { statusIcon } from '../Map/markerIcons'
import '../Map/Map.css'
import { useMapHouseholds } from '../../hooks/useMapHouseholds'
import type { AuthSession } from '../../types/agent'
import type { AdminHousehold } from '../../types/dashboard'
import type { HouseholdStatus } from '../../types/household'

interface MapTabProps {
  session: AuthSession
}

const KINSHASA_CENTER: [number, number] = [-4.4419, 15.2663]
const DEFAULT_ZOOM = 12

const STATUS_LABELS: Record<HouseholdStatus, string> = {
  complet: 'Complet',
  brouillon: 'Brouillon',
  a_verifier: 'À vérifier',
}

function chefName(h: AdminHousehold): string {
  return h.chef ? [h.chef.prenom, h.chef.nom].filter(Boolean).join(' ') : '(sans nom)'
}

export function MapTab({ session }: MapTabProps) {
  const { households, loading, error } = useMapHouseholds(session)
  const withLocation = households.filter((h) => h.location)

  if (loading && households.length === 0) return <p className="dashboard-loading">Chargement…</p>
  if (error) return <p className="dashboard-error">{error}</p>

  const first = withLocation[0]?.location
  const center: [number, number] = first ? [first.latitude, first.longitude] : KINSHASA_CENTER

  return (
    <div className="map-tab">
      <p className="households-count">
        {withLocation.length} ménage{withLocation.length > 1 ? 's' : ''} localisé
        {withLocation.length > 1 ? 's' : ''} sur {households.length} au total
      </p>
      <MapContainer center={center} zoom={DEFAULT_ZOOM} className="map-tab-container">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {withLocation.map((h) => (
          <Marker key={h.id} position={[h.location!.latitude, h.location!.longitude]} icon={statusIcon(h.status)}>
            <Popup>
              <div className="household-popup">
                <p className="household-popup-code">{h.codeMenage || '(sans code)'}</p>
                <p>Chef : {chefName(h)}</p>
                <p>
                  {STATUS_LABELS[h.status]}
                  {h.agentUsername ? ` · ${h.agentUsername}` : ''}
                </p>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}
