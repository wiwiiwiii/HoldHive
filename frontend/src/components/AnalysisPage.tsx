import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
} from 'recharts';

const ALLOCATION_DATA = [
    { name: 'Stocks', value: 63, color: '#4F86F7' },
    { name: 'Cash', value: 28, color: '#F5A623' },
    { name: 'ETF', value: 6, color: '#9B59B6' },
    { name: 'Unpriced', value: 3, color: '#BDC3C7' },
];

const REVIEW_NOTES = [
    { title: 'Price transparency', detail: '2 holdings use demo values.', color: '#f6b33b' },
    { title: 'Diversification', detail: 'Cash reserve is 28.2% of priced value.', color: '#4F86F7' },
    { title: 'No advice', detail: 'Signals are explanatory only.', color: '#9B59B6' },
];

function hexagonPoints(cx: number, cy: number, r: number): string {
    const pts = [];
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const rad = (angle * Math.PI) / 180;
        pts.push(`${cx + r * Math.cos(rad)},${cy + r * Math.sin(rad)}`);
    }
    return pts.join(' ');
}

export function AnalysisPage() {
    return (
        <>
            <section className="analysis-top-row">
                <div className="analysis-card">
                    <h2 className="analysis-card-title">Allocation X-Ray</h2>
                    <div className="allocation-xray-content">
                        <div className="donut-wrapper">
                            <ResponsiveContainer width="100%" height={220}>
                                <PieChart>
                                    <Pie
                                        data={ALLOCATION_DATA}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={95}
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
                                    <svg width="16" height="16" viewBox="0 0 16 16">
                                        <polygon
                                            points={hexagonPoints(8, 8, 7)}
                                            fill={item.color}
                                        />
                                    </svg>
                                    <span className="legend-name">{item.name}</span>
                                    <span className="legend-value">{item.value}%</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="analysis-card">
                    <h2 className="analysis-card-title">Concentration Check</h2>
                    <div className="concentration-content">
                        <div className="concentration-hex">
                            <svg width="140" height="140" viewBox="0 0 140 140">
                                <polygon
                                    points={hexagonPoints(70, 70, 60)}
                                    fill="#fff9ed"
                                    stroke="#f6b33b"
                                    strokeWidth="3"
                                />
                            </svg>
                            <div className="concentration-hex-label">
                                <span className="concentration-value">33.8%</span>
                                <span className="concentration-sub">largest holding</span>
                            </div>
                        </div>
                        <div className="concentration-info">
                            <p className="concentration-status">Below 40% alert line</p>
                            <p className="concentration-detail">Keep monitoring after new holdings.</p>
                        </div>
                    </div>
                </div>
            </section>

            <section className="analysis-page-section">
                <div className="analysis-card">
                    <h2 className="analysis-card-title">Review Notes</h2>
                    <div className="review-notes-list">
                        {REVIEW_NOTES.map((note) => (
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
        </>
    );
}
