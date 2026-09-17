import { CharacterGridInput } from './CharacterGridInput'
import { CODE_MENAGE_LENGTH } from '../../types/household'

interface CodeMenageInputProps {
  value: string
  onChange: (value: string) => void
  error?: string
}

export function CodeMenageInput({ value, onChange, error }: CodeMenageInputProps) {
  return (
    <CharacterGridInput
      length={CODE_MENAGE_LENGTH}
      value={value}
      onChange={onChange}
      pattern={/[A-Z0-9]/}
      error={error}
      ariaLabel="Code Ménage"
    />
  )
}
