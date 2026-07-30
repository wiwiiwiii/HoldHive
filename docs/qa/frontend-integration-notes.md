# Frontend Integration Notes

Use this as a regression reference for the implemented holding update flow in `1.0.0`.

Implementation status:

- `frontend/src/api/portfolioApi.ts` includes `updateHolding(id, request, priceMode?)`.
- `frontend/src/components/HoldingsPage.tsx` supports editing quantity and average purchase price.
- Successful updates refresh holdings and trigger analysis refresh through the shared holdings-change callback.
- Cash and bank deposit keep fixed average purchase price/current price at `1.00`.

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

## Frontend Regression Notes

1. Edit a stock holding and confirm quantity, average purchase price, cost basis, market value, and floating P&L update after refresh.
2. Edit a cash or bank-deposit holding and confirm backend keeps average purchase price/current price fixed at `1.00`.
3. Confirm failed validation shows a short user-facing message instead of raw JSON.
4. Confirm Settings data mode is passed to the update request and subsequent refresh.
5. Confirm Analysis refreshes after a successful edit.

## Local Smoke Request

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/holdings/{id}?priceMode=DEMO_ALLOWED" \
  -H "Content-Type: application/json" \
  -d '{"quantity":8,"averagePurchasePrice":320}'
```

Expected result: `200 OK` and refreshed holding valuation fields.
