import { useEffect, useMemo, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
} from 'recharts';
import { fetchAnalysisInsightsFull, fetchPortfolioExposure } from '../api/portfolioApi';
import type {
    PortfolioAnalysisFacts,
    PortfolioExposure,
    AnalysisAssetType,
} from '../api/types';

const ASSET_COLORS: Record<string, string> = {
    STOCK: '#4F86F7',
    ETF: '#9B59B6',
    MUTUAL_FUND: '#0f766e',
    FUND: '#9B59B6',
    CRYPTO: '#f59e0b',
    CASH: '#F5A623',
    BANK_DEPOSIT: '#64748b',
    TERM_DEPOSIT: '#64748b',
    UNPRICED: '#BDC3C7',
};

const ASSET_LABELS: Record<string, string> = {
    STOCK: 'Stocks',
    ETF: 'ETF',
    MUTUAL_FUND: 'Mutual Fund',
    FUND: 'Fund',
    CRYPTO: 'Crypto',
    CASH: 'Cash',
    BANK_DEPOSIT: 'Bank Deposit',
    TERM_DEPOSIT: 'Term Deposit',
    UNPRICED: 'Unpriced',
};

const RISK_LEVEL_LABELS: Record<string, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
};

const RISK_LEVEL_COLORS: Record<string, string> = {
    LOW: '#27ae60',
    MEDIUM: '#f6b33b',
    HIGH: '#e74c3c',
};

function hexagonPoints(cx: number, cy: number, r: number): string {
    const pts = [];
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const rad = (angle * Math.PI) / 180;
        pts.push(`${cx + r * Math.cos(rad)},${cy + r * Math.sin(rad)}`);
    }
    return pts.join(' ');
}

function parseAiText(text: string): { title: string; content: string }[] {
    const sections = text.split(/###\s+/).filter((s) => s.trim());
    return sections.map((section) => {
        const lines = section.split('\n');
        const title = lines[0]?.trim() ?? '';
        const content = lines.slice(1).join('\n').trim();
        return { title, content };
    });
}

function renderBoldText(text: string) {
    const parts = text.split(/(\*\*[^*]+\*\*)/g);
    return parts.map((part, i) => {
        if (part.startsWith('**') && part.endsWith('**')) {
            return <strong key={i} style={{ color: 'var(--ink)', fontWeight: 600 }}>{part.slice(2, -2)}</strong>;
        }
        return <span key={i}>{part}</span>;
    });
}

function renderSectionContent(content: string) {
    const lines = content.split('\n').filter((l) => l.trim());
    const hasBullets = lines.some((l) => l.trim().startsWith('- '));

    if (hasBullets) {
        return (
            <ul style={{ margin: 0, paddingLeft: 0, listStyle: 'none' }}>
                {lines.map((line, i) => {
                    const text = line.replace(/^-\s*/, '');
                    return (
                        <li key={i} style={{
                            fontSize: '0.92rem',
                            color: 'var(--muted-darker)',
                            lineHeight: 1.7,
                            marginBottom: 8,
                            display: 'flex',
                            gap: 8,
                        }}>
                            <span style={{ color: 'var(--honey)', fontWeight: 700, flexShrink: 0 }}>•</span>
                            <span>{renderBoldText(text)}</span>
                        </li>
                    );
                })}
            </ul>
        );
    }

    return (
        <p style={{
            fontSize: '0.92rem',
            color: 'var(--muted-darker)',
            lineHeight: 1.7,
            margin: 0,
        }}>{renderBoldText(content)}</p>
    );
}

function formatMoney(value: number): string {
    if (value >= 1000) {
        return `$${(value / 1000).toFixed(1)}k`;
    }
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
    }).format(value);
}

function formatMoneyFull(value: number): string {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
    }).format(value);
}

function formatPercent(value: number): string {
    return `${value.toFixed(1)}%`;
}

interface AnalysisPageProps {
    isDark?: boolean;
    refreshTrigger?: number;
}

