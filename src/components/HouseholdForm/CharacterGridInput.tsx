import { useRef } from 'react'
import type { ChangeEvent, KeyboardEvent } from 'react'

interface CharacterGridInputProps {
  length: number
  value: string
  onChange: (value: string) => void
  pattern?: RegExp
  error?: string
  ariaLabel?: string
}

export function CharacterGridInput({
  length,
  value,
  onChange,
  pattern = /[A-Z0-9]/,
  error,
  ariaLabel,
}: CharacterGridInputProps) {
  const inputsRef = useRef<(HTMLInputElement | null)[]>([])
  const chars = Array.from({ length }, (_, i) => value[i] ?? '')

  function setChar(index: number, char: string) {
    const next = chars.slice()
    next[index] = char
    onChange(next.join('').replace(/\s+$/, ''))
  }

  function handleChange(index: number, e: ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value.toUpperCase().slice(-1)
    const char = raw && pattern.test(raw) ? raw : ''
    setChar(index, char)
    if (char && index < length - 1) {
      inputsRef.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index: number, e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !chars[index] && index > 0) {
      inputsRef.current[index - 1]?.focus()
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputsRef.current[index - 1]?.focus()
    } else if (e.key === 'ArrowRight' && index < length - 1) {
      inputsRef.current[index + 1]?.focus()
    }
  }

  return (
    <div className={`char-grid${error ? ' has-error' : ''}`} role="group" aria-label={ariaLabel}>
      {chars.map((char, index) => (
        <input
          key={index}
          ref={(el) => {
            inputsRef.current[index] = el
          }}
          className="char-box"
          type="text"
          inputMode="text"
          maxLength={1}
          value={char}
          onChange={(e) => handleChange(index, e)}
          onKeyDown={(e) => handleKeyDown(index, e)}
        />
      ))}
    </div>
  )
}
