.PHONY: server setup build test platform-worker orders-worker checkout-worker checkout marketing orders

MVN  := mvn -q
EXEC := exec:java -Dexec.cleanupDaemonThreads=false

# Local dev server with one namespace per team. Needs a recent Temporal CLI (>= 1.9.1 for Standalone Activities)
# (see README: Nexus operations backed by standalone activities).
server:
	temporal server start-dev --namespace platform --namespace orders --namespace checkout

# One-time: create the Nexus endpoint (platform-services-endpoint -> platform/platform-services).
setup:
	./scripts/setup.sh

build:
	$(MVN) install -DskipTests

test:
	mvn test

# --- long-running workers (one terminal each) ---
platform-worker:
	$(MVN) -pl platform-service $(EXEC) -Dexec.mainClass=com.example.platform.service.PlatformWorker

orders-worker:
	$(MVN) -pl team-orders $(EXEC) -Dexec.mainClass=com.example.orders.OrdersWorker

checkout-worker:
	$(MVN) -pl team-checkout $(EXEC) -Dexec.mainClass=com.example.checkout.CheckoutWorker

# --- demo drivers ---
checkout:   # mode 1: embedded library (child workflow + in-process activity)
	$(MVN) -pl team-checkout $(EXEC) -Dexec.mainClass=com.example.checkout.CheckoutStarter

marketing:  # mode 2: standalone activities from plain application code
	$(MVN) -pl team-marketing $(EXEC) -Dexec.mainClass=com.example.marketing.CampaignSender

orders:     # mode 3: Nexus (workflow-backed + standalone-activity-backed operations)
	$(MVN) -pl team-orders $(EXEC) -Dexec.mainClass=com.example.orders.OrdersStarter
