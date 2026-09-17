import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { apiClient, ApiError } from '../../utils/apiClient'
import type { Agent, AuthSession } from '../../types/agent'
import './SettingsPanel.css'

interface SettingsPanelProps {
  session: AuthSession
  onClose: () => void
  onAgentUpdated: (agent: Agent) => void
  onLogout: () => void
}

export function SettingsPanel({ session, onClose, onAgentUpdated, onLogout }: SettingsPanelProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null)
  const [passwordLoading, setPasswordLoading] = useState(false)

  const [photoError, setPhotoError] = useState<string | null>(null)
  const [photoLoading, setPhotoLoading] = useState(false)

  async function handlePasswordSubmit(e: FormEvent) {
    e.preventDefault()
    setPasswordError(null)
    setPasswordSuccess(null)

    if (newPassword.length < 6) {
      setPasswordError('Le nouveau mot de passe doit contenir au moins 6 caractères.')
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Les deux mots de passe ne correspondent pas.')
      return
    }

    setPasswordLoading(true)
    try {
      const updated = await apiClient.put<Agent>(
        '/api/me/password',
        { currentPassword, newPassword },
        session.token,
      )
      onAgentUpdated(updated)
      setPasswordSuccess('Mot de passe mis à jour.')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setPasswordError('Mot de passe actuel incorrect.')
      } else {
        setPasswordError('Impossible de mettre à jour le mot de passe — vérifiez votre connexion.')
      }
    } finally {
      setPasswordLoading(false)
    }
  }

  async function handlePhotoChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setPhotoError(null)

    if (!file.type.startsWith('image/')) {
      setPhotoError('Le fichier doit être une image.')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      setPhotoError('La photo dépasse la taille maximale autorisée (2 Mo).')
      return
    }

    setPhotoLoading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const updated = await apiClient.postForm<Agent>('/api/me/photo', formData, session.token)
      onAgentUpdated(updated)
    } catch {
      setPhotoError('Envoi de la photo impossible — vérifiez votre connexion.')
    } finally {
      setPhotoLoading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="settings-panel" onClick={(e) => e.stopPropagation()}>
        <div className="settings-header">
          <h2>Paramètres du compte</h2>
          <button type="button" className="settings-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </div>

        <div className="settings-profile">
          <div className="settings-photo-wrap">
            {session.agent.photoDataUrl ? (
              <img src={session.agent.photoDataUrl} alt="Photo de profil" className="settings-photo" />
            ) : (
              <div className="settings-photo settings-photo-placeholder">
                {session.agent.fullName.charAt(0).toUpperCase()}
              </div>
            )}
            <button
              type="button"
              className="toolbar-btn"
              onClick={() => fileInputRef.current?.click()}
              disabled={photoLoading}
            >
              {photoLoading ? 'Envoi…' : 'Changer la photo'}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="settings-photo-input"
              onChange={handlePhotoChange}
            />
          </div>
          <div>
            <p className="settings-name">{session.agent.fullName}</p>
            <p className="settings-username">@{session.agent.username}</p>
          </div>
        </div>
        {photoError && <p className="field-error">{photoError}</p>}

        <form className="settings-password-form" onSubmit={handlePasswordSubmit}>
          <h3>Changer le mot de passe</h3>
          <label className="field-label">
            Mot de passe actuel
            <input
              type={showPasswords ? 'text' : 'password'}
              className="text-input"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          </label>
          <label className="field-label">
            Nouveau mot de passe
            <input
              type={showPasswords ? 'text' : 'password'}
              className="text-input"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={6}
              required
            />
          </label>
          <label className="field-label">
            Confirmer le nouveau mot de passe
            <input
              type={showPasswords ? 'text' : 'password'}
              className="text-input"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={6}
              required
            />
          </label>
          <label className="settings-show-passwords">
            <input type="checkbox" checked={showPasswords} onChange={(e) => setShowPasswords(e.target.checked)} />
            Afficher les mots de passe
          </label>

          {passwordError && <p className="field-error">{passwordError}</p>}
          {passwordSuccess && <p className="settings-success">{passwordSuccess}</p>}

          <button type="submit" className="submit-btn" disabled={passwordLoading}>
            {passwordLoading ? 'Mise à jour…' : 'Mettre à jour le mot de passe'}
          </button>
        </form>

        <div className="settings-footer">
          <button type="button" className="cancel-btn" onClick={onLogout}>
            Déconnexion
          </button>
        </div>
      </div>
    </div>
  )
}
