import { useDashboardStats } from '../../hooks/useDashboardStats'
import { StatsPanels } from './StatsPanels'
import type { AuthSession } from '../../types/agent'

interface OverviewTabProps {
  session: AuthSession
}

export function OverviewTab({ session }: OverviewTabProps) {
  const { stats, loading, error, refresh } = useDashboardStats(session)

  if (loading) return <p className="dashboard-loading">Chargement…</p>
  if (error) {
    return (
      <div className="dashboard-error">
        <p>{error}</p>
        <button type="button" className="toolbar-btn" onClick={() => void refresh()}>
          Réessayer
        </button>
      </div>
    )
  }
  if (!stats) return null

  return (
    <div className="overview-tab">
      <div className="stat-cards">
        <StatCard label="Total ménages" value={stats.total} />
        <StatCard label="Complets" value={stats.countComplet} tone="good" />
        <StatCard label="Brouillons" value={stats.countBrouillon} tone="muted" />
        <StatCard label="À vérifier" value={stats.countAVerifier} tone="warning" />
      </div>

      <StatsPanels
        registrationCounts={stats.registrationCounts}
        sexDistribution={stats.sexDistribution}
        monthlyRegistrations={stats.monthlyRegistrations}
      />
    </div>
  )
}

function StatCard({ label, value, tone = 'default' }: { label: string; value: number; tone?: 'default' | 'good' | 'warning' | 'muted' }) {
  return (
    <div className={`stat-card stat-card-${tone}`}>
      <span className="stat-card-value">{value}</span>
      <span className="stat-card-label">{label}</span>
    </div>
  )
}
