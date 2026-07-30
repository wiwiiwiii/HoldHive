import { useState, useEffect, useCallback } from 'react';
import { Trash2, Hexagon, Pencil } from 'lucide-react';
import toast from 'react-hot-toast';
import { fetchHoldingsFull, deleteHolding, updateHolding } from '../api/portfolioApi';
import type { HoldingResponse, PriceMode } from '../api/types';
import { ThinkingLoader } from './ThinkingLoader';

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

function getPriceStatusLabel(status: string): string {
    switch (status) {
        case 'LIVE': return 'Live';
        case 'CACHED': return 'Cached';
        case 'DEMO': return 'Demo';
        case 'FIXED': return 'Fixed';
        case 'UNAVAILABLE': return 'Unavailable';
        default: return 'Unknown';
    }
}

interface HoldingsPageProps {
    isDark?: boolean;
    refreshTrigger?: number;
    onHoldingsChanged?: () => void;
}

const PRICE_MODE: PriceMode = 'DEMO_ALLOWED';

export function HoldingsPage({ isDark, refreshTrigger, onHoldingsChanged }: HoldingsPageProps) {
    const [activeFilter, setActiveFilter] = useState('All');
    const [holdings, setHoldings] = useState<HoldingRow[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

    const [editTarget, setEditTarget] = useState<HoldingRow | null>(null);
    const [editQuantity, setEditQuantity] = useState('');
    const [editPrice, setEditPrice] = useState('');
    const [isUpdating, setIsUpdating] = useState(false);

    const loadHoldings = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await fetchHoldingsFull(PRICE_MODE);
            setHoldings(buildHoldingRows(data));
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load holdings.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            if (cancelled) return;
            await loadHoldings();
        }
        load();
        return () => { cancelled = true; };
    }, [refreshTrigger, loadHoldings]);

    const handleDelete = useCallback(async (id: number, ticker: string) => {
        try {
            const status = await deleteHolding(id);
            toast.success(`Removed ${ticker} successfully (HTTP ${status}). Allocation and totals updated.`);
            setDeleteConfirmId(null);
            if (onHoldingsChanged) {
                onHoldingsChanged();
            } else {
                await loadHoldings();
            }
        } catch (err) {
            const message = err instanceof Error ? err.message : `Remove holding failed: ${ticker}`;
            toast.error(message);
        }
    }, [loadHoldings, onHoldingsChanged]);

    const openEdit = useCallback((row: HoldingRow) => {
        setEditTarget(row);
        setEditQuantity(String(row.quantity));
        setEditPrice(String(row.averagePurchasePrice));
    }, []);

    const closeEdit = useCallback(() => {
        setEditTarget(null);
        setEditQuantity('');
        setEditPrice('');
    }, []);

    const handleUpdate = useCallback(async () => {
        if (!editTarget) return;
        const qty = parseFloat(editQuantity);
        const avgPrice = parseFloat(editPrice);
        if (isNaN(qty) || qty <= 0) {
            toast.error('Quantity must be greater than 0.');
            return;
        }
        if (isNaN(avgPrice) || avgPrice < 0) {
            toast.error('Average price must be 0 or greater.');
            return;
        }

        setIsUpdating(true);
        try {
            await updateHolding(editTarget.id, {
                quantity: qty,
                averagePurchasePrice: editTarget.assetType === 'CASH' || editTarget.assetType === 'BANK_DEPOSIT' ? 1.00 : avgPrice,
            }, PRICE_MODE);
            toast.success(`${editTarget.ticker} updated.`);
            closeEdit();
            if (onHoldingsChanged) {
                onHoldingsChanged();
            } else {
                await loadHoldings();
            }
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to update holding.';
            toast.error(message);
        } finally {
            setIsUpdating(false);
        }
    }, [editTarget, editQuantity, editPrice, closeEdit, loadHoldings, onHoldingsChanged]);

    const filteredHoldings = holdings.filter((row) => {
        if (activeFilter === 'All') return true;
        if (activeFilter === 'Priced') return row.priceStatus !== 'UNAVAILABLE' && row.currentPrice != null;
        if (activeFilter === 'Unpriced') return row.priceStatus === 'UNAVAILABLE' || row.currentPrice == null;
        if (activeFilter === 'High gain') return row.pl > 500;
        return true;
    });

    const deleteTarget = holdings.find((h) => h.id === deleteConfirmId);
    const isEditFixedPrice = editTarget?.assetType === 'CASH' || editTarget?.assetType === 'BANK_DEPOSIT';

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
                            <button className="holdings-error-retry" onClick={loadHoldings}>Retry</button>
                        </div>
                    )}

                    {isLoading ? (
                        <div className="holdings-empty-state">
                            <ThinkingLoader
                                compact
                                label="Thinking through holdings"
                                detail="Refreshing quantities, prices, and unrealized P/L."
                            />
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
                                <th>Avg Price</th>
                                <th>Price</th>
                                <th>Market Value</th>
                                <th>P/L</th>
                                <th>Allocation</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredHoldings.map((row) => (
                                <tr key={row.id}>
                                    <td className="ledger-symbol">{row.ticker}</td>
                                    <td>{row.assetType}</td>
                                    <td>{row.quantity > 0 ? row.quantity : '-'}</td>
                                    <td>{formatMoney(row.averagePurchasePrice)}</td>
                                    <td>
                                        {row.currentPrice != null
                                            ? formatMoney(row.currentPrice)
                                            : '—'}
                                    </td>
                                    <td className="ledger-value">
                                        {row.marketValue != null ? formatMoney(row.marketValue) : '—'}
                                    </td>
                                    <td className={row.pl > 0 ? 'ledger-pl-positive' : row.pl < 0 ? 'ledger-pl-negative' : ''}>
                                        {formatPl(row.pl)}
                                    </td>
                                    <td>{row.allocationStr}</td>
                                    <td>
                                        <span className={`price-status-badge price-status-${row.priceStatus.toLowerCase()}`}>
                                            {getPriceStatusLabel(row.priceStatus)}
                                        </span>
                                    </td>
                                    <td className="ledger-action-cell">
                                        <button
                                            className="ledger-edit-btn"
                                            onClick={() => openEdit(row)}
                                            title={`Edit ${row.ticker}`}
                                        >
                                            <Pencil size={14}/>
                                        </button>
                                        <button
                                            className="ledger-delete-btn"
                                            onClick={() => setDeleteConfirmId(row.id)}
                                            title={`Delete ${row.ticker}`}
                                        >
                                            <Trash2 size={16}/>
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
                            <button className="delete-dialog-cancel" onClick={() => setDeleteConfirmId(null)}>Cancel
                            </button>
                            <button className="delete-dialog-confirm"
                                    onClick={() => handleDelete(deleteConfirmId, deleteTarget.ticker)}>Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {editTarget !== null && (
                <div className="delete-overlay" onClick={closeEdit}>
                    <div className="delete-dialog" onClick={(e) => e.stopPropagation()}>
                        <h3 className="delete-dialog-title">Edit {editTarget.ticker}</h3>
                        <div className="edit-form-field">
                            <label className="add-holding-label">Quantity</label>
                            <input
                                className="add-holding-input"
                                type="number"
                                value={editQuantity}
                                onChange={(e) => setEditQuantity(e.target.value)}
                                min="1"
                                disabled={isUpdating}
                            />
                        </div>
                        <div className="edit-form-field">
                            <label className="add-holding-label">Average purchase price</label>
                            <input
                                className="add-holding-input"
                                type="number"
                                value={editPrice}
                                onChange={(e) => setEditPrice(e.target.value)}
                                min="0"
                                step="0.01"
                                disabled={isUpdating || isEditFixedPrice}
                            />
                            {isEditFixedPrice && (
                                <span className="add-holding-hint">Fixed at 1.00 for {editTarget.assetType === 'CASH' ? 'cash' : 'bank deposit'}.</span>
                            )}
                        </div>
                        <div className="delete-dialog-actions">
                            <button className="delete-dialog-cancel" onClick={closeEdit} disabled={isUpdating}>Cancel</button>
                            <button className="delete-dialog-confirm" onClick={handleUpdate} disabled={isUpdating}>
                                {isUpdating ? 'Saving...' : 'Save'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
