import { useEffect, useMemo, useState } from 'react';
import { Hexagon, RefreshCw } from 'lucide-react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    XAxis,
    YAxis,
    Tooltip,
    Area,
    AreaChart,
} from 'recharts';
import { API_BASE_URL, fetchFundLookthrough, fetchHoldingsFull, fetchPortfolioSummary, fetchPortfolioExposure } from '../api/portfolioApi';
import type {
    AssetType,
    FundLookthroughResponse,
    HoldingResponse,
    PortfolioExposure,
    PortfolioSummaryResponse,
    PriceStatus,
} from '../api/types';

const ASSET_COLORS: Record<AssetType, string> = {
    STOCK: '#4F86F7',
    ETF: '#9B59B6',
    MUTUAL_FUND: '#0f766e',
    CRYPTO: '#f59e0b',
    CASH: '#F5A623',
    BANK_DEPOSIT: '#64748b',
};

const DEMO_PERFORMANCE_DATA = [
    { date: 'Jan', value: 18200 },
    { date: 'Feb', value: 18800 },
    { date: 'Mar', value: 18500 },
    { date: 'Apr', value: 19100 },
    { date: 'May', value: 19600 },
    { date: 'Jun', value: 19200 },
    { date: 'Jul', value: 20100 },
    { date: 'Aug', value: 20800 },
    { date: 'Sep', value: 20500 },
    { date: 'Oct', value: 21200 },
    { date: 'Nov', value: 21794 },
];

function formatMoney(value?: number, currency = 'USD') {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
    }).format(value ?? 0);
}

function formatPercent(value?: number | null) {
    if (value === null || value === undefined) return '—';
    return `${value.toFixed(2)}%`;
}

function getPriceStatusLabel(status: PriceStatus): string {
    switch (status) {
        case 'LIVE': return 'Live';
        case 'CACHED': return 'Cached';
        case 'DEMO': return 'Demo';
        case 'FIXED': return 'Fixed';
        case 'UNAVAILABLE': return 'Unavailable';
        default: return 'Unknown';
    }
}

