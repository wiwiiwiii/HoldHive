import { Hexagon } from 'lucide-react';
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

const METRIC_CARDS = [
    { label: 'Total Value', value: '$21,794.75', detail: '+8.9% vs cost', positive: true },
    { label: 'Cost Basis', value: '$20,001.50', detail: '5 priced holdings', positive: null },
    { label: 'Unrealized P/L', value: '+$1,793.25', detail: 'partial valuation', positive: true },
    { label: 'Data Mode', value: 'DEMO', detail: 'fixed demo prices', positive: null },
];

const ALLOCATION_DATA = [
    { name: 'Stocks', value: 63, color: '#4F86F7' },
    { name: 'Cash', value: 28, color: '#F5A623' },
    { name: 'ETF', value: 6, color: '#9B59B6' },
    { name: 'Unpriced', value: 3, color: '#BDC3C7' },
];

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

const HOLDINGS_DATA = [
    { symbol: 'AAPL', company: 'Apple Inc.', qty: 35, price: '$210.25', marketValue: '$7,358.75' },
    { symbol: 'TSLA', company: 'Tesla Inc.', qty: 20, price: '$248.90', marketValue: '$4,978.00' },
    { symbol: 'AMZN', company: 'Amazon.com Inc.', qty: 10, price: '$186.70', marketValue: '$1,867.00' },
];

const PORTFOLIO_NOTES = [
    { title: 'AAPL concentration', detail: '33.8% of priced value' },
    { title: 'Demo data', detail: 'Fixed values for training' },
];

export function DashboardPage() {
    return (
        <>
            <section className="metric-cards-row">
                {METRIC_CARDS.map((card) => (
                    <div className="metric-card" key={card.label}>
                        <div className="metric-header">
                            <Hexagon size={20} className="metric-icon" />
                            <span className="metric-label">{card.label}</span>
                        </div>
                        <p className="metric-value">{card.value}</p>
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
                    <div className="allocation-content">
                        <div className="donut-wrapper">
                            <ResponsiveContainer width="100%" height={200}>
                                <PieChart>
                                    <Pie
                                        data={ALLOCATION_DATA}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={55}
                                        outerRadius={90}
                                        paddingAngle={2}
                                        dataKey="value"
                                        strokeWidth={0}
                                    >
                                        {ALLOCATION_DATA.map((entry) => (
                                            <Cell key={entry.name} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        formatter={(value: number) => [`${value}%`, '']}
                                        contentStyle={{
                                            borderRadius: 12,
                                            border: 'none',
                                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                        }}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                            <div className="donut-center-label">
                                <span className="donut-value">$21.8k</span>
                                <span className="donut-sub">priced value</span>
                            </div>
                        </div>
                        <div className="allocation-legend">
                            {ALLOCATION_DATA.map((item) => (
                                <div className="legend-item" key={item.name}>
                                    <span className="legend-dot" style={{ backgroundColor: item.color }} />
                                    <span className="legend-name">{item.name}</span>
                                    <span className="legend-value">{item.value}%</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="chart-card performance-card">
                    <h2 className="chart-title">Portfolio Performance</h2>
                    <p className="chart-subtitle">Snapshot value trend</p>
                    <div className="performance-chart-wrapper">
                        <ResponsiveContainer width="100%" height={220}>
                            <AreaChart data={PERFORMANCE_DATA}>
                                <defs>
                                    <linearGradient id="perfGradient" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor="#4F86F7" stopOpacity={0.3} />
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
                                    fill="url(#perfGradient)"
                                    dot={{ r: 4, fill: '#fff', stroke: '#4F86F7', strokeWidth: 2 }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </section>

            <section className="bottom-row">
                <div className="holdings-card">
                    <h2 className="section-title">Your Holdings</h2>
                    <table className="holdings-table">
                        <thead>
                        <tr>
                            <th>Symbol</th>
                            <th>Company</th>
                            <th>Qty</th>
                            <th>Price</th>
                            <th>Market Value</th>
                        </tr>
                        </thead>
                        <tbody>
                        {HOLDINGS_DATA.map((row) => (
                            <tr key={row.symbol}>
                                <td className="symbol-cell">{row.symbol}</td>
                                <td>{row.company}</td>
                                <td>{row.qty}</td>
                                <td>{row.price}</td>
                                <td className="value-cell">{row.marketValue}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <div className="notes-card">
                    <h2 className="section-title">Portfolio Notes</h2>
                    <div className="notes-list">
                        {PORTFOLIO_NOTES.map((note) => (
                            <div className="note-item" key={note.title}>
                                <Hexagon size={32} className="note-icon" />
                                <div>
                                    <p className="note-title">{note.title}</p>
                                    <p className="note-detail">{note.detail}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </>
    );
}
