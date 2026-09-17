import L from 'leaflet'
import type { HouseholdStatus } from '../../types/household'

const STATUS_COLORS: Record<HouseholdStatus, string> = {
  complet: '#178a17',
  brouillon: '#e0a800',
  a_verifier: '#d33',
}

export function statusIcon(status: HouseholdStatus) {
  return L.divIcon({
    className: 'household-marker-icon',
    html: `<span class="marker-pin" style="background:${STATUS_COLORS[status]}"></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 22],
    popupAnchor: [0, -20],
  })
}

export const pendingIcon = L.divIcon({
  className: 'pending-marker-icon',
  html: '<span class="marker-pin pending"></span>',
  iconSize: [22, 22],
  iconAnchor: [11, 22],
  popupAnchor: [0, -20],
})
