import { useState } from 'react'
import { useAdminHouseholds } from '../../hooks/useAdminHouseholds'
import type { AuthSession } from '../../types/agent'
import type { AdminHousehold } from '../../types/dashboard'
import type { HouseholdStatus } from '../../types/household'

interface HouseholdsTabProps {
  session: AuthSession
}

const STATUS_LABELS: Record<HouseholdStatus, string> = {
  complet: 'Complet',
  brouillon: 'Brouillon',
  a_verifier: 'À vérifier',
}

export function HouseholdsTab({ session }: HouseholdsTabProps) {
  const { households, page, totalPages, totalElements, loading, error, goToPage, remove } = useAdminHouseholds(session)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [confirmTarget, setConfirmTarget] = useState<AdminHousehold | null>(null)

  async function handleDelete(household: AdminHousehold) {
    setDeleting(household.id)
    try {
      await remove(household.id)
    } finally {
      setDeleting(null)
      setConfirmTarget(null)
    }
  }

  if (loading && households.length === 0) return <p className="dashboard-loading">Chargement…</p>
  if (error) return <p className="dashboard-error">{error}</p>

  return (
    <div className="households-tab">
      <p className="households-count">{totalElements} ménage{totalElements > 1 ? 's' : ''} enregistré{totalElements > 1 ? 's' : ''}</p>

      <table className="dashboard-table">
        <thead>
          <tr>
            <th>Code ménage</th>
            <th>Chef de ménage</th>
            <th>Commune / Quartier</th>
            <th>Statut</th>
            <th>Agent</th>
            <th>Mis à jour</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {households.map((h) => (
            <tr key={h.id}>
              <td>{h.codeMenage || '—'}</td>
              <td>{h.chef ? [h.chef.nom, h.chef.postnom, h.chef.prenom].filter(Boolean).join(' ') : '—'}</td>
              <td>{h.address ? [h.address.commune, h.address.quartier].filter(Boolean).join(' / ') : '—'}</td>
              <td>
                <span className={`status-badge status-${h.status}`}>{STATUS_LABELS[h.status]}</span>
              </td>
              <td>{h.agentUsername || '—'}</td>
              <td>{new Date(h.updatedAt).toLocaleDateString('fr-FR')}</td>
              <td>
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
          {households.length === 0 && (
            <tr>
              <td colSpan={7} className="dashboard-empty-row">
                Aucun ménage enregistré.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div className="dashboard-pagination">
          <button type="button" className="toolbar-btn" disabled={page === 0} onClick={() => void goToPage(page - 1)}>
            ← Précédent
          </button>
          <span>
            Page {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="toolbar-btn"
            disabled={page + 1 >= totalPages}
            onClick={() => void goToPage(page + 1)}
          >
            Suivant →
          </button>
        </div>
      )}

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
