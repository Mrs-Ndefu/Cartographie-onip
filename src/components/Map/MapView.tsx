import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { HouseholdMarker } from './HouseholdMarker'
import { pendingIcon } from './markerIcons'
import type { Household, GeoLocation } from '../../types/household'
import './Map.css'

const KINSHASA_CENTER: [number, number] = [-4.4419, 15.2663]
const DEFAULT_ZOOM = 12

interface MapClickCatcherProps {
  active: boolean
  onPick: (location: GeoLocation) => void
}

function MapClickCatcher({ active, onPick }: MapClickCatcherProps) {
  useMapEvents({
    click(e) {
      if (!active) return
      onPick({ latitude: e.latlng.lat, longitude: e.latlng.lng, saisieManuelle: true })
    },
  })
  return null
}

interface MapViewProps {
  households: Household[]
  pickingLocation?: GeoLocation | null
  pickingActive?: boolean
  onPickLocation?: (location: GeoLocation) => void
  onSelectHousehold?: (household: Household) => void
}

export function MapView({
  households,
  pickingLocation = null,
  pickingActive = false,
  onPickLocation,
  onSelectHousehold,
}: MapViewProps) {
  const firstWithLocation = households.find((h) => h.location)
  const center: [number, number] = pickingLocation
    ? [pickingLocation.latitude, pickingLocation.longitude]
    : firstWithLocation?.location
      ? [firstWithLocation.location.latitude, firstWithLocation.location.longitude]
      : KINSHASA_CENTER

  return (
    <MapContainer center={center} zoom={DEFAULT_ZOOM} className="map-view">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {onPickLocation && <MapClickCatcher active={pickingActive} onPick={onPickLocation} />}
      {households.map(
        (household) =>
          household.location && (
            <HouseholdMarker
              key={household.id}
              household={household}
              onSelect={() => onSelectHousehold?.(household)}
            />
          ),
      )}
      {pickingLocation && (
        <Marker
          position={[pickingLocation.latitude, pickingLocation.longitude]}
          icon={pendingIcon}
          draggable={pickingActive}
          eventHandlers={
            onPickLocation
              ? {
                  dragend: (e) => {
                    const { lat, lng } = e.target.getLatLng()
                    onPickLocation({ latitude: lat, longitude: lng, saisieManuelle: true })
                  },
                }
              : undefined
          }
        />
      )}
    </MapContainer>
  )
}
