import { useState, useMemo, useCallback } from 'react';
import toast from 'react-hot-toast';
import { createHolding } from '../api/portfolioApi';

function hexagonPoints(cx: number, cy: number, r: number): string {
    const pts = [];
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const rad = (angle * Math.PI) / 180;
        pts.push(`${cx + r * Math.cos(rad)},${cy + r * Math.sin(rad)}`);
    }
    return pts.join(' ');
}

interface AddHoldingPageProps {
    isDark?: boolean;
    onSaved?: () => void;
}

interface FormErrors {
    ticker?: string;
    quantity?: string;
    price?: string;
}

export function AddHoldingPage({ isDark, onSaved }: AddHoldingPageProps) {
    const [ticker, setTicker] = useState('');
    const [quantity, setQuantity] = useState('');
    const [price, setPrice] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errors, setErrors] = useState<FormErrors>({});

    const costBasis = useMemo(() => {
        const qty = parseFloat(quantity);
        const avgPrice = parseFloat(price);
        if (isNaN(qty) || isNaN(avgPrice) || qty <= 0 || avgPrice <= 0) return null;
        return qty * avgPrice;
    }, [quantity, price]);

    const qtyNum = parseInt(quantity);

    const validate = useCallback((): boolean => {
        const newErrors: FormErrors = {};
        if (!ticker.trim()) {
            newErrors.ticker = 'Ticker is required.';
        }
        const qty = parseFloat(quantity);
        if (!quantity || isNaN(qty) || qty <= 0) {
            newErrors.quantity = 'Must be greater than 0.';
        }
        const avgPrice = parseFloat(price);
        if (!price || isNaN(avgPrice) || avgPrice < 0) {
            newErrors.price = 'Must be 0 or greater.';
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    }, [ticker, quantity, price]);

    const handleSave = useCallback(async () => {
        if (!validate()) return;

        setIsSubmitting(true);
        try {
            await createHolding({
                assetType: 'STOCK',
                ticker: ticker.trim().toUpperCase(),
                quantity: parseFloat(quantity),
                averagePurchasePrice: parseFloat(price),
            });
            toast.success(`${ticker.toUpperCase()} added. Portfolio totals refreshed.`);
            setTicker('');
            setQuantity('');
            setPrice('');
            setErrors({});
            onSaved?.();
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to save holding.';
            toast.error(message);
        } finally {
            setIsSubmitting(false);
        }
    }, [ticker, quantity, price, validate, onSaved]);

    return (
        <>
            <section className="add-holding-layout">
                <div className="add-holding-form-card">
                    <h2 className="add-holding-form-title">New holding</h2>
                    <p className="add-holding-form-subtitle">Ticker, quantity, and average price.</p>

                    <div className="add-holding-field">
                        <label className="add-holding-label">Ticker</label>
                        <input
                            className={`add-holding-input ${errors.ticker ? 'input-error' : ''}`}
                            type="text"
                            value={ticker}
                            onChange={(e) => { setTicker(e.target.value.toUpperCase()); setErrors((prev) => ({ ...prev, ticker: undefined })); }}
                            placeholder="e.g. AAPL"
                            disabled={isSubmitting}
                        />
                        <span className={`add-holding-hint ${errors.ticker ? 'hint-error' : ''}`}>
                            {errors.ticker || 'Uppercase automatically'}
                        </span>
                    </div>

                    <div className="add-holding-field">
                        <label className="add-holding-label">Quantity</label>
                        <input
                            className={`add-holding-input ${errors.quantity ? 'input-error' : ''}`}
                            type="number"
                            value={quantity}
                            onChange={(e) => { setQuantity(e.target.value); setErrors((prev) => ({ ...prev, quantity: undefined })); }}
                            placeholder="e.g. 35"
                            min="1"
                            disabled={isSubmitting}
                        />
                        <span className={`add-holding-hint ${errors.quantity ? 'hint-error' : ''}`}>
                            {errors.quantity || 'Must be greater than 0'}
                        </span>
                    </div>

                    <div className="add-holding-field">
                        <label className="add-holding-label">Average purchase price</label>
                        <input
                            className={`add-holding-input ${errors.price ? 'input-error' : ''}`}
                            type="number"
                            value={price}
                            onChange={(e) => { setPrice(e.target.value); setErrors((prev) => ({ ...prev, price: undefined })); }}
                            placeholder="e.g. 175.50"
                            min="0"
                            step="0.01"
                            disabled={isSubmitting}
                        />
                        <span className={`add-holding-hint ${errors.price ? 'hint-error' : ''}`}>
                            {errors.price || 'Use the same currency as quote'}
                        </span>
                    </div>

                    <div className="add-holding-actions">
                        <button
                            className="add-holding-save-btn"
                            onClick={handleSave}
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Saving...' : 'Save holding'}
                        </button>
                        <button
                            className="add-holding-cancel-btn"
                            onClick={() => { setTicker(''); setQuantity(''); setPrice(''); setErrors({}); }}
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                    </div>
                </div>

                <div className="add-holding-preview-card">
                    <h2 className="add-holding-preview-title">Live preview</h2>
                    <div className="add-holding-preview-hex">
                        <svg width="180" height="180" viewBox="0 0 180 180">
                            <polygon
                                points={hexagonPoints(90, 90, 80)}
                                fill={isDark ? '#1a1810' : '#fff9ed'}
                                stroke={isDark ? '#c9a84c' : '#f6b33b'}
                                strokeWidth="3"
                            />
                            <text
                                x="90"
                                y="82"
                                textAnchor="middle"
                                className="add-holding-hex-ticker"
                            >
                                {ticker || '—'}
                            </text>
                            <text
                                x="90"
                                y="108"
                                textAnchor="middle"
                                className="add-holding-hex-shares"
                            >
                                {!isNaN(qtyNum) && qtyNum > 0 ? `${qtyNum} shares` : '—'}
                            </text>
                        </svg>
                    </div>
                    <div className="add-holding-preview-cost">
                        <p className="add-holding-cost-label">Estimated cost basis</p>
                        <p className="add-holding-cost-value">
                            {costBasis !== null
                                ? `$${costBasis.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                                : '—'}
                        </p>
                    </div>
                    <p className="add-holding-preview-note">Input stays visible after errors.</p>
                </div>
            </section>
        </>
    );
}
