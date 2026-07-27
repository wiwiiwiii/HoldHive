# HoldHive C4 Context

```text
User
  |
  v
React Frontend
  |
  v
Spring Boot REST API
  |
  +--> MySQL
  |
  +--> Market Data Adapter
```

## Notes

- The backend owns portfolio calculations and persistence.
- The frontend displays API results and does not duplicate valuation formulas.
- Market data starts in demo mode and can be swapped through adapter implementations.
