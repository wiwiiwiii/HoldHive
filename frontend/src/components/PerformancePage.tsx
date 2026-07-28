import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
} from 'recharts';

const PERFORMANCE_DATA = [
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

const INSIGHT_CARDS = [
    { label: 'Best contributor', value: 'AAPL', detail: '+$1,216.25 unrealized', color: '#27ae60' },
    { label: 'Needs review', value: 'AMZN', detail: '-$143.00 unrealized', color: '#e74c3c' },
    { label: 'Return basis', value: 'Snapshot P/L', detail: 'Not time-weighted return', color: '#172033' },
];

export function PerformancePage() {
    return (
        <>
            <section className="performance-page-section">
                <div className="performance-chart-card">
                    <h2 className="performance-chart-title">Portfolio Value Trend</h2>
                    <p className="performance-chart-subtitle">Current-value trend for demo data</p>
                    <div className="performance-chart-wrapper">
                        <ResponsiveContainer width="100%" height={300}>
                            <AreaChart data={PERFORMANCE_DATA}>
                                <defs>
                                    <linearGradient id="perfTrendGradient" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor="#4F86F7" stopOpacity={0.25} />
                                        <stop offset="100%" stopColor="#4F86F7" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <XAxis
                                    dataKey="date"
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{ fontSize: 12, fill: '#8a94a6' }}
                                />
                                <YAxis hide domain={['dataMin - 500', 'dataMax + 500']} />
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
                                    dot={{ r: 4, fill: '#fff', stroke: '#4F86F7', strokeWidth: 2 }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </section>

            <section className="performance-insights-row">
                {INSIGHT_CARDS.map((card) => (
                    <div className="performance-insight-card" key={card.label}>
                        <p className="insight-label">{card.label}</p>
                        <p className="insight-value" style={{ color: card.color }}>{card.value}</p>
                        <p className="insight-detail">{card.detail}</p>
                    </div>
                ))}
            </section>
        </>
    );
}
