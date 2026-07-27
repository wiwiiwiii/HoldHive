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
  assetType: AssetType;
  ticker: string;
  quantity: number;
  averagePurchasePrice: number;
  currentPrice?: number;
  marketValue?: number;
  unrealizedGainLoss?: number;
  allocationPercent?: number;
  priceStatus: PriceStatus;
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
  assetType: 'ETF' | 'MUTUAL_FUND';
  asOfDate: string;
  source: string;
  coveragePercent: number;
  holdings: FundComponentResponse[];
  warnings: string[];
}
