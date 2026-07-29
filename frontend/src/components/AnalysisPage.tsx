import { useEffect, useMemo, useState } from 'react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
} from 'recharts';
import { fetchHoldingsFull, fetchPortfolioExposure, fetchPortfolioSummary } from '../api/portfolioApi';
import type { AssetType, HoldingResponse, PortfolioExposure, PortfolioSummaryResponse } from '../api/types';

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
    const [exposure, setExposure] = useState<PortfolioExposure | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const [nextSummary, nextHoldings, nextExposure] = await Promise.all([
                    fetchPortfolioSummary(),
                    fetchHoldingsFull(),
                    fetchPortfolioExposure(true),
                ]);
                if (!cancelled) {
                    setSummary(nextSummary);
                    setHoldings(nextHoldings);
                    setExposure(nextExposure);
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

    const exposureWarnings = exposure?.warnings ?? [];

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

        if (exposureWarnings.length > 0) {
            notes.push({
                title: 'Fund overlap warning',
                detail: exposureWarnings.join(' '),
                color: '#e74c3c',
            });
        }

        notes.push({
            title: 'No advice',
            detail: 'Signals are explanatory only.',
            color: '#9B59B6',
        });

        return notes;
    }, [holdings, summary, maxAllocation, concentrationPercent, isConcentrationAlert, exposureWarnings]);

    return (
        <div className="analysis-page-wrapper">
            {exposureWarnings.length > 0 && (
                <section className="analysis-page-section">
                    <div className="analysis-card">
                        <div className="fund-warning-banner">
                            <span className="fund-warning-icon">⚠</span>
                            <span className="fund-warning-text">{exposureWarnings.join(' ')}</span>
                        </div>
                    </div>
                </section>
            )}

            <section className="analysis-top-row">
                {/* ... existing code ... */}
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
