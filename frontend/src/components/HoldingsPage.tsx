import { useState, useEffect, useCallback } from 'react';
import { Trash2, Hexagon } from 'lucide-react';
import toast from 'react-hot-toast';
import { fetchHoldingsFull, deleteHolding } from '../api/portfolioApi';
import type { HoldingResponse } from '../api/types';

const FILTERS = ['All', 'Priced', 'Unpriced', 'High gain'];

interface HoldingRow extends HoldingResponse {
    pl: number;
    allocationStr: string;
}

function buildHoldingRows(data: HoldingResponse[]): HoldingRow[] {
    const totalMarketValue = data.reduce((sum, h) => sum + (h.marketValue ?? 0), 0);
    return data.map((h) => {
        const costBasis = h.quantity * h.averagePurchasePrice;
        const mv = h.marketValue ?? costBasis;
        const pl = h.unrealizedGainLoss ?? (mv - costBasis);
        const allocationStr = totalMarketValue > 0 && h.allocationPercent != null
            ? `${h.allocationPercent.toFixed(1)}%`
            : '—';
        return { ...h, pl, allocationStr };
    });
}

function formatPl(value: number): string {
    if (value > 0) return `+$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    if (value < 0) return `-$${Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    return '$0.00';
}

function formatMoney(value: number): string {
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

interface HoldingsPageProps {
    isDark?: boolean;
    refreshTrigger?: number;
}

export function HoldingsPage({ isDark, refreshTrigger }: HoldingsPageProps) {
    const [activeFilter, setActiveFilter] = useState('All');
    const [holdings, setHoldings] = useState<HoldingRow[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            setIsLoading(true);
            setError(null);
            try {
                const data = await fetchHoldingsFull();
                if (!cancelled) {
                    setHoldings(buildHoldingRows(data));
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : 'Failed to load holdings.');
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        }
        load();
        return () => { cancelled = true; };
    }, [refreshTrigger]);

    const handleDelete = useCallback(async (id: number, ticker: string) => {
        try {
            await deleteHolding(id);
            setHoldings((prev) => prev.filter((h) => h.id !== id));
            setDeleteConfirmId(null);
            toast.success(`${ticker} removed. Allocation and totals updated.`);
        } catch {
            toast.error(`Failed to delete ${ticker}.`);
        }
    }, []);

    const filteredHoldings = holdings.filter((row) => {
        if (activeFilter === 'All') return true;
        if (activeFilter === 'Priced') return row.priceStatus !== 'UNAVAILABLE' && row.currentPrice != null;
        if (activeFilter === 'Unpriced') return row.priceStatus === 'UNAVAILABLE' || row.currentPrice == null;
        if (activeFilter === 'High gain') return row.pl > 500;
        return true;
    });

    const deleteTarget = holdings.find((h) => h.id === deleteConfirmId);

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

                    {error && (
                        <div className="holdings-error-banner" role="alert">
                            <p>{error}</p>
                            <button className="holdings-error-retry" onClick={() => setHoldings([])}>Retry</button>
                        </div>
                    )}

                    {isLoading ? (
                        <div className="holdings-empty-state">
                            <p className="holdings-empty-text">Loading holdings...</p>
                        </div>
                    ) : filteredHoldings.length === 0 && !error ? (
                        <div className="holdings-empty-state">
                            <Hexagon size={48} className="holdings-empty-icon" />
                            <p className="holdings-empty-title">No holdings found</p>
                            <p className="holdings-empty-text">
                                {activeFilter !== 'All'
                                    ? 'No holdings match this filter. Try "All".'
                                    : 'Add your first holding to create a portfolio snapshot.'}
                            </p>
                        </div>
                    ) : !error ? (
                        <table className="holdings-ledger-table">
                            <thead>
                            <tr>
                                <th>Symbol</th>
                                <th>Type</th>
                                <th>Qty</th>
                                <th>Price</th>
                                <th>Market Value</th>
                                <th>P/L</th>
                                <th>Allocation</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredHoldings.map((row) => (
                                <tr key={row.id}>
                                    <td className="ledger-symbol">{row.ticker}</td>
                                    <td>{row.assetType}</td>
                                    <td>{row.quantity > 0 ? row.quantity : '-'}</td>
                                    <td>
                                        {row.currentPrice != null
                                            ? formatMoney(row.currentPrice)
                                            : formatMoney(row.averagePurchasePrice)}
                                        {row.priceStatus === 'DEMO' && (
                                            <span className="price-status-demo"> demo</span>
                                        )}
                                    </td>
                                    <td className="ledger-value">
                                        {row.marketValue != null ? formatMoney(row.marketValue) : '—'}
                                    </td>
                                    <td className={row.pl > 0 ? 'ledger-pl-positive' : row.pl < 0 ? 'ledger-pl-negative' : ''}>
                                        {formatPl(row.pl)}
                                    </td>
                                    <td>{row.allocationStr}</td>
                                    <td className="ledger-action-cell">
                                        <button
                                            className="ledger-delete-btn"
                                            onClick={() => setDeleteConfirmId(row.id)}
                                            title={`Delete ${row.ticker}`}
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    ) : null}
                </div>
            </section>

            <section className="holdings-page-section">
                <div className="holdings-conflict-card">
                    <span className="conflict-label">Conflict rule</span>
                    <span className="conflict-detail">Duplicate tickers show a clear message. Cost basis is never changed silently.</span>
                </div>
            </section>

            {deleteConfirmId !== null && deleteTarget && (
                <div className="delete-overlay" onClick={() => setDeleteConfirmId(null)}>
                    <div className="delete-dialog" onClick={(e) => e.stopPropagation()}>
                        <h3 className="delete-dialog-title">Delete holding</h3>
                        <p className="delete-dialog-text">
                            Are you sure you want to delete <strong>{deleteTarget.ticker}</strong>?
                            This action cannot be undone.
                        </p>
                        <div className="delete-dialog-actions">
                            <button
                                className="delete-dialog-cancel"
                                onClick={() => setDeleteConfirmId(null)}
                            >
                                Cancel
                            </button>
                            <button
                                className="delete-dialog-confirm"
                                onClick={() => handleDelete(deleteConfirmId, deleteTarget.ticker)}
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
