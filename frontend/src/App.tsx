import { Hexagon, Plus, RefreshCcw } from 'lucide-react';
import { Toaster } from 'react-hot-toast';

import { API_BASE_URL } from './api/portfolioApi';

const starterCards = [
  { label: 'Portfolio Value', value: '$0.00', detail: 'Ready for holdings' },
  { label: 'Unrealized P/L', value: '$0.00', detail: 'Calculated by backend' },
  { label: 'Holdings', value: '0', detail: 'Add your first position' }
];

export function App() {
  return (
    <main className="app-shell">
      <section className="hero-card" aria-labelledby="dashboard-title">
        <div className="brand-row">
          <div className="brand-mark" aria-hidden="true">
            <Hexagon size={28} />
          </div>
          <div>
            <p className="eyebrow">HoldHive starter workspace</p>
            <h1 id="dashboard-title">Portfolio dashboard skeleton</h1>
          </div>
        </div>

        <p className="hero-copy">
          This scaffold is ready for the team to connect holdings, valuation, pricing,
          and chart features. Backend API base URL: <code>{API_BASE_URL}</code>
        </p>

        <div className="action-row">
          <button className="primary-button" type="button">
            <Plus size={18} />
            Add holding
          </button>
          <button className="secondary-button" type="button">
            <RefreshCcw size={18} />
            Refresh
          </button>
        </div>
      </section>

      <section className="metric-grid" aria-label="Portfolio starter metrics">
        {starterCards.map((card) => (
          <article className="metric-card" key={card.label}>
            <p>{card.label}</p>
            <strong>{card.value}</strong>
            <span>{card.detail}</span>
          </article>
        ))}
      </section>

      <Toaster position="top-right" />
    </main>
  );
}
