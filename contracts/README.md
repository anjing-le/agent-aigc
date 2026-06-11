# Contracts

This directory stores machine-readable platform contracts.

- `platform-contract.json`: stable cross-frontend/backend contract for API prefix, response envelope, pagination, request context headers, time strategy, and error code ranges.
- `service-boundaries.json`: machine-readable service/module boundary map for API ownership, current host, future microservice split, and runtime routes.

Generated constants:

- `backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java`
- `backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java`
- `frontend/src/contracts/platform-contract.ts`
- `frontend/src/contracts/service-boundaries.ts`

Update flow:

```bash
node scripts/generate-platform-contract-backend.js
node scripts/generate-platform-contract-frontend.js
node scripts/generate-service-boundaries-backend.js
node scripts/generate-service-boundaries-frontend.js
node scripts/check-platform-contract.js
node scripts/check-service-boundaries.js
```
