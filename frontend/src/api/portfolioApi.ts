import type { FundLookthroughResponse, HoldingResponse, PortfolioSummaryResponse } from './types';

export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export async function fetchPortfolioSummary(): Promise<PortfolioSummaryResponse> {
  const response = await fetch(`${API_BASE_URL}/portfolio/summary?priceMode=BEST_AVAILABLE`);

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

export interface CreateHoldingRequest {
  assetType: string;
  ticker: string;
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

export async function fetchHoldings(): Promise<HoldingResponse[]> {
  const response = await fetch(`${API_BASE_URL}/holdings`);

  if (!response.ok) {
    throw new Error(`Failed to fetch holdings`);
  }

  const envelope: HoldingListEnvelope = await response.json();
  return envelope.items;
}

export async function fetchHoldingsFull(): Promise<HoldingResponse[]> {
  const response = await fetch(`${API_BASE_URL}/holdings`);

  if (!response.ok) {
    throw new Error(`Failed to fetch holdings`);
  }

  const envelope: HoldingListEnvelope = await response.json();
  return envelope.items;
}
