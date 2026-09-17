import type { Sexe } from '../../types/household'

interface SexeToggleProps {
  value: Sexe | null
  onChange: (value: Sexe) => void
  error?: string
}

export function SexeToggle({ value, onChange, error }: SexeToggleProps) {
  return (
    <span className={`sexe-toggle${error ? ' has-error' : ''}`}>
      <span className="sexe-label">Sexe</span>
      <span className="sexe-option">
        <span>M</span>
        <input
          type="checkbox"
          className="sexe-box"
          checked={value === 'M'}
          onChange={() => onChange('M')}
          aria-label="Sexe masculin"
        />
      </span>
      <span className="sexe-option">
        <span>F</span>
        <input
          type="checkbox"
          className="sexe-box"
          checked={value === 'F'}
          onChange={() => onChange('F')}
          aria-label="Sexe féminin"
        />
      </span>
    </span>
  )
}