export function DashboardPage() {
    const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
    const [holdings, setHoldings] = useState<HoldingResponse[]>([]);
    const [fundLookthrough, setFundLookthrough] = useState<FundLookthroughResponse | null>(null);
    const [exposure, setExposure] = useState<PortfolioExposure | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    async function refreshSummary() {
        setIsLoading(true);
        setError(null);
        try {
            const [nextSummary, nextHoldings, nextExposure] = await Promise.all([
                fetchPortfolioSummary(),
                fetchHoldingsFull(),
                fetchPortfolioExposure(true),
            ]);
            setSummary(nextSummary);
            setHoldings(nextHoldings);
            setExposure(nextExposure);
            await refreshFundLookthrough(nextSummary);
        } catch (caught) {
            setError(caught instanceof Error ? caught.message : 'Unable to load portfolio data.');
        } finally {
            setIsLoading(false);
        }
    }

    useEffect(() => {
        void refreshSummary();
    }, []);

    async function refreshFundLookthrough(nextSummary: PortfolioSummaryResponse) {
        const firstFund = nextSummary.allocations.find(
            (a) => a.assetType === 'ETF' || a.assetType === 'MUTUAL_FUND'
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
    const unpricedHoldings = summary?.unpricedHoldings ?? [];

    const dataModeLabel = useMemo(() => {
        if (!summary) return 'Loading';
        const statuses = holdings.map((h) => h.priceStatus);
        if (statuses.every((s) => s === 'LIVE')) return 'LIVE';
        if (statuses.some((s) => s === 'DEMO')) return 'DEMO';
        if (statuses.some((s) => s === 'CACHED')) return 'CACHED';
        return summary.valuationStatus;
    }, [summary, holdings]);

    const dataModeDetail = useMemo(() => {
        if (!summary) return '';
        if (dataModeLabel === 'LIVE') return 'real-time market prices';
        if (dataModeLabel === 'DEMO') return 'demo prices for training';
        if (dataModeLabel === 'CACHED') return 'cached prices, may be stale';
        return `${summary.valuationStatus} valuation`;
    }, [summary, dataModeLabel]);

    const metricCards = useMemo(
        () => [
            {
                label: 'Total Value',
                value: formatMoney(summary?.totalMarketValue, baseCurrency),
                detail: summary ? `${summary.valuationStatus} valuation` : 'Loading',
                positive: null as boolean | null,
            },
            {
                label: 'Unrealized P/L',
                value: formatMoney(summary?.totalUnrealizedGainLoss, baseCurrency),
                detail: `${formatPercent(summary?.totalUnrealizedGainLossPercent)} total return`,
                positive: (summary?.totalUnrealizedGainLoss ?? 0) >= 0,
            },
            {
                label: 'Priced Holdings',
                value: `${summary?.pricedHoldingCount ?? 0}/${summary?.holdingCount ?? 0}`,
                detail: summary?.priceAsOf
                    ? `As of ${new Date(summary.priceAsOf).toLocaleString()}`
                    : 'Fixed assets need no quote',
                positive: null,
            },
            {
                label: 'Data Mode',
                value: dataModeLabel,
                detail: dataModeDetail,
                positive: null,
            },
        ],
        [baseCurrency, summary, dataModeLabel, dataModeDetail]
    );

    const allocationChartData = useMemo(() => {
        if (allocations.length === 0) return [];
        return allocations.map((a) => ({
            name: a.ticker,
            value: a.marketValue,
            color: ASSET_COLORS[a.assetType] ?? '#BDC3C7',
        }));
    }, [allocations]);

    const totalAllocationValue = allocationChartData.reduce((sum, d) => sum + d.value, 0);

    const portfolioNotes = useMemo(() => {
        const notes: { title: string; detail: string }[] = [];

        if (allocations.length > 0) {
            const maxAllocation = allocations.reduce((max, a) =>
                    a.allocationPercent > max.allocationPercent ? a : max
                , allocations[0]);
            if (maxAllocation.allocationPercent > 40) {
                notes.push({
                    title: `${maxAllocation.ticker} concentration`,
                    detail: `${formatPercent(maxAllocation.allocationPercent)} of priced value`,
                });
            }
        }

        const demoCount = holdings.filter((h) => h.priceStatus === 'DEMO').length;
        if (demoCount > 0) {
            notes.push({
                title: 'Demo prices',
                detail: `${demoCount} holding${demoCount > 1 ? 's' : ''} use demo values.`,
            });
        }

        const unpricedCount = unpricedHoldings.length;
        if (unpricedCount > 0) {
            notes.push({
                title: 'Unpriced holdings',
                detail: `${unpricedCount} holding${unpricedCount > 1 ? 's' : ''} have no price.`,
            });
        }

        if (notes.length === 0 && summary) {
            notes.push({
                title: 'All priced',
                detail: `All ${summary.pricedHoldingCount} holdings have valid prices.`,
            });
        }

        return notes;
    }, [allocations, holdings, unpricedHoldings, summary]);

    const displayPerformanceData = DEMO_PERFORMANCE_DATA;

    const exposureWarnings = exposure?.warnings ?? [];
    const exposureItems = exposure?.items ?? [];

    return (
        <div className="dashboard-page-wrapper">
            {error && (
                <div className="metric-cards-row">
                    <div className="metric-card" style={{borderColor: '#e74c3c'}}>
                        <p className="metric-detail negative" role="alert">{error}</p>
                    </div>
                </div>
            )}

            <section className="metric-cards-row">
                {metricCards.map((card) => (
                    <div className="metric-card" key={card.label}>
                        <div className="metric-header">
                            <Hexagon size={20} className="metric-icon"/>
                            <span className="metric-label">{card.label}</span>
                        </div>
                        <p className="metric-value">{isLoading && !summary ? '—' : card.value}</p>
                        <span
                            className={`metric-detail ${
                                card.positive === true ? 'positive' : card.positive === false ? 'negative' : ''
                            }`}
                        >
                            {card.detail}
                        </span>
                    </div>
                ))}
            </section>

            <section className="charts-row">
                <div className="chart-card allocation-card">
                    <h2 className="chart-title">Asset Allocation</h2>
                    <p className="chart-subtitle">Distribution by priced market value</p>
                    {allocationChartData.length > 0 ? (
                        <div className="allocation-content">
                            <div className="donut-wrapper">
                                <ResponsiveContainer width="100%" height={200}>
                                    <PieChart>
                                        <Pie
                                            data={allocationChartData}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={55}
                                            outerRadius={90}
                                            paddingAngle={2}
                                            dataKey="value"
                                            strokeWidth={0}
                                        >
                                            {allocationChartData.map((entry) => (
                                                <Cell key={entry.name} fill={entry.color}/>
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            formatter={(value: number) => [formatMoney(value, baseCurrency), '']}
                                            contentStyle={{
                                                borderRadius: 12,
                                                border: 'none',
                                                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                            }}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                                <div className="donut-center-label">
                                    <span className="donut-value">
                                        {formatMoney(totalAllocationValue, baseCurrency)}
                                    </span>
                                    <span className="donut-sub">priced value</span>
                                </div>
                            </div>
                            <div className="allocation-legend">
                                {allocationChartData.map((item) => (
                                    <div className="legend-item" key={item.name}>
                                        <span className="legend-dot" style={{backgroundColor: item.color}}/>
                                        <span className="legend-name">{item.name}</span>
                                        <span className="legend-value">
                                            {totalAllocationValue > 0
                                                ? `${((item.value / totalAllocationValue) * 100).toFixed(0)}%`
                                                : '0%'}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="holdings-empty-state">
                            <Hexagon size={48} className="holdings-empty-icon"/>
                            <p className="holdings-empty-title">No allocation data</p>
                            <p className="holdings-empty-text">Add holdings to see the asset allocation chart.</p>
                        </div>
                    )}
                </div>

                <div className="chart-card performance-card">
                    <h2 className="chart-title">Portfolio Performance</h2>
                    <p className="chart-subtitle">
                        {summary ? `Snapshot value trend · ${getPriceStatusLabel(dataModeLabel as PriceStatus)}` : 'Snapshot value trend'}
                    </p>
                    <div className="performance-chart-wrapper">
                        <ResponsiveContainer width="100%" height={200}>
                            <AreaChart data={displayPerformanceData}>
                                <defs>
                                    <linearGradient id="perfGradient" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor="#4F86F7" stopOpacity={0.3}/>
                                        <stop offset="100%" stopColor="#4F86F7" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <XAxis
                                    dataKey="date"
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{fontSize: 12, fill: '#8a94a6'}}
                                />
                                <YAxis hide domain={['dataMin - 500', 'dataMax + 500']}/>
                                <Tooltip
                                    formatter={(value: number) => [`$${value.toLocaleString()}`, 'Value']}
                                    contentStyle={{
                                        borderRadius: 12,
                                        border: 'none',
                                        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                    }}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="value"
                                    stroke="#4F86F7"
                                    strokeWidth={2.5}
                                    fill="url(#perfGradient)"
                                    dot={{r: 4, fill: '#fff', stroke: '#4F86F7', strokeWidth: 2}}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </section>

            {exposureItems.length > 0 && (
                <section className="charts-row">
                    <div className="chart-card" style={{gridColumn: '1 / -1'}}>
                        <h2 className="chart-title">Portfolio Exposure</h2>
                        <p className="chart-subtitle">Direct + fund lookthrough positions</p>
                        {exposureWarnings.length > 0 && (
                            <div className="fund-warning-banner" style={{marginBottom: 16}}>
                                <span className="fund-warning-icon"></span>
                                <span className="fund-warning-text">{exposureWarnings.join(' ')}</span>
                            </div>
                        )}
                        <table className="holdings-ledger-table">
                            <thead>
                            <tr>
                                <th>Ticker</th>
                                <th>Type</th>
                                <th>Direct Value</th>
                                <th>Fund Lookthrough</th>
                                <th>Total Exposure</th>
                                <th>Exposure %</th>
                                <th>Sources</th>
                            </tr>
                            </thead>
                            <tbody>
                            {exposureItems.map((item) => (
                                <tr key={item.ticker}>
                                    <td className="ledger-symbol">{item.ticker}</td>
                                    <td>{item.assetType}</td>
                                    <td>{item.directMarketValue > 0 ? formatMoney(item.directMarketValue) : '—'}</td>
                                    <td>{item.fundLookthroughMarketValue > 0 ? formatMoney(item.fundLookthroughMarketValue) : '—'}</td>
                                    <td className="ledger-value">{formatMoney(item.totalExposureValue)}</td>
                                    <td>{formatPercent(item.exposurePercent * 100)}</td>
                                    <td style={{
                                        fontSize: '0.78rem',
                                        color: 'var(--muted)'
                                    }}>{item.sources.join(', ')}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            <section className="bottom-row">
                <div className="holdings-card">
                    <h2 className="section-title">Your Holdings</h2>
                    {holdings.length > 0 ? (
                        <table className="holdings-table">
                            <thead>
                            <tr>
                                <th>Symbol</th>
                                <th>Type</th>
                                <th>Qty</th>
                                <th>Price</th>
                                <th>Market Value</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {holdings.map((row) => (
                                <tr key={row.id}>
                                    <td className="symbol-cell">{row.ticker}</td>
                                    <td>{row.assetType}</td>
                                    <td>{row.quantity}</td>
                                    <td>
                                        {row.currentPrice != null
                                            ? formatMoney(row.currentPrice, baseCurrency)
                                            : formatMoney(row.averagePurchasePrice, baseCurrency)}
                                    </td>
                                    <td className="value-cell">
                                        {row.marketValue != null
                                            ? formatMoney(row.marketValue, baseCurrency)
                                            : '—'}
                                    </td>
                                    <td>
                                        <span
                                            className={`price-status-badge price-status-${row.priceStatus.toLowerCase()}`}>
                                            {getPriceStatusLabel(row.priceStatus)}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    ) : (
                        <div className="holdings-empty-state">
                            <Hexagon size={48} className="holdings-empty-icon"/>
                            <p className="holdings-empty-title">No holdings yet</p>
                            <p className="holdings-empty-text">Add your first holding to see it here.</p>
                        </div>
                    )}
                </div>

                <div className="notes-card">
                    <h2 className="section-title">Portfolio Notes</h2>
                    <div className="notes-list">
                        {portfolioNotes.map((note) => (
                            <div className="note-item" key={note.title}>
                                <Hexagon size={32} className="note-icon"/>
                                <div>
                                    <p className="note-title">{note.title}</p>
                                    <p className="note-detail">{note.detail}</p>
                                </div>
                            </div>
                        ))}
                    </div>

                    {fundLookthrough && (
                        <div style={{marginTop: 16}}>
                            <h2 className="section-title">Fund Look-through</h2>
                            <p className="note-detail">
                                {fundLookthrough.ticker} — {formatPercent(fundLookthrough.coveragePercent)} covered
                            </p>
                            {fundLookthrough.holdings.map((h) => (
                                <div key={h.ticker} className="note-item">
                                    <div>
                                        <p className="note-title">{h.ticker} — {h.displayName}</p>
                                        <p className="note-detail">{formatPercent(h.weightPercent)}</p>
                                    </div>
                                </div>
                            ))}
                            {fundLookthrough.warnings[0] && (
                                <p className="metric-detail negative">{fundLookthrough.warnings[0]}</p>
                            )}
                        </div>
                    )}

                    <div style={{marginTop: 12}}>
                        <button
                            className="theme-toggle"
                            onClick={() => void refreshSummary()}
                            style={{fontSize: '0.82rem', padding: '8px 14px'}}
                        >
                            <RefreshCw size={14} className={isLoading ? 'spin-icon' : undefined}/>
                            {isLoading ? 'Refreshing' : 'Refresh'}
                        </button>
                    </div>
                </div>
            </section>
        </div>
    );
}
