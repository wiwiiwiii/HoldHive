# Frontend Integration Notes

Use this after the backend holding update PR is merged into `qa`.

## Backend Contract Added

`PATCH /api/v1/holdings/{holdingId}`

Query parameter:

- `priceMode` is optional. Use `DEMO_ALLOWED` during local demo checks when the UI expects demo prices in the response.

Request body:

```json
{
  "quantity": 8,
  "averagePurchasePrice": 320
}
```

Success response:

- `200 OK`
- Body is the updated `HoldingResponse`.
- For stock, ETF, mutual fund, and crypto holdings, `averagePurchasePrice` is updated from the request.
- For `CASH` and `BANK_DEPOSIT`, backend keeps `averagePurchasePrice` at `1.00000000`.

Error responses:

- `400 VALIDATION_FAILED` for missing, zero, or negative quantity.
- `400 VALIDATION_FAILED` for missing or negative average purchase price.
- `404 HOLDING_NOT_FOUND` when the id does not exist.

## Member C Next Steps

1. Add an `updateHolding(id, request, priceMode?)` function in `frontend/src/api/portfolioApi.ts`.
2. Add a small `UpdateHoldingRequest` type with `quantity` and `averagePurchasePrice`.
3. Add an edit action in `HoldingsPage` beside delete, reusing the existing table row data as initial form values.
4. After successful update, refresh both holdings and portfolio summary instead of calculating totals in the browser.
5. Display API validation messages through the existing toast/error UI, not as raw JSON.
6. For cash and bank deposit rows, either disable the average price field or show a hint that backend keeps it fixed at `1.00`.

## Local Smoke Request

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/holdings/{id}?priceMode=DEMO_ALLOWED" \
  -H "Content-Type: application/json" \
  -d '{"quantity":8,"averagePurchasePrice":320}'
```

Expected result: `200 OK` and refreshed holding valuation fields.
