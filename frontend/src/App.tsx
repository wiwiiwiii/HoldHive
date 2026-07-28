import { useEffect, useMemo, useState } from 'react';
import { Hexagon, Plus, RefreshCw } from 'lucide-react';
import { Toaster } from 'react-hot-toast';
import { Cell, Pie, PieChart, Tooltip } from 'recharts';

import { API_BASE_URL, fetchFundLookthrough, fetchPortfolioSummary } from './api/portfolioApi';
import type {
  AllocationResponse,
  AssetType,
  FundLookthroughResponse,
  PortfolioSummaryResponse
} from './api/types';

const ASSET_COLORS: Record<AssetType, string> = {
  STOCK: '#1d4ed8',
  ETF: '#7c3aed',
  MUTUAL_FUND: '#0f766e',
  CRYPTO: '#f59e0b',
  CASH: '#16a34a',
  BANK_DEPOSIT: '#64748b'
};

const assetOptions: Array<{ type: AssetType; label: string; copy: string }> = [
  { type: 'STOCK', label: 'Stock', copy: 'Listed equities with market quotes.' },
  { type: 'ETF', label: 'ETF', copy: 'Exchange-traded funds; priced like stocks.' },
  { type: 'MUTUAL_FUND', label: 'Mutual fund', copy: 'NAV-based funds with delayed disclosure.' },
  { type: 'CRYPTO', label: 'Crypto', copy: 'Digital assets with demo/cache prices in MVP.' },
  { type: 'CASH', label: 'Cash', copy: 'Fixed 1.0 price, included in allocation.' },
  { type: 'BANK_DEPOSIT', label: 'Bank deposit', copy: 'Principal balance, interest later.' }
];

function formatMoney(value?: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2
  }).format(value ?? 0);
}

