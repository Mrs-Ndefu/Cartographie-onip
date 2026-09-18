import { useMemo, useState } from 'react'
import { useHouseholds } from '../../hooks/useHouseholds'
import { deleteHouseholdOnServer } from '../../utils/syncClient'
import type { AuthSession } from '../../types/agent'
import type { Household, HouseholdStatus } from '../../types/household'

interface JournalTabProps {
  session: AuthSession
  onEdit: (household: Household) => void
  onToast: (message: string) => void
}

const STATUS_LABELS: Record<HouseholdStatus, string> = {
  complet: 'Complet',
  brouillon: 'Brouillon',
  a_verifier: 'À vérifier',
}

function isToday(isoDate: string) {
  const d = new Date(isoDate)
  const now = new Date()
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
}

function chefFullName(h: Household) {
  return [h.chef.prenom, h.chef.postnom, h.chef.nom].filter(Boolean).join(' ')
}

export function JournalTab({ session, onEdit, onToast }: JournalTabProps) {
  const { households, deleteHousehold } = useHouseholds()
  const [search, setSearch] = useState('')
  const [confirmTarget, setConfirmTarget] = useState<Household | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)

  const todaysHouseholds = useMemo(() => {
    const query = search.trim().toLowerCase()
    return households
      .filter((h) => isToday(h.createdAt))
      .filter((h) => (query ? chefFullName(h).toLowerCase().includes(query) : true))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  }, [households, search])

  async function handleDelete(household: Household) {
    setDeleting(household.id)
    try {
      await deleteHousehold(household.id)
      onToast(`Ménage ${household.codeMenage || '(sans code)'} supprimé.`)

      if (household.syncedAt && navigator.onLine) {
        try {
          await deleteHouseholdOnServer(household.id, session.token)
        } catch {
          // suppression locale déjà effective — le serveur sera à jour au prochain contact
        }
      }
    } finally {
      setDeleting(null)
      setConfirmTarget(null)
    }
  }

  return (
    <div className="journal-tab">
      <div className="table-card">
        <div className="table-card-header journal-card-header">
          <div>
            <h2>Journal du jour</h2>
            <span className="table-card-count">
              {todaysHouseholds.length} enregistrement{todaysHouseholds.length !== 1 ? 's' : ''} aujourd'hui
            </span>
          </div>
          <div className="journal-search-wrap">
            <svg className="journal-search-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <circle cx="9" cy="9" r="6.5" stroke="currentColor" strokeWidth="1.6" />
              <path d="M14 14L18 18" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
            <input
              type="search"
              className="journal-search"
              placeholder="Rechercher un chef de ménage…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        <div className="table-scroll">
          <table className="dashboard-table">
            <thead>
              <tr>
                <th>Code Ménage</th>
                <th>Chef de ménage</th>
                <th>Commune / Quartier</th>
                <th>Statut</th>
                <th>Heure</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {todaysHouseholds.map((h) => (
                <tr key={h.id}>
                  <td className="cell-code">{h.codeMenage || '—'}</td>
                  <td>{chefFullName(h) || '—'}</td>
                  <td>{[h.address?.commune, h.address?.quartier].filter(Boolean).join(' / ') || '—'}</td>
                  <td>
                    <span className={`status-badge status-${h.status}`}>{STATUS_LABELS[h.status]}</span>
                  </td>
                  <td className="cell-date">
                    {new Date(h.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                  </td>
                  <td className="col-actions">
                    <button type="button" className="table-action-btn" onClick={() => onEdit(h)}>
                      Modifier
                    </button>{' '}
                    <button
                      type="button"
                      className="table-action-btn table-action-danger"
                      onClick={() => setConfirmTarget(h)}
                      disabled={deleting === h.id}
                    >
                      Supprimer
                    </button>
                  </td>
                </tr>
              ))}
              {todaysHouseholds.length === 0 && (
                <tr>
                  <td colSpan={6} className="dashboard-empty-row">
                    <div className="journal-empty">
                      <svg viewBox="0 0 48 48" fill="none" aria-hidden="true" className="journal-empty-icon">
                        <rect x="8" y="10" width="32" height="30" rx="3" stroke="currentColor" strokeWidth="2" />
                        <path d="M8 18h32" stroke="currentColor" strokeWidth="2" />
                        <path d="M16 26h16M16 32h10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                      <p>
                        {search
                          ? 'Aucun ménage ne correspond à cette recherche pour aujourd\'hui.'
                          : "Aucun ménage enregistré aujourd'hui pour l'instant."}
                      </p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {confirmTarget && (
        <div className="settings-overlay" onClick={() => setConfirmTarget(null)}>
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <p>Supprimer le ménage {confirmTarget.codeMenage || '(sans code)'} ? Cette action est définitive.</p>
            <div className="confirm-dialog-actions">
              <button type="button" className="cancel-btn" onClick={() => setConfirmTarget(null)}>
                Annuler
              </button>
              <button type="button" className="table-action-danger submit-btn" onClick={() => void handleDelete(confirmTarget)}>
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
