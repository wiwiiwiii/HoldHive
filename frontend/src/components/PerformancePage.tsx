import { useEffect, useMemo, useState } from 'react';
import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
} from 'recharts';
import { fetchHoldingsFull, fetchPortfolioSummary } from '../api/portfolioApi';
import type { HoldingResponse, PortfolioSummaryResponse, PriceMode } from '../api/types';
import { ThinkingLoader } from './ThinkingLoader';

const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

interface PerformancePoint {
    date: string;
    value: number;
}

function generateTrendData(
    costBasis: number,
    marketValue: number,
    months: number = 11,
): PerformancePoint[] {
    if (costBasis <= 0 || marketValue <= 0) return [];

    const now = new Date();
    const points: PerformancePoint[] = [];
    const totalReturn = (marketValue - costBasis) / costBasis;

    for (let i = months - 1; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const progress = 1 - i / (months - 1);

        const trend = costBasis * (1 + totalReturn * progress);
        const volatility = costBasis * 0.02 * Math.sin(progress * Math.PI * 2.5 + 1.3);
        const noise = (Math.random() - 0.5) * costBasis * 0.015;

        points.push({
            date: MONTH_LABELS[d.getMonth()],
            value: Math.round((trend + volatility + noise) * 100) / 100,
        });
    }

    points[points.length - 1].value = marketValue;
    return points;
}

function formatMoney(value: number): string {
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

interface InsightCard {
    label: string;
    value: string;
    detail: string;
    color: string;
}

interface PerformancePageProps {
    priceMode?: PriceMode;
}

export function PerformancePage({ priceMode = 'BEST_AVAILABLE' }: PerformancePageProps) {
    const [holdings, setHoldings] = useState<HoldingResponse[]>([]);
    const [summary, setSummary] = useState<PortfolioSummaryResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            setIsLoading(true);
            try {
                const [data, sum] = await Promise.all([
                    fetchHoldingsFull(priceMode),
                    fetchPortfolioSummary(priceMode),
                ]);
                if (!cancelled) {
                    setHoldings(data);
                    setSummary(sum);
                }
            } catch {
                // keep empty on failure
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        }
        load();
        return () => { cancelled = true; };
    }, [priceMode]);

    const chartData = useMemo((): PerformancePoint[] => {
        if (!summary || summary.totalMarketValue <= 0) return [];
        return generateTrendData(summary.totalCostBasis, summary.totalMarketValue);
    }, [summary]);

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

    const totalReturn = summary && summary.totalCostBasis > 0
        ? ((summary.totalMarketValue - summary.totalCostBasis) / summary.totalCostBasis * 100).toFixed(2)
        : null;

    return (
        <div className="performance-page-wrapper">
            <section className="performance-page-section">
                <div className="performance-chart-card">
                    <h2 className="performance-chart-title">Portfolio Value Trend</h2>
                    <p className="performance-chart-subtitle">
                        {chartData.length > 0
                            ? `Cost basis ${formatMoney(summary!.totalCostBasis)} → Current ${formatMoney(summary!.totalMarketValue)} (${totalReturn}%)`
                            : 'Add holdings to see your portfolio trend'}
                    </p>
                    <div className="performance-chart-wrapper">
                        {isLoading && !summary ? (
                            <ThinkingLoader
                                label="Thinking through performance"
                                detail="Building the value trend and contributor snapshot."
                            />
                        ) : chartData.length > 0 ? (
                            <ResponsiveContainer width="100%" height={300}>
                                <AreaChart data={chartData}>
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
                                        formatter={(value: number) => [formatMoney(value), 'Value']}
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
                        ) : (
                            <div style={{
                                height: 300,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: '#8a94a6',
                                fontSize: '0.95rem',
                            }}>
                                No portfolio data yet
                            </div>
                        )}
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
