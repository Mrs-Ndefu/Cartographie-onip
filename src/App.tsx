import { useEffect, useState } from 'react'
import { MapView } from './components/Map/MapView'
import { LocationPicker } from './components/Map/LocationPicker'
import { HouseholdForm } from './components/HouseholdForm/HouseholdForm'
import { LoginScreen } from './components/Auth/LoginScreen'
import { Toast } from './components/Toast/Toast'
import { SettingsPanel } from './components/Settings/SettingsPanel'
import { useHouseholds } from './hooks/useHouseholds'
import { useGeolocation } from './hooks/useGeolocation'
import { useAuth } from './hooks/useAuth'
import { useSync } from './hooks/useSync'
import type { Household, GeoLocation } from './types/household'
import './App.css'

type View = 'map' | 'picking' | 'form'

const SYNC_STATUS_LABELS: Record<string, string> = {
  idle: 'Synchronisé',
  syncing: 'Synchronisation…',
  error: 'Erreur de synchronisation',
  offline: 'Hors ligne',
}

function App() {
  const { session, isAuthenticated, login, logout, updateAgent, requireReauth, loginError, loginLoading } = useAuth()
  const { households } = useHouseholds()
  const { status: syncStatus } = useSync(session, requireReauth)
  const [view, setView] = useState<View>('map')
  const [editingHousehold, setEditingHousehold] = useState<Household | null>(null)
  const [pickedLocation, setPickedLocation] = useState<GeoLocation | null>(null)
  const [toast, setToast] = useState<string | null>(null)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const { location: gpsLocation, error: gpsError, loading: gpsLoading, requestLocation } = useGeolocation()

  useEffect(() => {
    if (gpsLocation) setPickedLocation(gpsLocation)
  }, [gpsLocation])

  function startNewHousehold() {
    setEditingHousehold(null)
    setPickedLocation(null)
    setView('picking')
  }

  function selectHouseholdForEdit(household: Household) {
    setEditingHousehold(household)
    setPickedLocation(household.location)
    setView('form')
  }

  function cancelPicking() {
    setPickedLocation(null)
    setView('map')
  }

  function handleSaved(household: Household) {
    setToast(editingHousehold ? `Ménage ${household.codeMenage} mis à jour.` : `Ménage ${household.codeMenage} enregistré.`)
    setView('map')
    setEditingHousehold(null)
    setPickedLocation(null)
  }

  function handleCancelForm() {
    setView('map')
    setEditingHousehold(null)
    setPickedLocation(null)
  }

  if (!isAuthenticated) {
    return <LoginScreen onLogin={login} loading={loginLoading} error={loginError} />
  }

  return (
    <div className="app">
      {view !== 'form' && (
        <>
          <header className="app-toolbar">
            <img src="/onip-logo.png" alt="ONIP" className="app-toolbar-logo" />
            <h1>Carto-Onip-RDC</h1>
            <div className="app-toolbar-actions">
              {view === 'map' && (
                <button type="button" className="add-member-btn" onClick={startNewHousehold}>
                  + Nouveau ménage
                </button>
              )}
              <span className={`sync-badge sync-${syncStatus}`}>{SYNC_STATUS_LABELS[syncStatus]}</span>
              <span className="agent-name">{session?.agent.fullName}</span>
              <button
                type="button"
                className="avatar-btn"
                onClick={() => setSettingsOpen(true)}
                aria-label="Paramètres du compte"
                title="Paramètres du compte"
              >
                {session?.agent.photoDataUrl ? (
                  <img src={session.agent.photoDataUrl} alt="" className="avatar-img" />
                ) : (
                  <span className="avatar-placeholder">
                    {session?.agent.fullName.charAt(0).toUpperCase()}
                  </span>
                )}
              </button>
            </div>
          </header>
          <div className="app-map-wrap">
            <MapView
              households={households}
              pickingLocation={pickedLocation}
              pickingActive={view === 'picking'}
              onPickLocation={setPickedLocation}
              onSelectHousehold={selectHouseholdForEdit}
            />
          </div>
          {view === 'picking' && (
            <LocationPicker
              location={pickedLocation}
              gpsLoading={gpsLoading}
              gpsError={gpsError}
              onUseGps={requestLocation}
              onConfirm={() => setView('form')}
              onCancel={cancelPicking}
            />
          )}
        </>
      )}
      {view === 'form' && (
        <HouseholdForm
          location={pickedLocation}
          household={editingHousehold}
          onSaved={handleSaved}
          onCancel={handleCancelForm}
        />
      )}
      {toast && <Toast message={toast} onDismiss={() => setToast(null)} />}
      {settingsOpen && session && (
        <SettingsPanel
          session={session}
          onClose={() => setSettingsOpen(false)}
          onAgentUpdated={updateAgent}
          onLogout={() => {
            setSettingsOpen(false)
            logout()
          }}
        />
      )}
    </div>
  )
}

export default App
