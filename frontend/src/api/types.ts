export type AssetType =
    | 'STOCK'
    | 'ETF'
    | 'MUTUAL_FUND'
    | 'CRYPTO'
    | 'CASH'
    | 'BANK_DEPOSIT';

export type PriceStatus = 'LIVE' | 'CACHED' | 'DEMO' | 'FIXED' | 'UNAVAILABLE';

export type ValuationStatus = 'EMPTY' | 'COMPLETE' | 'PARTIAL' | 'UNAVAILABLE';

export type PriceMode = 'BEST_AVAILABLE' | 'DEMO_ALLOWED';

export interface HoldingResponse {
  id: number;
  instrumentId: number;
  assetType: AssetType;
  ticker: string;
  exchangeCode: string | null;
  displayName: string | null;
  provider: string | null;
  providerQuoteId: string | null;
  currency: string | null;
  quantity: number;
  averagePurchasePrice: number;
  currentPrice: number | null;
  marketValue: number | null;
  costBasis: number | null;
  unrealizedGainLoss: number | null;
  unrealizedGainLossPercent: number | null;
  allocationPercent: number | null;
  priceStatus: PriceStatus;
  priceObservedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AllocationResponse {
  holdingId: number;
  assetType: AssetType;
  ticker: string;
  marketValue: number;
  allocationPercent: number;
}

export interface UnpricedHoldingResponse {
  holdingId: number;
  assetType: AssetType;
  ticker: string;
  reason: string;
}

export interface PortfolioSummaryResponse {
  portfolioId: number;
  portfolioName: string;
  baseCurrency: string;
  holdingCount: number;
  pricedHoldingCount: number;
  valuationStatus: ValuationStatus;
  totalCostBasis: number;
  totalMarketValue: number;
  totalUnrealizedGainLoss: number;
  totalUnrealizedGainLossPercent: number | null;
  priceAsOf: string | null;
  allocations: AllocationResponse[];
  unpricedHoldings: UnpricedHoldingResponse[];
}

export interface FundComponentResponse {
  ticker: string;
  displayName: string;
  assetType: AssetType;
  weightPercent: number;
}

export interface FundLookthroughResponse {
  fundInstrumentId: number;
  ticker: string;
  displayName: string;
  assetType: AssetType;
  asOfDate: string;
  source: string;
  coveragePercent: number;
  holdings: FundComponentResponse[];
  warnings: string[];
}

export interface MarketSearchItem {
  ticker: string;
  displayName: string;
  exchangeCode: string;
  provider: string;
  providerQuoteId: string;
  assetType: AssetType;
}

export interface MarketSearchResult {
  query: string;
  results: MarketSearchItem[];
  source: string;
  cached: boolean;
}

export interface MarketQuote {
  provider: string;
  providerQuoteId: string;
  ticker: string;
  displayName: string;
  currency: string;
  currentPrice: number;
  priceStatus: PriceStatus;
  priceObservedAt: string;
}

export interface UnavailableQuote {
  providerQuoteId: string;
  reason: string;
}

export interface MarketQuoteResult {
  provider: string;
  priceMode: PriceMode;
  quotes: MarketQuote[];
  unavailable: UnavailableQuote[];
}

export interface PortfolioExposureItem {
  ticker: string;
  displayName: string;
  assetType: AssetType;
  directMarketValue: number;
  fundLookthroughMarketValue: number;
  totalExposureValue: number;
  exposurePercent: number;
  sources: string[];
}

export interface PortfolioExposure {
  portfolioId: number;
  portfolioName: string;
  baseCurrency: string;
  lookthrough: boolean;
  priceMode: PriceMode;
  totalMarketValue: number;
  items: PortfolioExposureItem[];
  warnings: string[];
}
