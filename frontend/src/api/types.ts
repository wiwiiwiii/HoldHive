export type PriceStatus = 'LIVE' | 'CACHED' | 'DEMO' | 'UNAVAILABLE';

export interface HoldingResponse {
  id: number;
  ticker: string;
  quantity: number;
  averagePurchasePrice: number;
  currentPrice?: number;
  marketValue?: number;
  unrealizedGainLoss?: number;
  allocationPercent?: number;
  priceStatus: PriceStatus;
}

export interface PortfolioSummaryResponse {
  totalCost: number;
  totalMarketValue: number;
  unrealizedGainLoss: number;
  pricedHoldingCount: number;
  totalHoldingCount: number;
  priceStatus: PriceStatus;
}
