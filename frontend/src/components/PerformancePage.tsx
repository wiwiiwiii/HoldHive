import { useEffect, useMemo, useState } from 'react';
import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
} from 'recharts';
import { fetchHoldingsFull } from '../api/portfolioApi';
import type { HoldingResponse } from '../api/types';

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

function formatMoney(value: number): string {
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

interface InsightCard {
    label: string;
    value: string;
    detail: string;
    color: string;
}

export function PerformancePage() {
    const [holdings, setHoldings] = useState<HoldingResponse[]>([]);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            try {
                const data = await fetchHoldingsFull();
                if (!cancelled) setHoldings(data);
            } catch {
                // keep empty on failure
            }
        }
        load();
        return () => { cancelled = true; };
    }, []);

    const insightCards = useMemo((): InsightCard[] => {
        if (holdings.length === 0) {
            return [
                { label: 'Best contributor', value: '—', detail: 'No holdings yet', color: '#8a94a6' },
                { label: 'Needs review', value: '—', detail: 'No holdings yet', color: '#8a94a6' },
                { label: 'Return basis', value: 'Snapshot P/L', detail: 'Not time-weighted return', color: '#172033' },
            ];
        }

        const withPl = holdings.map((h) => {
            const costBasis = h.quantity * h.averagePurchasePrice;
            const mv = h.marketValue ?? costBasis;
            const pl = h.unrealizedGainLoss ?? (mv - costBasis);
            return { ...h, pl };
        });

        const best = withPl.reduce((max, h) => h.pl > max.pl ? h : max, withPl[0]);
        const worst = withPl.reduce((min, h) => h.pl < min.pl ? h : min, withPl[0]);

        return [
            {
                label: 'Best contributor',
                value: best.ticker,
                detail: `${best.pl >= 0 ? '+' : ''}${formatMoney(best.pl)} unrealized`,
                color: best.pl >= 0 ? '#27ae60' : '#e74c3c',
            },
            {
                label: 'Needs review',
                value: worst.ticker,
                detail: `${worst.pl >= 0 ? '+' : ''}${formatMoney(worst.pl)} unrealized`,
                color: worst.pl < 0 ? '#e74c3c' : '#8a94a6',
            },
            {
                label: 'Return basis',
                value: 'Snapshot P/L',
                detail: 'Not time-weighted return',
                color: '#172033',
            },
        ];
    }, [holdings]);

    return (
        <div className="performance-page-wrapper">
            <section className="performance-page-section">
                <div className="performance-chart-card">
                    <h2 className="performance-chart-title">Portfolio Value Trend</h2>
                    <p className="performance-chart-subtitle">
                        {holdings.length > 0 ? 'Current-value trend' : 'Demo data — add holdings for live trend'}
                    </p>
                    <div className="performance-chart-wrapper">
                        <ResponsiveContainer width="100%" height={300}>
                            <AreaChart data={DEMO_PERFORMANCE_DATA}>
                                <defs>
                                    <linearGradient id="perfTrendGradient" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor="#4F86F7" stopOpacity={0.25}/>
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
                                    fill="url(#perfTrendGradient)"
                                    dot={{r: 4, fill: '#fff', stroke: '#4F86F7', strokeWidth: 2}}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </section>

            <section className="performance-insights-row">
                {insightCards.map((card) => (
                    <div className="performance-insight-card" key={card.label}>
                        <p className="insight-label">{card.label}</p>
                        <p className="insight-value" style={{color: card.color}}>{card.value}</p>
                        <p className="insight-detail">{card.detail}</p>
                    </div>
                ))}
            </section>
        </div>
    );
}