export function AnalysisPage({ isDark, refreshTrigger = 0 }: AnalysisPageProps) {
    const [facts, setFacts] = useState<PortfolioAnalysisFacts | null>(null);
    const [exposure, setExposure] = useState<PortfolioExposure | null>(null);
    const [aiText, setAiText] = useState('');
    const [isAiStreaming, setIsAiStreaming] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [manualRefreshKey, setManualRefreshKey] = useState(0);

    useEffect(() => {
        let cancelled = false;
        const controller = new AbortController();
        async function load() {
            setIsLoading(true);
            setAiText('');
            setIsAiStreaming(true);
            try {
                const [analysisFacts, nextExposure] = await Promise.all([
                    fetchAnalysisInsightsFull({
                        signal: controller.signal,
                        onToken: (token) => {
                            if (!cancelled) {
                                setAiText((prev) => prev + token);
                            }
                        },
                        onDone: () => {
                            if (!cancelled) {
                                setIsAiStreaming(false);
                            }
                        },
                    }),
                    fetchPortfolioExposure(true),
                ]);
                if (!cancelled) {
                    setFacts(analysisFacts);
                    setExposure(nextExposure);
                }
            } catch {
                // keep empty on failure
                if (!cancelled) {
                    setIsAiStreaming(false);
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }
        load();
        return () => {
            cancelled = true;
            controller.abort();
        };
    }, [refreshTrigger, manualRefreshKey]);

    const overview = facts?.overview;
    const concentration = facts?.concentration;
    const fundOverlap = facts?.fundOverlap;
    const sectorExposure = facts?.sectorExposure;
    const profitLoss = facts?.profitLoss;

    const assetTypeData = useMemo(() => {
        if (!overview || overview.allocations.length === 0) return [];
        return overview.allocations.map((a) => ({
            name: ASSET_LABELS[a.assetType] ?? a.assetType,
            value: a.marketValue,
            percent: a.percent,
            color: ASSET_COLORS[a.assetType] ?? '#BDC3C7',
        }));
    }, [overview]);

    const totalValue = overview?.totalMarketValue ?? 0;

    const maxHolding = concentration?.topHoldings?.[0] ?? null;
    const concentrationPercent = maxHolding?.percentOfPortfolio ?? 0;
    const isConcentrationAlert = concentrationPercent > 40;

    const overlapWarnings = useMemo(() => {
        const warnings: string[] = [];
        if (fundOverlap) {
            for (const fund of fundOverlap.funds) {
                for (const stock of fund.overlapStocks) {
                    warnings.push(`${stock.ticker} appears both as direct holding and inside ${fund.fundTicker} holdings.`);
                }
            }
            for (const fund of fundOverlap.unavailableFunds) {
                warnings.push(`No fund lookthrough disclosure is available for ${fund.fundTicker}.`);
            }
        }
        return warnings;
    }, [fundOverlap]);

    const reviewNotes = useMemo(() => {
        const notes: { title: string; detail: string; color: string }[] = [];

        if (fundOverlap && fundOverlap.unavailableFunds.length > 0) {
            notes.push({
                title: 'Price transparency',
                detail: `${fundOverlap.unavailableFunds.length} fund${fundOverlap.unavailableFunds.length > 1 ? 's' : ''} use demo values.`,
                color: '#f6b33b',
            });
        }

        if (profitLoss && profitLoss.missingCostBasisTickers.length > 0) {
            notes.push({
                title: 'Unpriced holdings',
                detail: `${profitLoss.missingCostBasisTickers.length} holding${profitLoss.missingCostBasisTickers.length > 1 ? 's' : ''} have no price.`,
                color: '#e74c3c',
            });
        }

        if (concentration && isConcentrationAlert && maxHolding) {
            notes.push({
                title: `${maxHolding.ticker} concentration`,
                detail: `${formatPercent(concentrationPercent)} of priced value. Consider diversifying.`,
                color: '#e74c3c',
            });
        }

        if (overlapWarnings.length > 0) {
            notes.push({
                title: 'Fund overlap warning',
                detail: `${fundOverlap?.totalOverlapPercentOfPortfolio ? formatPercent(fundOverlap.totalOverlapPercentOfPortfolio * 100) : ''} of portfolio has overlapping exposure.`,
                color: '#e74c3c',
            });
        }

        if (profitLoss) {
            const cashAlloc = overview?.allocations.find((a) => a.assetType === 'CASH' || a.assetType === 'BANK_DEPOSIT');
            if (cashAlloc && cashAlloc.percent > 20) {
                notes.push({
                    title: 'Diversification',
                    detail: `Cash reserve is ${formatPercent(cashAlloc.percent * 100)} of priced value.`,
                    color: '#4F86F7',
                });
            }
        }

        notes.push({
            title: 'No advice',
            detail: 'Signals are explanatory only.',
            color: '#9B59B6',
        });

        return notes;
    }, [fundOverlap, profitLoss, concentration, maxHolding, concentrationPercent, isConcentrationAlert, overlapWarnings, overview]);

    const sectorData = useMemo(() => {
        if (!sectorExposure || sectorExposure.sectors.length === 0) return [];
        return sectorExposure.sectors.map((s) => ({
            name: s.sector,
            value: s.effectiveMarketValue,
            percent: s.effectivePercentOfPortfolio,
        }));
    }, [sectorExposure]);

    const topSectors = sectorData.slice(0, 5);

    const pnlSorted = useMemo(() => {
        if (!profitLoss) return [];
        return [...profitLoss.holdings].sort((a, b) => (b.unrealizedPnlPercent ?? 0) - (a.unrealizedPnlPercent ?? 0));
    }, [profitLoss]);

    return (
        <div className="analysis-page-wrapper">
            <section className="analysis-page-section">
                <div className="analysis-refresh-card">
                    <div>
                        <p className="analysis-refresh-title">Portfolio analysis</p>
                        <p className="analysis-refresh-detail">
                            Refreshes automatically after holdings change. Use manual refresh after market data changes.
                        </p>
                    </div>
                    <button
                        type="button"
                        className="analysis-refresh-button"
                        disabled={isLoading || isAiStreaming}
                        onClick={() => setManualRefreshKey((key) => key + 1)}
                    >
                        <RefreshCw size={15} className={isLoading || isAiStreaming ? 'spin-icon' : undefined}/>
                        {isLoading || isAiStreaming ? 'Updating' : 'Refresh analysis'}
                    </button>
                </div>
            </section>

            {overlapWarnings.length > 0 && (
                <section className="analysis-page-section">
                    <div className="analysis-card">
                        {overlapWarnings.map((w, i) => (
                            <div key={i} className="fund-warning-banner" style={{ marginBottom: i < overlapWarnings.length - 1 ? 12 : 0, display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                                <svg width="18" height="18" viewBox="0 0 20 20" style={{ flexShrink: 0, marginTop: 1 }}>
                                    <polygon
                                        points="10,2 17.66,6.5 17.66,13.5 10,18 2.34,13.5 2.34,6.5"
                                        fill="none"
                                        stroke="#f6b33b"
                                        strokeWidth="1.5"
                                    />
                                </svg>
                                <span className="fund-warning-text">{w}</span>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            <section className="analysis-top-row">
                <div className="analysis-card">
                    <h2 className="analysis-card-title">Allocation X-Ray</h2>
                    {assetTypeData.length > 0 ? (
                        <div className="allocation-xray-content">
                            <div className="donut-wrapper">
                                <ResponsiveContainer width="100%" height={220}>
                                    <PieChart>
                                        <Pie
                                            data={assetTypeData}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={60}
                                            outerRadius={100}
                                            paddingAngle={2}
                                            dataKey="value"
                                            strokeWidth={0}
                                        >
                                            {assetTypeData.map((entry) => (
                                                <Cell key={entry.name} fill={entry.color}/>
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            formatter={(value: number, name: string) => [
                                                `${formatMoneyFull(value)} (${assetTypeData.find((d) => d.name === name)?.percent.toFixed(1)}%)`,
                                                name,
                                            ]}
                                            contentStyle={{
                                                borderRadius: 12,
                                                border: 'none',
                                                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                            }}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                                <div className="donut-center-label">
                                    <span className="donut-value">{formatMoney(totalValue)}</span>
                                    <span className="donut-sub">priced value</span>
                                </div>
                            </div>
                            <div className="allocation-legend">
                                {assetTypeData.map((item) => (
                                    <div className="legend-item" key={item.name}>
                                        <span className="legend-dot" style={{
                                            backgroundColor: item.color,
                                            width: 12,
                                            height: 12,
                                            borderRadius: 3,
                                            flexShrink: 0,
                                        }}/>
                                        <span className="legend-name">{item.name}</span>
                                        <span className="legend-value">{formatPercent(item.percent)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="holdings-empty-state">
                            <p className="holdings-empty-title">No allocation data</p>
                            <p className="holdings-empty-text">Add holdings to see the allocation breakdown.</p>
                        </div>
                    )}
                </div>

                <div className="analysis-card">
                    <h2 className="analysis-card-title">Concentration Check</h2>
                    {maxHolding ? (
                        <div className="concentration-content">
                            <div className="concentration-hex">
                                <svg width="140" height="140" viewBox="0 0 140 140">
                                    <polygon
                                        points={hexagonPoints(70, 70, 62)}
                                        fill={isDark ? '#1a1810' : '#fff9ed'}
                                        stroke={isConcentrationAlert ? '#e74c3c' : '#F5A623'}
                                        strokeWidth="3"
                                    />
                                </svg>
                                <div className="concentration-hex-label">
                                    <span className="concentration-value">{formatPercent(concentrationPercent)}</span>
                                    <span className="concentration-sub">largest holding</span>
                                </div>
                            </div>
                            <div className="concentration-info">
                                <p className="concentration-status"
                                   style={{color: isConcentrationAlert ? '#e74c3c' : '#27ae60'}}>
                                    {isConcentrationAlert ? 'Above 40% alert line' : 'Below 40% alert line'}
                                </p>
                                <p className="concentration-detail">
                                    {isConcentrationAlert
                                        ? `${maxHolding.ticker} is ${formatPercent(concentrationPercent)} of your portfolio. Consider diversifying.`
                                        : `${maxHolding.ticker} at ${formatPercent(concentrationPercent)}. Keep monitoring after new holdings.`}
                                </p>
                                {concentration && (
                                    <p className="concentration-detail" style={{marginTop: 8}}>
                                        HHI: {concentration.hhi.toFixed(3)} · Risk: <span
                                        style={{color: RISK_LEVEL_COLORS[concentration.riskLevel]}}>{RISK_LEVEL_LABELS[concentration.riskLevel]}</span>
                                    </p>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="holdings-empty-state">
                            <p className="holdings-empty-title">No concentration data</p>
                            <p className="holdings-empty-text">Add holdings to check concentration.</p>
                        </div>
                    )}
                </div>
            </section>

            {sectorExposure && sectorExposure.sectors.length > 0 && (
                <section className="analysis-page-section">
                    <div className="analysis-card">
                        <h2 className="analysis-card-title">Sector Exposure</h2>
                        <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24}}>
                            <div>
                                {topSectors.map((s) => (
                                    <div key={s.name}
                                         style={{display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12}}>
                                        <div style={{
                                            width: 80,
                                            fontSize: '0.82rem',
                                            color: 'var(--muted-darker)',
                                            flexShrink: 0,
                                        }}>{s.name}</div>
                                        <div style={{
                                            flex: 1,
                                            height: 8,
                                            background: 'var(--line)',
                                            borderRadius: 4,
                                            overflow: 'hidden'
                                        }}>
                                            <div style={{
                                                width: `${Math.min(s.percent, 100)}%`,
                                                height: '100%',
                                                background: 'var(--honey)',
                                                borderRadius: 4,
                                            }}/>
                                        </div>
                                        <div style={{
                                            width: 48,
                                            textAlign: 'right',
                                            fontSize: '0.82rem',
                                            fontWeight: 600,
                                            color: 'var(--ink)',
                                        }}>{formatPercent(s.percent)}</div>
                                    </div>
                                ))}
                            </div>
                            <div>
                                <p style={{fontSize: '0.82rem', color: 'var(--muted)', margin: '0 0 8px'}}>
                                    Sector HHI: {sectorExposure.sectorHhi.toFixed(3)} ·{' '}
                                    <span style={{color: RISK_LEVEL_COLORS[sectorExposure.sectorRiskLevel]}}>
                                        {RISK_LEVEL_LABELS[sectorExposure.sectorRiskLevel]} risk
                                    </span>
                                </p>
                                {sectorExposure.topSector && (
                                    <p style={{fontSize: '0.82rem', color: 'var(--muted)', margin: 0}}>
                                        Top
                                        sector: {sectorExposure.topSector} ({formatPercent(sectorExposure.topSectorPercent)})
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>
                </section>
            )}

            {profitLoss && profitLoss.holdings.length > 0 && (
                <section className="analysis-page-section">
                    <div className="analysis-card">
                        <h2 className="analysis-card-title">Profit & Loss</h2>
                        <table className="holdings-ledger-table">
                            <thead>
                            <tr>
                                <th>Ticker</th>
                                <th>Market Value</th>
                                <th>Cost Basis</th>
                                <th>Unrealized P/L</th>
                                <th>Return %</th>
                            </tr>
                            </thead>
                            <tbody>
                            {pnlSorted.map((p) => (
                                <tr key={p.ticker}>
                                    <td className="ledger-symbol">{p.ticker}</td>
                                    <td>{formatMoneyFull(p.marketValue)}</td>
                                    <td>{formatMoneyFull(p.costBasis)}</td>
                                    <td style={{color: p.unrealizedPnl >= 0 ? '#27ae60' : '#e74c3c', fontWeight: 600}}>
                                        {p.unrealizedPnl >= 0 ? '+' : ''}{formatMoneyFull(p.unrealizedPnl)}
                                    </td>
                                    <td style={{color: (p.unrealizedPnlPercent ?? 0) >= 0 ? '#27ae60' : '#e74c3c'}}>
                                        {p.unrealizedPnlPercent != null ? `${p.unrealizedPnlPercent >= 0 ? '+' : ''}${p.unrealizedPnlPercent.toFixed(2)}%` : '—'}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {profitLoss.totalUnrealizedPnl !== undefined && (
                            <div style={{
                                marginTop: 16,
                                paddingTop: 16,
                                borderTop: '1px solid var(--line)',
                                display: 'flex',
                                justifyContent: 'space-between',
                                fontSize: '0.88rem',
                            }}>
                                <span style={{color: 'var(--muted)'}}>Total P/L</span>
                                <span style={{
                                    color: profitLoss.totalUnrealizedPnl >= 0 ? '#27ae60' : '#e74c3c',
                                    fontWeight: 700,
                                }}>
                                    {profitLoss.totalUnrealizedPnl >= 0 ? '+' : ''}{formatMoneyFull(profitLoss.totalUnrealizedPnl)}
                                    {profitLoss.totalUnrealizedPnlPercent != null && ` (${profitLoss.totalUnrealizedPnlPercent >= 0 ? '+' : ''}${profitLoss.totalUnrealizedPnlPercent.toFixed(2)}%)`}
                                </span>
                            </div>
                        )}
                    </div>
                </section>
            )}

            {aiText && (
                <section className="analysis-page-section">
                    <div className="analysis-card">
                        <h2 className="analysis-card-title">
                            AI Insights
                            {isAiStreaming && <span className="ai-streaming-indicator">Generating...</span>}
                        </h2>
                        <div style={{display: 'flex', flexDirection: 'column', gap: 20}}>
                            {parseAiText(aiText).map((section, i) => (
                                <div key={i} style={{
                                    background: 'var(--honey-soft)',
                                    borderRadius: 14,
                                    padding: '16px 20px',
                                    border: '1px solid #f5e6c8',
                                }}>
                                    <div style={{
                                        display: 'inline-block',
                                        background: 'var(--honey)',
                                        color: '#fff',
                                        fontSize: '0.78rem',
                                        fontWeight: 700,
                                        padding: '4px 14px',
                                        borderRadius: 999,
                                        marginBottom: 10,
                                        letterSpacing: '0.02em',
                                    }}>{section.title}</div>
                                    {renderSectionContent(section.content)}
                                </div>
                            ))}
                        </div>
                    </div>
                </section>
            )}

            <section className="analysis-page-section">
                <div className="analysis-card">
                    <h2 className="analysis-card-title">Review Notes</h2>
                    <div className="review-notes-list">
                        {reviewNotes.map((note) => (
                            <div className="review-note-item" key={note.title}>
                                <svg width="40" height="40" viewBox="0 0 40 40" className="review-note-icon">
                                <polygon
                                        points={hexagonPoints(20, 20, 16)}
                                        fill="none"
                                        stroke={note.color}
                                        strokeWidth="2"
                                    />
                                </svg>
                                <div>
                                    <p className="review-note-title">{note.title}</p>
                                    <p className="review-note-detail">{note.detail}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </div>
    );
}
