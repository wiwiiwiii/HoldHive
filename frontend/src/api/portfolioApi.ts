import type { FundLookthroughResponse, PortfolioSummaryResponse } from './types';

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export async function fetchPortfolioSummary(): Promise<PortfolioSummaryResponse> {
  const response = await fetch(`${API_BASE_URL}/portfolio/summary?priceMode=DEMO_ALLOWED`);

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
