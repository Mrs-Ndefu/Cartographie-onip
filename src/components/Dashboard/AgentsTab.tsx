import { useState } from 'react'
import type { FormEvent } from 'react'
import { useAgents } from '../../hooks/useAgents'
import type { Agent, AgentRole, AuthSession } from '../../types/agent'

interface AgentsTabProps {
  session: AuthSession
}

export function AgentsTab({ session }: AgentsTabProps) {
  const { agents, loading, error, actionError, clearActionError, createAgent, setActive, changeRole, resetPassword } =
    useAgents(session)
  const [showCreate, setShowCreate] = useState(false)
  const [resetTarget, setResetTarget] = useState<Agent | null>(null)

  if (loading) return <p className="dashboard-loading">Chargement…</p>
  if (error) return <p className="dashboard-error">{error}</p>

  return (
    <div className="agents-tab">
      <div className="agents-tab-header">
        <p className="households-count">{agents.length} agent{agents.length > 1 ? 's' : ''}</p>
        <button type="button" className="submit-btn" onClick={() => setShowCreate(true)}>
          + Nouvel agent
        </button>
      </div>

      {actionError && (
        <p className="field-error" onClick={clearActionError}>
          {actionError}
        </p>
      )}

      <table className="dashboard-table">
        <thead>
          <tr>
            <th>Nom</th>
            <th>Identifiant</th>
            <th>Rôle</th>
            <th>Actif</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {agents.map((agent) => {
            const isSelf = agent.username === session.agent.username
            return (
              <tr key={agent.id}>
                <td>{agent.fullName}</td>
                <td>@{agent.username}{isSelf ? ' (vous)' : ''}</td>
                <td>
                  <select
                    className="text-input"
                    value={agent.role}
                    disabled={isSelf}
                    onChange={(e) => void changeRole(agent, e.target.value as AgentRole)}
                  >
                    <option value="AGENT">Agent</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </td>
                <td>
                  <label className="agent-active-toggle">
                    <input
                      type="checkbox"
                      checked={agent.active}
                      disabled={isSelf}
                      onChange={(e) => void setActive(agent, e.target.checked)}
                    />
                  </label>
                </td>
                <td>
                  <button type="button" className="table-action-btn" onClick={() => setResetTarget(agent)}>
                    Réinitialiser mot de passe
                  </button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {showCreate && <CreateAgentDialog onClose={() => setShowCreate(false)} onCreate={createAgent} />}
      {resetTarget && (
        <ResetPasswordDialog
          agent={resetTarget}
          onClose={() => setResetTarget(null)}
          onConfirm={async (password) => {
            const ok = await resetPassword(resetTarget, password)
            if (ok) setResetTarget(null)
          }}
        />
      )}
    </div>
  )
}

function CreateAgentDialog({
  onClose,
  onCreate,
}: {
  onClose: () => void
  onCreate: (input: { username: string; password: string; fullName: string; role: AgentRole }) => Promise<boolean>
}) {
  const [fullName, setFullName] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<AgentRole>('AGENT')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    const ok = await onCreate({ fullName, username, password, role })
    setSubmitting(false)
    if (ok) onClose()
  }

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="settings-panel" onClick={(e) => e.stopPropagation()}>
        <div className="settings-header">
          <h2>Nouvel agent</h2>
          <button type="button" className="settings-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit} className="settings-password-form">
          <label className="field-label">
            Nom complet
            <input className="text-input" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
          </label>
          <label className="field-label">
            Identifiant
            <input className="text-input" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="field-label">
            Mot de passe
            <input
              type="password"
              className="text-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={6}
              required
            />
          </label>
          <label className="field-label">
            Rôle
            <select className="text-input" value={role} onChange={(e) => setRole(e.target.value as AgentRole)}>
              <option value="AGENT">Agent</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <button type="submit" className="submit-btn" disabled={submitting}>
            {submitting ? 'Création…' : 'Créer'}
          </button>
        </form>
      </div>
    </div>
  )
}

function ResetPasswordDialog({
  agent,
  onClose,
  onConfirm,
}: {
  agent: Agent
  onClose: () => void
  onConfirm: (password: string) => Promise<void>
}) {
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    await onConfirm(password)
    setSubmitting(false)
  }

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <p>Nouveau mot de passe pour @{agent.username}</p>
          <input
            type="password"
            className="text-input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={6}
            required
            autoFocus
          />
          <div className="confirm-dialog-actions">
            <button type="button" className="cancel-btn" onClick={onClose}>
              Annuler
            </button>
            <button type="submit" className="submit-btn" disabled={submitting || password.length < 6}>
              Confirmer
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