function formatPercent(value?: number | null) {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${value.toFixed(2)}%`;
}

export function App() {
  const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
  const [fundLookthrough, setFundLookthrough] = useState<FundLookthroughResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function refreshSummary() {
    setIsLoading(true);
    setError(null);

    try {
      const nextSummary = await fetchPortfolioSummary();
      setSummary(nextSummary);
      await refreshFundLookthrough(nextSummary);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to load portfolio summary.');
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void refreshSummary();
  }, []);

  async function refreshFundLookthrough(nextSummary: PortfolioSummaryResponse) {
    const firstFund = nextSummary.allocations.find((allocation) =>
      allocation.assetType === 'ETF' || allocation.assetType === 'MUTUAL_FUND'
    );

    if (!firstFund) {
      setFundLookthrough(null);
      return;
    }

    try {
      const nextLookthrough = await fetchFundLookthrough(firstFund.holdingId);
      setFundLookthrough(nextLookthrough);
    } catch {
      setFundLookthrough(null);
    }
  }

  const allocations = summary?.allocations ?? [];
  const baseCurrency = summary?.baseCurrency ?? 'USD';
  const metricCards = useMemo(
    () => [
      {
        label: 'Portfolio Value',
        value: formatMoney(summary?.totalMarketValue, baseCurrency),
        detail: summary ? `${summary.valuationStatus} valuation` : 'Loading summary'
      },
      {
        label: 'Unrealized P/L',
        value: formatMoney(summary?.totalUnrealizedGainLoss, baseCurrency),
        detail: `${formatPercent(summary?.totalUnrealizedGainLossPercent)} total return`
      },
      {
        label: 'Priced Holdings',
        value: `${summary?.pricedHoldingCount ?? 0}/${summary?.holdingCount ?? 0}`,
        detail: summary?.priceAsOf ? `As of ${new Date(summary.priceAsOf).toLocaleString()}` : 'Fixed assets need no quote'
      }
    ],
    [baseCurrency, summary]
  );

  return (
    <main className="app-shell">
      <section className="hero-card" aria-labelledby="dashboard-title">
        <div className="brand-row">
          <div className="brand-mark" aria-hidden="true">
            <Hexagon size={28} />
          </div>
          <div>
            <p className="eyebrow">HoldHive multi-asset workspace</p>
            <h1 id="dashboard-title">HoldHive Portfolio Dashboard</h1>
          </div>
        </div>

        <p className="hero-copy">
          Track stocks, ETFs, mutual funds, crypto, cash and bank deposits in one
          portfolio view. Backend API base URL: <code>{API_BASE_URL}</code>
        </p>

        {error ? <p className="inline-alert" role="alert">{error}</p> : null}

        <div className="action-row">
          <button className="primary-button" type="button">
            <Plus size={18} />
            Add holding
          </button>
          <button className="secondary-button" type="button" onClick={() => void refreshSummary()}>
            <RefreshCw size={18} className={isLoading ? 'spin-icon' : undefined} />
            {isLoading ? 'Refreshing' : 'Refresh'}
          </button>
        </div>
      </section>

      <section className="metric-grid" aria-label="Portfolio metrics">
        {metricCards.map((card) => (
          <article className="metric-card" key={card.label}>
            <p>{card.label}</p>
            <strong>{card.value}</strong>
            <span>{card.detail}</span>
          </article>
        ))}
      </section>

      <section className="dashboard-grid" aria-label="Portfolio analysis">
        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Allocation</p>
              <h2>Asset mix</h2>
            </div>
            <span className="status-pill">{summary?.valuationStatus ?? 'LOADING'}</span>
          </div>

          {allocations.length > 0 ? (
            <div className="allocation-layout">
              <PieChart width={250} height={250}>
                <Pie
                  data={allocations}
                  dataKey="marketValue"
                  nameKey="ticker"
                  cx="50%"
                  cy="50%"
                  innerRadius={68}
                  outerRadius={104}
                  paddingAngle={3}
                >
                  {allocations.map((allocation) => (
                    <Cell
                      key={allocation.holdingId}
                      fill={ASSET_COLORS[allocation.assetType]}
                    />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatMoney(Number(value), baseCurrency)} />
              </PieChart>

              <div className="allocation-list">
                {allocations.map((allocation) => (
                  <AllocationRow
                    allocation={allocation}
                    currency={baseCurrency}
                    key={allocation.holdingId}
                  />
                ))}
              </div>
            </div>
          ) : (
            <p className="empty-copy">No priced holdings yet. Add an asset to populate the chart.</p>
          )}
        </article>

        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Add flow</p>
              <h2>Supported assets</h2>
            </div>
          </div>

          <div className="asset-option-grid">
            {assetOptions.map((option) => (
              <div className="asset-option" key={option.type}>
                <span style={{ backgroundColor: ASSET_COLORS[option.type] }} />
                <div>
                  <strong>{option.label}</strong>
                  <p>{option.copy}</p>
                </div>
              </div>
            ))}
          </div>

          <p className="fund-note">
            Funds can contain underlying stocks. HoldHive keeps fund valuation separate,
            then uses look-through analysis later to show overlap without creating duplicate stock holdings.
          </p>

          {fundLookthrough ? (
            <div className="lookthrough-card" aria-label="Fund look-through">
              <div className="lookthrough-heading">
                <strong>{fundLookthrough.ticker} Top holdings</strong>
                <span>{formatPercent(fundLookthrough.coveragePercent)} covered</span>
              </div>
              <div className="lookthrough-list">
                {fundLookthrough.holdings.map((holding) => (
                  <div className="lookthrough-row" key={holding.ticker}>
                    <div>
                      <strong>{holding.ticker}</strong>
                      <small>{holding.displayName}</small>
                    </div>
                    <span>{formatPercent(holding.weightPercent)}</span>
                  </div>
                ))}
              </div>
              <small className="lookthrough-warning">{fundLookthrough.warnings[0]}</small>
            </div>
          ) : null}
        </article>
      </section>

      <Toaster position="top-right" />
    </main>
  );
}

function AllocationRow({
  allocation,
  currency
}: {
  allocation: AllocationResponse;
  currency: string;
}) {
  return (
    <div className="allocation-row">
      <span
        className="allocation-dot"
        style={{ backgroundColor: ASSET_COLORS[allocation.assetType] }}
      />
      <div>
        <strong>{allocation.ticker}</strong>
        <small>{allocation.assetType}</small>
      </div>
      <div className="allocation-values">
        <strong>{formatMoney(allocation.marketValue, currency)}</strong>
        <small>{formatPercent(allocation.allocationPercent)}</small>
      </div>
    </div>
  );
}
