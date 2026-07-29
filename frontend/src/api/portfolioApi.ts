import type {
  FundLookthroughResponse,
  HoldingResponse,
  MarketQuoteResult,
  MarketSearchResult,
  PortfolioExposure,
  PortfolioSummaryResponse,
  PriceMode,
} from './types';

export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export async function fetchPortfolioSummary(priceMode: PriceMode = 'BEST_AVAILABLE'): Promise<PortfolioSummaryResponse> {
  const response = await fetch(`${API_BASE_URL}/portfolio/summary?priceMode=${priceMode}`);

  if (!response.ok) {
    throw new Error(`Portfolio summary request failed with HTTP ${response.status}`);
  }

  return response.json();
}

export async function fetchFundLookthrough(instrumentId: number): Promise<FundLookthroughResponse> {
  const response = await fetch(`${API_BASE_URL}/funds/${instrumentId}/lookthrough`);

  if (!response.ok) {
    throw new Error(`Fund lookthrough request failed with HTTP ${response.status}`);
  }

  return response.json();
}

export async function fetchPortfolioExposure(
    lookthrough = true,
    priceMode: PriceMode = 'BEST_AVAILABLE'
): Promise<PortfolioExposure> {
  const response = await fetch(
      `${API_BASE_URL}/portfolio/exposure?lookthrough=${lookthrough}&priceMode=${priceMode}`
  );

  if (!response.ok) {
    throw new Error(`Portfolio exposure request failed with HTTP ${response.status}`);
  }

  return response.json();
}

export async function searchMarket(query: string, market?: string): Promise<MarketSearchResult> {
  const params = new URLSearchParams({ query });
  if (market) params.append('market', market);
  const response = await fetch(`${API_BASE_URL}/market/search?${params.toString()}`);

  if (!response.ok) {
    throw new Error(`Market search request failed with HTTP ${response.status}`);
  }

  return response.json();
}

export async function fetchMarketQuotes(
    providerQuoteIds: string[],
    priceMode: PriceMode = 'BEST_AVAILABLE'
): Promise<MarketQuoteResult> {
  const params = new URLSearchParams({
    providerQuoteIds: providerQuoteIds.join(','),
    priceMode,
  });
  const response = await fetch(`${API_BASE_URL}/market/quotes?${params.toString()}`);

  if (!response.ok) {
    throw new Error(`Market quotes request failed with HTTP ${response.status}`);
  }

  return response.json();
}

export interface CreateHoldingRequest {
  assetType: string;
  ticker: string;
  exchangeCode?: string;
  displayName?: string;
  providerQuoteId?: string;
  currency?: string;
  quantity: number;
  averagePurchasePrice: number;
}

export async function createHolding(request: CreateHoldingRequest): Promise<HoldingResponse> {
  const response = await fetch(`${API_BASE_URL}/holdings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to create holding: ${errorText}`);
  }

  return response.json();
}

export interface UpdateHoldingRequest {
  quantity: number;
  averagePurchasePrice: number;
}

export async function updateHolding(
    id: number,
    request: UpdateHoldingRequest,
    priceMode: PriceMode = 'BEST_AVAILABLE'
): Promise<HoldingResponse> {
  const response = await fetch(`${API_BASE_URL}/holdings/${id}?priceMode=${priceMode}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to update holding: ${errorText}`);
  }

  return response.json();
}

export async function deleteHolding(id: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/holdings/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error(`Failed to delete holding ${id}`);
  }
}

interface HoldingListEnvelope {
  items: HoldingResponse[];
  count: number;
}

export async function fetchHoldings(priceMode: PriceMode = 'BEST_AVAILABLE'): Promise<HoldingResponse[]> {
  const response = await fetch(`${API_BASE_URL}/holdings?priceMode=${priceMode}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch holdings`);
  }

  const envelope: HoldingListEnvelope = await response.json();
  return envelope.items;
}

export async function fetchHoldingsFull(priceMode: PriceMode = 'BEST_AVAILABLE'): Promise<HoldingResponse[]> {
  const response = await fetch(`${API_BASE_URL}/holdings?priceMode=${priceMode}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch holdings`);
  }

  const envelope: HoldingListEnvelope = await response.json();
  return envelope.items;
}
