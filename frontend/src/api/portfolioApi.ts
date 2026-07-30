import type {
  FundLookthroughResponse,
  HoldingResponse,
  MarketQuoteResult,
  MarketSearchResult,
  PortfolioAnalysisFacts,
  PortfolioExposure,
  PortfolioSummaryResponse,
  PriceMode,
} from './types';

export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  status: number;
  detail: string;

  constructor(action: string, status: number, detail: string) {
    super(`${action} failed (HTTP ${status}): ${detail}`);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
  }
}

function extractErrorDetail(payload: unknown): string | null {
  if (typeof payload === 'string') {
    return payload.trim() || null;
  }

  if (!payload || typeof payload !== 'object') {
    return null;
  }

  const record = payload as Record<string, unknown>;
  for (const key of ['message', 'detail', 'reason', 'error', 'title']) {
    const value = record[key];
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }

  const errors = record.errors;
  if (errors && typeof errors === 'object') {
    const details = Object.entries(errors as Record<string, unknown>)
        .flatMap(([field, value]) => {
          if (Array.isArray(value)) {
            return value.map((item) => `${field}: ${String(item)}`);
          }
          return [`${field}: ${String(value)}`];
        })
        .filter(Boolean);
    if (details.length > 0) {
      return details.join('; ');
    }
  }

  return null;
}

async function throwApiError(response: Response, action: string): Promise<never> {
  const fallbackDetail = response.statusText || 'Request failed';
  let detail = fallbackDetail;

  try {
    const text = await response.text();
    if (text.trim()) {
      try {
        detail = extractErrorDetail(JSON.parse(text)) ?? fallbackDetail;
      } catch {
        detail = text.replace(/\s+/g, ' ').trim().slice(0, 200) || fallbackDetail;
      }
    }
  } catch {
    detail = fallbackDetail;
  }

  throw new ApiError(action, response.status, detail);
}

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
    await throwApiError(response, 'Add holding');
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
    await throwApiError(response, 'Update holding');
  }

  return response.json();
}

export async function deleteHolding(id: number): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/holdings/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    await throwApiError(response, 'Remove holding');
  }

  return response.status;
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

export interface AnalysisInsightStreamOptions {
  onToken?: (text: string) => void;
  onDone?: () => void;
  onError?: (error: Error) => void;
  signal?: AbortSignal;
  priceMode?: PriceMode;
}

export async function fetchAnalysisInsightsFull(
    options?: ((text: string) => void) | AnalysisInsightStreamOptions
): Promise<PortfolioAnalysisFacts> {
  return new Promise((resolve, reject) => {
    let facts: PortfolioAnalysisFacts | null = null;
    let isClosed = false;
    const streamOptions: AnalysisInsightStreamOptions =
        typeof options === 'function' ? { onToken: options } : options ?? {};
    const params = new URLSearchParams({
      priceMode: streamOptions.priceMode ?? 'BEST_AVAILABLE',
    });
    const es = new EventSource(`${API_BASE_URL}/portfolio/analysis/insights/full?${params.toString()}`);

    const closeStream = () => {
      if (isClosed) return;
      isClosed = true;
      es.close();
      streamOptions.signal?.removeEventListener('abort', handleAbort);
    };

    const handleAbort = () => {
      closeStream();
      if (!facts) {
        reject(new Error('Analysis stream cancelled'));
      }
    };

    if (streamOptions.signal?.aborted) {
      closeStream();
      reject(new Error('Analysis stream cancelled'));
      return;
    }

    streamOptions.signal?.addEventListener('abort', handleAbort);

    es.addEventListener('facts', (e) => {
      const { payload } = JSON.parse((e as MessageEvent).data);
      facts = payload;
      if (facts) resolve(facts);
    });

    es.addEventListener('token', (e) => {
      const { payload } = JSON.parse((e as MessageEvent).data);
      streamOptions.onToken?.(payload);
    });

    es.addEventListener('done', () => {
      closeStream();
      streamOptions.onDone?.();
    });

    es.onerror = () => {
      const error = new Error('Failed to connect to analysis endpoint');
      closeStream();
      streamOptions.onError?.(error);
      if (!facts) {
        reject(error);
      }
    };
  });
}
