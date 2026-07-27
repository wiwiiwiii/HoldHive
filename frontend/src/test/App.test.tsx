import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { App } from '../App';

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);

        if (url.includes('/funds/302/lookthrough')) {
          return {
            ok: true,
            json: async () => ({
              fundInstrumentId: 302,
              ticker: 'VOO',
              displayName: 'Vanguard S&P 500 ETF',
              assetType: 'ETF',
              asOfDate: '2026-06-30',
              source: 'DEMO_DISCLOSURE',
              coveragePercent: 41.15,
              holdings: [
                {
                  ticker: 'MSFT',
                  displayName: 'Microsoft Corp.',
                  assetType: 'STOCK',
                  weightPercent: 6.65
                }
              ],
              warnings: ['Fund holdings are based on the latest available disclosure.']
            })
          };
        }

        return {
          ok: true,
          json: async () => ({
          portfolioId: 1,
          portfolioName: 'My Portfolio',
          baseCurrency: 'USD',
          holdingCount: 5,
          pricedHoldingCount: 5,
          valuationStatus: 'COMPLETE',
          totalCostBasis: 10175,
          totalMarketValue: 10623.3,
          totalUnrealizedGainLoss: 347.5,
          totalUnrealizedGainLossPercent: 3.75472717,
          priceAsOf: '2026-07-24T08:29:00Z',
          allocations: [
            {
              holdingId: 101,
              assetType: 'STOCK',
              ticker: 'AAPL',
              marketValue: 2102.5,
              allocationPercent: 21.89533976
            },
            {
              holdingId: 302,
              assetType: 'ETF',
              ticker: 'VOO',
              marketValue: 1020.8,
              allocationPercent: 9.60909981
            },
            {
              holdingId: 301,
              assetType: 'CRYPTO',
              ticker: 'BTC',
              marketValue: 1000,
              allocationPercent: 10.4139547
            },
            {
              holdingId: 201,
              assetType: 'CASH',
              ticker: 'USD',
              marketValue: 4500,
              allocationPercent: 46.86279615
            },
            {
              holdingId: 202,
              assetType: 'BANK_DEPOSIT',
              ticker: 'HSBC_USD',
              marketValue: 2000,
              allocationPercent: 20.8279094
            }
          ],
          unpricedHoldings: []
        })
        };
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the multi-asset portfolio dashboard from the summary API', async () => {
    render(<App />);

    expect(
      await screen.findByRole('heading', { name: /holdhive portfolio dashboard/i })
    ).toBeInTheDocument();
    expect(screen.getByText('$10,623.30')).toBeInTheDocument();
    expect(screen.getByText('STOCK')).toBeInTheDocument();
    expect(screen.getByText('CRYPTO')).toBeInTheDocument();
    expect(screen.getByText('CASH')).toBeInTheDocument();
    expect(screen.getByText('BANK_DEPOSIT')).toBeInTheDocument();
    expect(screen.getByText(/funds can contain underlying stocks/i)).toBeInTheDocument();
    expect(await screen.findByText('Microsoft Corp.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add holding/i })).toBeInTheDocument();
  });
});
