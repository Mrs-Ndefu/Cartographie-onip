export type AgentRole = 'ADMIN' | 'AGENT'

export interface Agent {
  id: string
  username: string
  fullName: string
  role: AgentRole
  active: boolean
  createdAt: string
  photoDataUrl: string | null
}

export interface AuthSession {
  token: string
  expiresInMinutes: number
  agent: Agent
  loggedInAt: string // ISO — horodatage local de connexion
}
