import type { RegistrationCounts, MonthlyCount } from '../../types/dashboard'

interface StatsPanelsProps {
  registrationCounts: RegistrationCounts
  sexDistribution: Record<string, number>
  monthlyRegistrations: MonthlyCount[]
}

export function StatsPanels({ registrationCounts, sexDistribution, monthlyRegistrations }: StatsPanelsProps) {
  const male = sexDistribution.M ?? 0
  const female = sexDistribution.F ?? 0
  const totalPeople = Math.max(male + female, 1)
  const malePct = Math.round((male / totalPeople) * 100)
  const femalePct = 100 - malePct
  const maxMonthly = Math.max(...monthlyRegistrations.map((m) => m.count), 1)

  return (
    <>
      <div className="dashboard-cards-row">
        <section className="dashboard-card">
          <h2>Enregistrements</h2>
          <div className="registration-counts">
            <LabeledCount label="Aujourd'hui" value={registrationCounts.today} />
            <LabeledCount label="Ce mois" value={registrationCounts.thisMonth} />
            <LabeledCount label="Cette année" value={registrationCounts.thisYear} />
          </div>
        </section>

        <section className="dashboard-card">
          <h2>Répartition par sexe</h2>
          <div className="sex-bar" role="img" aria-label={`Hommes ${malePct}%, Femmes ${femalePct}%`}>
            <div className="sex-bar-segment sex-bar-male" style={{ width: `${malePct}%` }} />
            <div className="sex-bar-segment sex-bar-female" style={{ width: `${femalePct}%` }} />
          </div>
          <div className="sex-legend">
            <span className="legend-item">
              <span className="legend-swatch legend-swatch-male" /> Hommes : {male} ({malePct}%)
            </span>
            <span className="legend-item">
              <span className="legend-swatch legend-swatch-female" /> Femmes : {female} ({femalePct}%)
            </span>
          </div>
        </section>
      </div>

      <section className="dashboard-card">
        <h2>Enregistrements par mois (12 derniers mois)</h2>
        <div className="monthly-chart">
          {monthlyRegistrations.map((m) => (
            <div key={m.label} className="monthly-bar-col" title={`${m.label} : ${m.count}`}>
              <div className="monthly-bar-track">
                <div
                  className="monthly-bar-fill"
                  style={{ height: `${Math.max((m.count / maxMonthly) * 100, m.count > 0 ? 4 : 0)}%` }}
                />
              </div>
              <span className="monthly-bar-label">{m.label.slice(0, 5)}</span>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

function LabeledCount({ label, value }: { label: string; value: number }) {
  return (
    <div className="labeled-count">
      <span className="labeled-count-value">{value}</span>
      <span className="labeled-count-label">{label}</span>
    </div>
  )
}
