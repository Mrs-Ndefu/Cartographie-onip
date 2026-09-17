import { useCallback, useState } from 'react'
import type { GeoLocation } from '../types/household'

interface UseGeolocationResult {
  location: GeoLocation | null
  error: string | null
  loading: boolean
  requestLocation: () => void
}

export function useGeolocation(): UseGeolocationResult {
  const [location, setLocation] = useState<GeoLocation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setError("La géolocalisation n'est pas disponible sur cet appareil.")
      return
    }
    setLoading(true)
    setError(null)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          precision: position.coords.accuracy,
          saisieManuelle: false,
        })
        setLoading(false)
      },
      (err) => {
        setError(err.message || 'Impossible de récupérer la position GPS.')
        setLoading(false)
      },
      { enableHighAccuracy: true, timeout: 15000 },
    )
  }, [])

  return { location, error, loading, requestLocation }
}
