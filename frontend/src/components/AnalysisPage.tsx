import { useEffect, useMemo, useState } from 'react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
} from 'recharts';
import { fetchHoldingsFull, fetchPortfolioSummary } from '../api/portfolioApi';
import type { AssetType, HoldingResponse, PortfolioSummaryResponse } from '../api/types';

const ASSET_COLORS: Record<AssetType, string> = {
    STOCK: '#4F86F7',
    ETF: '#9B59B6',
    MUTUAL_FUND: '#0f766e',
    CRYPTO: '#f59e0b',
    CASH: '#F5A623',
    BANK_DEPOSIT: '#64748b',
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

function formatMoney(value: number): string {
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
}

export function AnalysisPage({ isDark }: AnalysisPageProps) {
    const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
    const [holdings, setHoldings] = useState<HoldingResponse[]>([]);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const [nextSummary, nextHoldings] = await Promise.all([
                    fetchPortfolioSummary(),
                    fetchHoldingsFull(),
                ]);
                if (!cancelled) {
                    setSummary(nextSummary);
                    setHoldings(nextHoldings);
                }
            } catch {
                // keep empty on failure
            }
        }
        load();
        return () => { cancelled = true; };
    }, []);

    const allocations = summary?.allocations ?? [];

    const allocationChartData = useMemo(() => {
        if (allocations.length === 0) return [];
        return allocations.map((a) => ({
            name: a.ticker,
            value: a.marketValue,
            color: ASSET_COLORS[a.assetType] ?? '#BDC3C7',
        }));
    }, [allocations]);

    const totalAllocationValue = allocationChartData.reduce((sum, d) => sum + d.value, 0);

    const maxAllocation = useMemo(() => {
        if (allocations.length === 0) return null;
        return allocations.reduce((max, a) =>
                a.allocationPercent > max.allocationPercent ? a : max
            , allocations[0]);
    }, [allocations]);

    const concentrationPercent = maxAllocation?.allocationPercent ?? 0;
    const isConcentrationAlert = concentrationPercent > 40;

    const reviewNotes = useMemo(() => {
        const notes: { title: string; detail: string; color: string }[] = [];

        const demoCount = holdings.filter((h) => h.priceStatus === 'DEMO').length;
        if (demoCount > 0) {
            notes.push({
                title: 'Price transparency',
                detail: `${demoCount} holding${demoCount > 1 ? 's' : ''} use demo values.`,
                color: '#f6b33b',
            });
        }

        const unpricedCount = summary?.unpricedHoldings.length ?? 0;
        if (unpricedCount > 0) {
            notes.push({
                title: 'Unpriced holdings',
                detail: `${unpricedCount} holding${unpricedCount > 1 ? 's' : ''} have no price.`,
                color: '#e74c3c',
            });
        }

        if (maxAllocation && isConcentrationAlert) {
            notes.push({
                title: `${maxAllocation.ticker} concentration`,
                detail: `${formatPercent(concentrationPercent)} of priced value. Consider diversifying.`,
                color: '#e74c3c',
            });
        }

        notes.push({
            title: 'No advice',
            detail: 'Signals are explanatory only.',
            color: '#9B59B6',
        });

        return notes;
    }, [holdings, summary, maxAllocation, concentrationPercent, isConcentrationAlert]);

    return (
        <div className="analysis-page-wrapper">
            <section className="analysis-top-row">
                <div className="analysis-card">
                    <h2 className="analysis-card-title">Allocation X-Ray</h2>
                    {allocationChartData.length > 0 ? (
                        <div className="allocation-xray-content">
                            <div className="donut-wrapper">
                                <ResponsiveContainer width="100%" height={220}>
                                    <PieChart>
                                        <Pie
                                            data={allocationChartData}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={60}
                                            outerRadius={95}
                                            paddingAngle={2}
                                            dataKey="value"
                                            strokeWidth={0}
                                        >
                                            {allocationChartData.map((entry) => (
                                                <Cell key={entry.name} fill={entry.color}/>
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            formatter={(value: number) => [formatMoney(value), '']}
                                            contentStyle={{
                                                borderRadius: 12,
                                                border: 'none',
                                                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                            }}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                                <div className="donut-center-label">
                                    <span className="donut-value">{formatMoney(totalAllocationValue)}</span>
                                    <span className="donut-sub">priced value</span>
                                </div>
                            </div>
                            <div className="allocation-legend">
                                {allocationChartData.map((item) => (
                                    <div className="legend-item" key={item.name}>
                                        <svg width="16" height="16" viewBox="0 0 16 16">
                                            <polygon
                                                points={hexagonPoints(8, 8, 7)}
                                                fill={item.color}
                                            />
                                        </svg>
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
                            <p className="holdings-empty-text">No allocation data. Add holdings to see the chart.</p>
                        </div>
                    )}
                </div>

                <div className="analysis-card">
                    <h2 className="analysis-card-title">Concentration Check</h2>
                    {maxAllocation ? (
                        <div className="concentration-content">
                            <div className="concentration-hex">
                                <svg width="140" height="140" viewBox="0 0 140 140">
                                    <polygon
                                        points={hexagonPoints(70, 70, 60)}
                                        fill={isDark ? '#1a1810' : '#fff9ed'}
                                        stroke={isConcentrationAlert ? '#e74c3c' : '#f6b33b'}
                                        strokeWidth="3"
                                    />
                                </svg>
                                <div className="concentration-hex-label">
                                    <span className="concentration-value">{formatPercent(concentrationPercent)}</span>
                                    <span className="concentration-sub">{maxAllocation.ticker} largest</span>
                                </div>
                            </div>
                            <div className="concentration-info">
                                <p className={`concentration-status ${isConcentrationAlert ? 'concentration-alert' : ''}`}>
                                    {isConcentrationAlert
                                        ? `Above 40% alert line — consider diversifying`
                                        : 'Below 40% alert line'}
                                </p>
                                <p className="concentration-detail">Keep monitoring after new holdings.</p>
                            </div>
                        </div>
                    ) : (
                        <div className="holdings-empty-state">
                            <p className="holdings-empty-text">No data to check concentration.</p>
                        </div>
                    )}
                </div>
            </section>

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
