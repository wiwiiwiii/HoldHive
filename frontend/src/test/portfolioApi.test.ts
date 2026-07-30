import { afterEach, describe, expect, it, vi } from 'vitest';

import { createHolding, deleteHolding } from '../api/portfolioApi';

describe('portfolioApi holding mutations', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('turns create-holding JSON failures into concise status-aware messages', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({
        timestamp: '2026-07-30T11:00:00Z',
        status: 409,
        error: 'Conflict',
        message: 'AAPL already exists in this portfolio',
        path: '/api/v1/holdings',
      }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    )));

    await expect(createHolding({
      assetType: 'STOCK',
      ticker: 'AAPL',
      quantity: 1,
      averagePurchasePrice: 100,
    })).rejects.toMatchObject({
      status: 409,
      message: 'Add holding failed (HTTP 409): AAPL already exists in this portfolio',
    });

    await expect(createHolding({
      assetType: 'STOCK',
      ticker: 'AAPL',
      quantity: 1,
      averagePurchasePrice: 100,
    })).rejects.not.toThrow(/timestamp|path|\{|\}/);
  });

  it('turns delete-holding JSON failures into concise status-aware messages', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({
        status: 404,
        error: 'Not Found',
        detail: 'Holding 99 was not found',
      }),
      { status: 404, headers: { 'Content-Type': 'application/json' } },
    )));

    await expect(deleteHolding(99)).rejects.toMatchObject({
      status: 404,
      message: 'Remove holding failed (HTTP 404): Holding 99 was not found',
    });
  });

  it('returns delete-holding success status for user-facing success messages', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 204 })));

    await expect(deleteHolding(99)).resolves.toBe(204);
  });
});
