import { useState } from 'react'
import type { FormEvent } from 'react'
import './LoginScreen.css'

interface LoginScreenProps {
  onLogin: (username: string, password: string) => Promise<boolean>
  loading: boolean
  error: string | null
}

export function LoginScreen({ onLogin, loading, error }: LoginScreenProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    await onLogin(username, password)
  }

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={handleSubmit}>
        <img src="/onip-logo.png" alt="ONIP" className="login-logo" />
        <h1>Carto-Onip-RDC</h1>
        <p className="subtitle">Connexion agent cartographe</p>
        {error && <p className="field-error">{error}</p>}
        <label>
          Nom d'utilisateur
          <input value={username} onChange={(e) => setUsername(e.target.value)} required autoFocus />
        </label>
        <label>
          Mot de passe
          <span className="password-field">
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button
              type="button"
              className="toggle-password"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
            >
              {showPassword ? '🙈' : '👁'}
            </button>
          </span>
        </label>
        <button type="submit" className="submit-btn" disabled={loading}>
          {loading ? 'Connexion…' : 'Se connecter'}
        </button>
        <p className="hint">Une fois connecté, l'application reste utilisable hors connexion.</p>
      </form>
    </div>
  )
}
