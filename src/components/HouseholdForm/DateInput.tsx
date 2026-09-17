import { useRef } from 'react'
import type { ChangeEvent, KeyboardEvent } from 'react'

interface DateInputProps {
  value: string // "JJ/MM/AAAA" ou ''
  onChange: (value: string) => void
  error?: string
}

const SEGMENTS = [
  { length: 2, placeholder: 'JJ' },
  { length: 2, placeholder: 'MM' },
  { length: 4, placeholder: 'AAAA' },
] as const

function parseValue(value: string): [string, string, string] {
  const [jour = '', mois = '', annee = ''] = value.split('/')
  return [jour, mois, annee]
}

function buildValue(parts: [string, string, string]): string {
  if (parts.every((p) => p === '')) return ''
  return parts.join('/')
}

export function DateInput({ value, onChange, error }: DateInputProps) {
  const refs = useRef<(HTMLInputElement | null)[]>([])
  const parts = parseValue(value)

  function update(index: number, raw: string) {
    const digits = raw.replace(/\D/g, '').slice(0, SEGMENTS[index].length)
    const next = [...parts] as [string, string, string]
    next[index] = digits
    onChange(buildValue(next))
    if (digits.length === SEGMENTS[index].length && index < SEGMENTS.length - 1) {
      refs.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index: number, e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && parts[index] === '' && index > 0) {
      refs.current[index - 1]?.focus()
    }
  }

  return (
    <span className={`date-input${error ? ' has-error' : ''}`}>
      {SEGMENTS.map((segment, index) => (
        <span key={segment.placeholder} className="date-segment-wrap">
          <input
            ref={(el) => {
              refs.current[index] = el
            }}
            className="date-segment"
            style={{ width: `${segment.length + 1}ch` }}
            type="text"
            inputMode="numeric"
            maxLength={segment.length}
            placeholder={segment.placeholder}
            value={parts[index]}
            onChange={(e: ChangeEvent<HTMLInputElement>) => update(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
          />
          {index < SEGMENTS.length - 1 && <span className="date-sep">/</span>}
        </span>
      ))}
    </span>
  )
}
