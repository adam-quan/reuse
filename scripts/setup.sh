#!/usr/bin/env bash
# One-time setup against a running dev server (`make server`):
#   - creates the Nexus endpoint that routes platform.v1 calls into the
#     platform namespace / task queue.
set -euo pipefail

ADDR="${TEMPORAL_ADDRESS:-localhost:7233}"
ENDPOINT="platform-services-endpoint"

if temporal operator nexus endpoint get --address "$ADDR" --name "$ENDPOINT" >/dev/null 2>&1; then
  echo "Nexus endpoint '$ENDPOINT' already exists"
else
  temporal operator nexus endpoint create \
    --address "$ADDR" \
    --name "$ENDPOINT" \
    --target-namespace platform \
    --target-task-queue platform-services \
    --description-file <(printf '%s\n' \
      '## Platform services (platform.v1)' \
      '' \
      '- `send-email` -> standalone activity `notifications.SendEmail.v1`' \
      '- `process-payment` -> workflow `payments.ProcessPayment.v1`' \
      '' \
      'Contract: `com.example:platform-api` (PlatformService.java)')
  echo "Created Nexus endpoint '$ENDPOINT' -> platform/platform-services"
fi
