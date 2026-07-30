import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';

import { fetchAnalysisInsightsFull } from '../api/portfolioApi';
import { AnalysisPage } from '../components/AnalysisPage';

vi.mock('../api/portfolioApi', () => {
  const facts = {
    overview: { totalMarketValue: 1000, allocations: [] },
    concentration: {
      hhi: 0,
      topHoldingTicker: null,
      topHoldingPercent: 0,
      riskLevel: 'LOW',
      holdingCount: 0,
      topHoldings: [],
      topHoldingsCombinedPercent: 0,
    },
    fundOverlap: {
      funds: [],
      unavailableFunds: [],
      totalOverlapMarketValue: 0,
      totalOverlapPercentOfPortfolio: 0,
    },
    lookThrough: {
      items: [],
      lookThroughHhi: 0,
      lookThroughRiskLevel: 'LOW',
      topTicker: null,
      topPercent: 0,
      attributedPercentOfPortfolio: 0,
    },
    sectorExposure: {
      sectors: [],
      sectorHhi: 0,
      sectorRiskLevel: 'LOW',
      topSector: null,
      topSectorPercent: 0,
      attributedPercentOfPortfolio: 0,
    },
    profitLoss: {
      holdings: [],
      totalCostBasis: 1000,
      totalMarketValue: 1000,
      totalUnrealizedPnl: 0,
      totalUnrealizedPnlPercent: 0,
      bestPerformerTicker: null,
      bestPerformerPnlPercent: null,
      worstPerformerTicker: null,
      worstPerformerPnlPercent: null,
      missingCostBasisTickers: [],
    },
  };

  return {
    fetchAnalysisInsightsFull: vi.fn(async (options?: { onToken?: (text: string) => void; onDone?: () => void }) => {
      options?.onToken?.('### Overview\n- Portfolio refreshed.\n');
      options?.onDone?.();
      return facts;
    }),
    fetchPortfolioExposure: vi.fn(async () => ({
      portfolioId: 1,
      portfolioName: 'Demo',
      baseCurrency: 'USD',
      lookthrough: true,
      priceMode: 'BEST_AVAILABLE',
      totalMarketValue: 1000,
      items: [],
      warnings: [],
    })),
  };
});

describe('AnalysisPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('refreshes automatically when holdings change and also supports manual refresh', async () => {
    const { rerender } = render(<AnalysisPage refreshTrigger={0} />);

    await waitFor(() => expect(fetchAnalysisInsightsFull).toHaveBeenCalledTimes(1));

    rerender(<AnalysisPage refreshTrigger={1} />);

    await waitFor(() => expect(fetchAnalysisInsightsFull).toHaveBeenCalledTimes(2));

    fireEvent.click(screen.getByRole('button', { name: /refresh analysis/i }));

    await waitFor(() => expect(fetchAnalysisInsightsFull).toHaveBeenCalledTimes(3));
  });
});
