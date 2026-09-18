import { useState } from 'react'
import { OverviewTab } from './OverviewTab'
import { HouseholdsTab } from './HouseholdsTab'
import { AgentsTab } from './AgentsTab'
import { AgentOverviewTab } from './AgentOverviewTab'
import { JournalTab } from './JournalTab'
import type { AuthSession } from '../../types/agent'
import type { Household } from '../../types/household'
import './Dashboard.css'

interface DashboardProps {
  session: AuthSession
  onClose: () => void
  onEditHousehold: (household: Household) => void
  onToast: (message: string) => void
}

type Tab = 'overview' | 'households' | 'agents' | 'journal'

const ADMIN_TABS: { id: Tab; label: string }[] = [
  { id: 'overview', label: "Vue d'ensemble" },
  { id: 'households', label: 'Ménages' },
  { id: 'agents', label: 'Agents' },
]

// Un compte AGENT voit ses propres statistiques (calculées localement, hors ligne) et son
// journal du jour — pas les stats globales ni la gestion des autres agents, réservées aux ADMIN.
const AGENT_TABS: { id: Tab; label: string }[] = [
  { id: 'overview', label: "Vue d'ensemble" },
  { id: 'journal', label: 'Journal du jour' },
]

export function Dashboard({ session, onClose, onEditHousehold, onToast }: DashboardProps) {
  const isAdmin = session.agent.role === 'ADMIN'
  const tabs = isAdmin ? ADMIN_TABS : AGENT_TABS
  const [tab, setTab] = useState<Tab>('overview')

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>{isAdmin ? 'Tableau de bord' : 'Mon tableau de bord'}</h1>
        <button type="button" className="toolbar-btn" onClick={onClose}>
          ← Retour à la carte
        </button>
      </header>

      <nav className="dashboard-tabs">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            className={`dashboard-tab${tab === t.id ? ' dashboard-tab-active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </nav>

      <div className="dashboard-content">
        {tab === 'overview' && (isAdmin ? <OverviewTab session={session} /> : <AgentOverviewTab session={session} />)}
        {tab === 'households' && isAdmin && <HouseholdsTab session={session} />}
        {tab === 'agents' && isAdmin && <AgentsTab session={session} />}
        {tab === 'journal' && !isAdmin && <JournalTab session={session} onEdit={onEditHousehold} onToast={onToast} />}
      </div>
    </div>
  )
}
