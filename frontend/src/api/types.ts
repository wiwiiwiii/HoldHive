export type AssetType =
  | 'STOCK'
  | 'ETF'
  | 'MUTUAL_FUND'
  | 'CRYPTO'
  | 'CASH'
  | 'BANK_DEPOSIT';

export type PriceStatus = 'LIVE' | 'CACHED' | 'DEMO' | 'FIXED' | 'UNAVAILABLE';

export type ValuationStatus = 'EMPTY' | 'COMPLETE' | 'PARTIAL' | 'UNAVAILABLE';

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
