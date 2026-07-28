import { useState } from 'react';

const FILTERS = ['All', 'Priced', 'Unpriced', 'High gain'];

const HOLDINGS_DATA = [
    { symbol: 'AAPL', company: 'Apple Inc.', qty: 35, price: '$210.25', marketValue: '$7,358.75', pl: 1216.25, allocation: '33.8%' },
    { symbol: 'TSLA', company: 'Tesla Inc.', qty: 20, price: '$248.90', marketValue: '$4,978.00', pl: 482.00, allocation: '22.8%' },
    { symbol: 'AMZN', company: 'Amazon.com Inc.', qty: 10, price: '$186.70', marketValue: '$1,867.00', pl: -143.00, allocation: '8.6%' },
    { symbol: 'GOOGL', company: 'Alphabet Inc.', qty: 8, price: '$179.45', marketValue: '$1,435.60', pl: 238.00, allocation: '6.6%' },
    { symbol: 'CASH', company: 'Cash Reserve', qty: null, price: '$1.00', marketValue: '$6,155.40', pl: 0, allocation: '28.2%' },
];

function formatPl(value: number): string {
    if (value > 0) return `+$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    if (value < 0) return `-$${Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    return '$0.00';
}

export function HoldingsPage() {
    const [activeFilter, setActiveFilter] = useState('All');

    return (
        <>
            <section className="holdings-page-section">
                <div className="holdings-filter-card">
                    <h2 className="holdings-filter-title">Quick filters</h2>
                    <div className="holdings-filter-buttons">
                        {FILTERS.map((filter) => (
                            <button
                                key={filter}
                                className={`holdings-filter-btn ${activeFilter === filter ? 'active' : ''}`}
                                onClick={() => setActiveFilter(filter)}
                            >
                                {filter}
                            </button>
                        ))}
                    </div>
                </div>
            </section>

            <section className="holdings-page-section">
                <div className="holdings-ledger-card">
                    <h2 className="holdings-ledger-title">Holding Ledger</h2>
                    <table className="holdings-ledger-table">
                        <thead>
                        <tr>
                            <th>Symbol</th>
                            <th>Company</th>
                            <th>Qty</th>
                            <th>Price</th>
                            <th>Market Value</th>
                            <th>P/L</th>
                            <th>Allocation</th>
                        </tr>
                        </thead>
                        <tbody>
                        {HOLDINGS_DATA.map((row) => (
                            <tr key={row.symbol}>
                                <td className="ledger-symbol">{row.symbol}</td>
                                <td>{row.company}</td>
                                <td>{row.qty ?? '-'}</td>
                                <td>{row.price}</td>
                                <td className="ledger-value">{row.marketValue}</td>
                                <td className={row.pl > 0 ? 'ledger-pl-positive' : row.pl < 0 ? 'ledger-pl-negative' : ''}>
                                    {formatPl(row.pl)}
                                </td>
                                <td>{row.allocation}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>

            <section className="holdings-page-section">
                <div className="holdings-conflict-card">
                    <span className="conflict-label">Conflict rule</span>
                    <span className="conflict-detail">Duplicate tickers show a clear message. Cost basis is never changed silently.</span>
                </div>
            </section>
        </>
    );
}
