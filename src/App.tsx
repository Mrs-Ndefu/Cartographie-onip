import { useEffect, useState } from 'react'
import { MapView } from './components/Map/MapView'
import { LocationPicker } from './components/Map/LocationPicker'
import { HouseholdForm } from './components/HouseholdForm/HouseholdForm'
import { LoginScreen } from './components/Auth/LoginScreen'
import { Toast } from './components/Toast/Toast'
import { SettingsPanel } from './components/Settings/SettingsPanel'
import { Dashboard } from './components/Dashboard/Dashboard'
import { useHouseholds } from './hooks/useHouseholds'
import { useGeolocation } from './hooks/useGeolocation'
import { useAuth } from './hooks/useAuth'
import { useSync } from './hooks/useSync'
import { deleteHouseholdOnServer } from './utils/syncClient'
import type { Household, GeoLocation } from './types/household'
import './App.css'

type View = 'map' | 'picking' | 'form' | 'dashboard'

const SYNC_STATUS_LABELS: Record<string, string> = {
  idle: 'Synchronisé',
  syncing: 'Synchronisation…',
  error: 'Erreur de synchronisation',
  offline: 'Hors ligne',
}

function App() {
  const { session, isAuthenticated, login, logout, updateAgent, requireReauth, loginError, loginLoading } = useAuth()
  const { households } = useHouseholds()
  const { status: syncStatus, forceSyncAll } = useSync(session, requireReauth)
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

  async function handleDeleted(household: Household) {
    setToast(`Ménage ${household.codeMenage || '(sans code)'} supprimé.`)
    setView('map')
    setEditingHousehold(null)
    setPickedLocation(null)

    if (household.syncedAt && session && navigator.onLine) {
      try {
        await deleteHouseholdOnServer(household.id, session.token)
      } catch {
        // best effort : le ménage reste supprimé localement même si le serveur est injoignable
      }
    }
  }

  if (!isAuthenticated) {
    return <LoginScreen onLogin={login} loading={loginLoading} error={loginError} />
  }

  if (view === 'dashboard' && session) {
    return (
      <>
        <Dashboard
          session={session}
          onClose={() => setView('map')}
          onEditHousehold={selectHouseholdForEdit}
          onToast={setToast}
        />
        {toast && <Toast message={toast} onDismiss={() => setToast(null)} />}
      </>
    )
  }

  return (
    <div className="app">
      {view !== 'form' && (
        <>
          <header className="app-toolbar">
            <img src="/onip-logo.png" alt="ONIP" className="app-toolbar-logo" />
            <h1>Adressage de Ménages</h1>
            <div className="app-toolbar-actions">
              {view === 'map' && (
                <button type="button" className="add-member-btn" onClick={startNewHousehold}>
                  + Nouveau ménage
                </button>
              )}
              {view === 'map' && session && (
                <button type="button" className="dashboard-btn" onClick={() => setView('dashboard')}>
                  Tableau de bord
                </button>
              )}
              <span className={`sync-badge sync-${syncStatus}`}>{SYNC_STATUS_LABELS[syncStatus]}</span>
              <button
                type="button"
                className="resync-btn"
                onClick={() => void forceSyncAll()}
                disabled={syncStatus === 'syncing'}
                title="Renvoyer tous les ménages locaux au serveur"
              >
                ⟳
              </button>
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
          onDeleted={handleDeleted}
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
