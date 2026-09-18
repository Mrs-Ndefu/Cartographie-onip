import { useHouseholds } from '../../hooks/useHouseholds'
import { useLocalStats } from '../../hooks/useLocalStats'
import { StatsPanels } from './StatsPanels'
import type { AuthSession } from '../../types/agent'

interface AgentOverviewTabProps {
  session: AuthSession
}

const TODAY_LABEL = new Date().toLocaleDateString('fr-FR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

export function AgentOverviewTab({ session }: AgentOverviewTabProps) {
  const { households, isLoading } = useHouseholds()
  const stats = useLocalStats(households)

  if (isLoading) return <p className="dashboard-loading">Chargement…</p>

  const todayCount = stats.registrationCounts.today

  return (
    <div className="overview-tab">
      <div className="agent-overview-header">
        <div>
          <h2 className="agent-overview-greeting">Bonjour, {session.agent.fullName.split(' ')[0]}</h2>
          <p className="agent-overview-date">{TODAY_LABEL}</p>
        </div>
        <div className="hero-stat">
          <span className="hero-stat-value">{todayCount}</span>
          <span className="hero-stat-label">
            ménage{todayCount !== 1 ? 's' : ''} enregistré{todayCount !== 1 ? 's' : ''}
            <br />
            aujourd'hui
          </span>
        </div>
      </div>

      <StatsPanels
        registrationCounts={stats.registrationCounts}
        sexDistribution={stats.sexDistribution}
        monthlyRegistrations={stats.monthlyRegistrations}
      />
    </div>
  )
}
