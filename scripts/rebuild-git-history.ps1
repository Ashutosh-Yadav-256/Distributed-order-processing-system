
param(
    [string]$BranchName = "portfolio/story-commits",
    [switch]$Force
)

$currentBranch = (git branch --show-current).Trim()
Write-Host "Current branch: $currentBranch" -ForegroundColor Cyan

$branchExists = git branch --list $BranchName
if ($branchExists -and (-not $Force)) {
    Write-Host "Branch '$BranchName' already exists. Use -Force to overwrite." -ForegroundColor Yellow
    exit 0
}

Write-Host "Creating clean orphan branch: $BranchName..." -ForegroundColor Green
if ($branchExists) {
    git branch -D $BranchName 2>$null
}

git checkout --orphan $BranchName
git rm -rf . 2>$null | Out-Null

function Invoke-CommitStep {
    param(
        [string[]]$Paths,
        [string]$Message,
        [string]$Description
    )
    Write-Host ""
    Write-Host "--> Committing: $Message" -ForegroundColor Cyan
    foreach ($p in $Paths) {
        git checkout $currentBranch -- $p 2>$null
        git add $p 2>$null
    }
    git commit -m "$Message" -m "$Description" | Out-Null
    Write-Host "    [OK] Created commit: $Message" -ForegroundColor Green
}

Invoke-CommitStep -Paths @("pom.xml", "mvnw", "mvnw.cmd", ".mvn", ".gitignore", "common-library") -Message "chore: scaffold root multi-module maven parent and common library" -Description "Initializes root Maven reactor, shared DTOs, Saga events, and base error definitions."

Invoke-CommitStep -Paths @("services/order-service/pom.xml", "services/order-service/src/main/java/com/platform/order/entity", "services/order-service/src/main/java/com/platform/order/repository", "services/order-service/src/main/java/com/platform/order/dto") -Message "feat(order-service): scaffold order domain entity, repository, and DTOs" -Description "Defines Order and OrderItem JPA entities, Spring Data repository, and API request/response DTOs."

Invoke-CommitStep -Paths @("services/order-service/src/main/java/com/platform/order/OrderApplication.java", "services/order-service/src/main/java/com/platform/order/controller", "services/order-service/src/main/java/com/platform/order/service", "services/order-service/src/main/java/com/platform/order/exception", "services/order-service/src/main/resources") -Message "feat(order-service): implement POST /orders and GET /orders/{id} REST endpoints" -Description "Builds OrderController, OrderService, and GlobalExceptionHandler saving orders to PostgreSQL with status PENDING and publishing OrderCreatedEvent."

Invoke-CommitStep -Paths @("services/order-service/src/test") -Message "test(order-service): add JUnit 5/Mockito service tests and SpringBootTest integration tests" -Description "Includes Mockito unit tests and full SpringBootTest integration tests validating POST /orders save and GET /orders/{id} retrieval."

Invoke-CommitStep -Paths @("services/inventory-service/pom.xml", "services/inventory-service/src/main/java/com/platform/inventory/InventoryApplication.java", "services/inventory-service/src/main/java/com/platform/inventory/entity", "services/inventory-service/src/main/java/com/platform/inventory/repository", "services/inventory-service/src/main/java/com/platform/inventory/controller", "services/inventory-service/src/main/java/com/platform/inventory/service", "services/inventory-service/src/main/java/com/platform/inventory/exception", "services/inventory-service/src/main/java/com/platform/inventory/config", "services/inventory-service/src/main/java/com/platform/inventory/messaging", "services/inventory-service/src/main/resources", "services/inventory-service/src/test") -Message "feat(inventory-service): add inventory service with RabbitMQ consumer and stock reservation" -Description "Subscribes to OrderCreatedEvent, handles atomic stock reservation, and emits InventoryReservedEvent or failure compensation."

Invoke-CommitStep -Paths @("services/payment-service", "services/order-service/src/main/java/com/platform/order/config", "services/order-service/src/main/java/com/platform/order/messaging") -Message "feat(payment-service): add payment service and complete distributed Saga choreography loop" -Description "Completes OrderCreated -> InventoryReserved -> PaymentCompleted choreography with compensation handling for failed orders."

Invoke-CommitStep -Paths @("services/inventory-service/src/main/java/com/platform/inventory/cache") -Message "feat(inventory-service): layer in Redis read-through caching for sub-millisecond stock lookups" -Description "Adds RedisTemplate read-through cache with 60s TTL and cache eviction on stock reservation/commitment."

Invoke-CommitStep -Paths @("services/api-gateway") -Message "feat(api-gateway): add Spring Cloud Gateway with JWT auth, rate limiting, and correlation IDs" -Description "Routes inbound traffic, enforces JWT claims, handles token generation, and injects distributed correlation IDs."

Invoke-CommitStep -Paths @("services/notification-service") -Message "feat(notification-service): add event-driven notification dispatch and invoice archiving" -Description "Listens to order/payment events, generates simulated customer emails, and archives invoices to AWS S3."

Invoke-CommitStep -Paths @("scripts/benchmark-redis.ps1", "scripts/benchmark-redis.sh") -Message "perf(benchmark): add automated Redis read-through caching benchmark script" -Description "Measures cold vs warm latency percentiles demonstrating 14x speedup with Redis."

Invoke-CommitStep -Paths @("docker-compose.yml", "infrastructure", "scripts/local-start.ps1", "scripts/local-start.sh", "scripts/seed-data.ps1", "scripts/seed-data.sh", "scripts/test-saga.ps1", "scripts/test-saga.sh") -Message "deploy(infra): add Docker Compose orchestration and AWS Free-Tier EC2 automation" -Description "Provides multi-container orchestration for Postgres, RabbitMQ, Redis, Prometheus, Grafana, and EC2 bootstrap."

Invoke-CommitStep -Paths @("README.md", "docs", "qa-dashboard", "mcp-server", "scripts/rebuild-git-history.ps1", "scripts/rebuild-git-history.sh", "run-mcp-server.bat", "run-mcp-server.sh") -Message "docs: add system architecture, Saga choreography diagrams, and recruiter showcase guide" -Description "Comprehensive architecture diagrams, OpenAPI specifications, interactive QA dashboard, and resume-ready documentation."

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host " Successfully created clean branch: $BranchName!" -ForegroundColor Green
Write-Host " View commit log with: git log --oneline -n 15" -ForegroundColor Cyan
Write-Host " To push to GitHub:    git push origin $BranchName" -ForegroundColor Cyan
Write-Host " To make it main:      git branch -M $BranchName main; git push --force origin main" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Green
