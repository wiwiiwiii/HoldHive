import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import toast from 'react-hot-toast';
import { createHolding, searchMarket } from '../api/portfolioApi';
import type { AssetType, MarketSearchItem } from '../api/types';

const ASSET_TYPES: { value: AssetType; label: string }[] = [
    { value: 'STOCK', label: 'Stock' },
    { value: 'ETF', label: 'ETF' },
    { value: 'MUTUAL_FUND', label: 'Mutual Fund' },
    { value: 'CRYPTO', label: 'Crypto' },
    { value: 'CASH', label: 'Cash' },
    { value: 'BANK_DEPOSIT', label: 'Bank Deposit' },
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
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<MarketSearchItem[]>([]);
    const [showSearchDropdown, setShowSearchDropdown] = useState(false);
    const [selectedItem, setSelectedItem] = useState<MarketSearchItem | null>(null);
    const [isSearching, setIsSearching] = useState(false);

    const [assetType, setAssetType] = useState<AssetType>('STOCK');
    const [quantity, setQuantity] = useState('');
    const [price, setPrice] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errors, setErrors] = useState<FormErrors>({});

    const searchTimerRef = useRef<ReturnType<typeof setTimeout>>();
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setShowSearchDropdown(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSearchInput = useCallback((value: string) => {
        setSearchQuery(value);
        setSelectedItem(null);
        if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
        if (!value.trim()) {
            setSearchResults([]);
            setShowSearchDropdown(false);
            return;
        }
        searchTimerRef.current = setTimeout(async () => {
            setIsSearching(true);
            try {
                const result = await searchMarket(value.trim());
                const keyword = value.trim().toUpperCase();
                const filtered = result.results.filter((item) =>
                    item.ticker.toUpperCase().includes(keyword)
                );
                setSearchResults(filtered);
                setShowSearchDropdown(true);
            } catch {
                setSearchResults([]);
            } finally {
                setIsSearching(false);
            }
        }, 300);
    }, []);

    const handleSelectResult = useCallback((item: MarketSearchItem) => {
        setSelectedItem(item);
        setSearchQuery(item.ticker);
        setAssetType(item.assetType);
        setShowSearchDropdown(false);
        setErrors((prev) => ({ ...prev, ticker: undefined }));
    }, []);

    const isFundType = assetType === 'ETF' || assetType === 'MUTUAL_FUND';
    const isFixedPriceType = assetType === 'CASH' || assetType === 'BANK_DEPOSIT';

    const costBasis = useMemo(() => {
        const qty = parseFloat(quantity);
        const avgPrice = isFixedPriceType ? 1 : parseFloat(price);
        if (isNaN(qty) || isNaN(avgPrice) || qty <= 0 || avgPrice <= 0) return null;
        return qty * avgPrice;
    }, [quantity, price, isFixedPriceType]);

    const qtyNum = parseInt(quantity);

    const validate = useCallback((): boolean => {
        const newErrors: FormErrors = {};
        if (!searchQuery.trim() && !selectedItem) {
            newErrors.ticker = 'Please search and select a ticker.';
        }
        const qty = parseFloat(quantity);
        if (!quantity || isNaN(qty) || qty <= 0) {
            newErrors.quantity = 'Must be greater than 0.';
        }
        if (!isFixedPriceType) {
            const avgPrice = parseFloat(price);
            if (!price || isNaN(avgPrice) || avgPrice < 0) {
                newErrors.price = 'Must be 0 or greater.';
            }
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    }, [searchQuery, selectedItem, quantity, price, isFixedPriceType]);

    const handleSave = useCallback(async () => {
        if (!validate()) return;

        setIsSubmitting(true);
        try {
            const createdHolding = await createHolding({
                assetType,
                ticker: selectedItem?.ticker ?? searchQuery.trim().toUpperCase(),
                exchangeCode: selectedItem?.exchangeCode,
                displayName: selectedItem?.displayName,
                providerQuoteId: selectedItem?.providerQuoteId,
                currency: selectedItem ? undefined : undefined,
                quantity: parseFloat(quantity),
                averagePurchasePrice: isFixedPriceType ? 1 : parseFloat(price),
            });
            toast.success(`Added ${createdHolding.ticker} successfully. Portfolio totals refreshed.`);
            setSearchQuery('');
            setSelectedItem(null);
            setSearchResults([]);
            setQuantity('');
            setPrice('');
            setAssetType('STOCK');
            setErrors({});
            onSaved?.();
        } catch (err) {
            const message = err instanceof Error ? err.message : 'Failed to save holding.';
            toast.error(message);
        } finally {
            setIsSubmitting(false);
        }
    }, [assetType, selectedItem, searchQuery, quantity, price, isFixedPriceType, validate, onSaved]);

    return (
        <div className="add-holding-page-wrapper">
            <section className="add-holding-layout">
                <div className="add-holding-form-card">
                    <h2 className="add-holding-form-title">New holding</h2>
                    <p className="add-holding-form-subtitle">Search a ticker, choose type, and enter quantity.</p>

                    <div className="add-holding-field" ref={dropdownRef} style={{ position: 'relative' }}>
                        <label className="add-holding-label">Ticker</label>
                        <input
                            className={`add-holding-input ${errors.ticker ? 'input-error' : ''}`}
                            type="text"
                            value={searchQuery}
                            onChange={(e) => handleSearchInput(e.target.value)}
                            onFocus={() => searchResults.length > 0 && setShowSearchDropdown(true)}
                            placeholder="Search ticker, e.g. AAPL"
                            disabled={isSubmitting}
                            autoComplete="off"
                        />
                        {isSearching && <span className="add-holding-search-spinner">Searching...</span>}
                        <span className={`add-holding-hint ${errors.ticker ? 'hint-error' : ''}`}>
                            {errors.ticker || 'Search and select from results'}
                        </span>
                        {showSearchDropdown && searchResults.length > 0 && (
                            <ul className="add-holding-search-dropdown">
                                {searchResults.map((item) => (
                                    <li
                                        key={item.providerQuoteId}
                                        className={`add-holding-search-item ${selectedItem?.providerQuoteId === item.providerQuoteId ? 'selected' : ''}`}
                                        onClick={() => handleSelectResult(item)}
                                    >
                                        <span className="search-item-ticker">{item.ticker}</span>
                                        <span className="search-item-name">{item.displayName}</span>
                                        <span className="search-item-meta">{item.exchangeCode} · {item.assetType}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    <div className="add-holding-field">
                        <label className="add-holding-label">Asset type</label>
                        <select
                            className="add-holding-input add-holding-select"
                            value={assetType}
                            onChange={(e) => setAssetType(e.target.value as AssetType)}
                            disabled={isSubmitting}
                        >
                            {ASSET_TYPES.map((t) => (
                                <option key={t.value} value={t.value}>{t.label}</option>
                            ))}
                        </select>
                    </div>

                    {isFundType && (
                        <div className="add-holding-fund-warning">
                            Fund holdings may contain stocks that overlap with your direct stock holdings.
                        </div>
                    )}

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
                            value={isFixedPriceType ? '1.00' : price}
                            onChange={(e) => { setPrice(e.target.value); setErrors((prev) => ({ ...prev, price: undefined })); }}
                            placeholder={isFixedPriceType ? 'Fixed at 1.00' : 'e.g. 175.50'}
                            min="0"
                            step="0.01"
                            disabled={isSubmitting || isFixedPriceType}
                        />
                        <span className={`add-holding-hint ${errors.price ? 'hint-error' : ''}`}>
                            {errors.price || (isFixedPriceType ? 'Fixed at 1.00 for this type' : 'Use the same currency as quote')}
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
                            onClick={() => {
                                setSearchQuery('');
                                setSelectedItem(null);
                                setSearchResults([]);
                                setQuantity('');
                                setPrice('');
                                setAssetType('STOCK');
                                setErrors({});
                            }}
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
                                {selectedItem?.ticker || searchQuery || '—'}
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
        </div>
    );
}
