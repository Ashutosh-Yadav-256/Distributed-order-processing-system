# Distributed Order Platform — Model Context Protocol (MCP) Server

An official **Model Context Protocol (MCP)** server providing AI assistants (**Antigravity**, **Claude Desktop**, **Cursor**, etc.) with direct, tool-based access to the **Distributed Order Processing System**.

---

## Key Highlights
* **Standard JSON-RPC 2.0 (stdio)**: Compliant with the 2024-11-05 MCP specification.
* **Zero External Dependencies**: Implemented using pure Python standard library (`sys`, `json`, `urllib`). No `pip install` required — runs immediately on any system with Python 3.10+.
* **Dual Runtime Target**: Seamlessly talks to either the Spring Cloud Gateway (`http://localhost:8080`) or the QA Test Harness (`http://localhost:4000`).

---

## Available MCP Tools

| Tool Name | Parameters | Description |
| :--- | :--- | :--- |
| **`create_order`** | `items`, `customerEmail`, `customerId`, `paymentMethod` | Places an order and executes the distributed Saga (Stock Reservation $\rightarrow$ Payment Authorization $\rightarrow$ S3 Invoice Archival $\rightarrow$ Email Notification). |
| **`get_order_status`**| `orderId` | Retrieves order state, payment transaction, stock reservations, and dispatched notifications. |
| **`check_inventory`** | `productId` (optional) | Queries product catalog stock levels, available vs reserved counts, prices, and Redis cache hit/miss status. |
| **`get_s3_invoices`**  | `orderId` (optional) | Lists customer order invoices archived in the Amazon S3 bucket (Floci port 4566). |
| **`run_saga_scenario`**| `scenario` (`happy_path`, `out_of_stock`, `payment_decline`, `idempotency`) | Runs automated Saga test scenarios and returns step-by-step transaction assertions. |
| **`get_system_health`**| *(none)* | Checks real-time health across all 6 microservices and the Floci AWS emulator. |

---

## Available MCP Resources

* **`order://catalog`**: Real-time snapshot of products in stock with available and reserved quantities.
* **`order://system-health`**: Telemetry and health metrics across all services.

---

## How to Use & Configure

### 1. In Antigravity (`.agents/mcp_config.json` or `~/.gemini/config/mcp_config.json`)
```json
{
  "mcpServers": {
    "order-platform": {
      "command": "python",
      "args": ["mcp-server/server.py"],
      "env": {
        "PLATFORM_API_BASE": "http://localhost:4000"
      }
    }
  }
}
```

### 2. In Claude Desktop (`claude_desktop_config.json`)
* **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
* **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "order-platform": {
      "command": "python",
      "args": [
        "C:\\Desktop\\CODING _IS_LIFE\\1 ANTI GRAVITY\\Distributed Order Processing System\\mcp-server\\server.py"
      ],
      "env": {
        "PLATFORM_API_BASE": "http://localhost:4000"
      }
    }
  }
}
```

---

## Testing the MCP Server
Run the built-in automated test suite:
```bash
python mcp-server/test_mcp.py
```
Expected output:
```text
========================================================================
Starting Automated MCP Server Test Suite (stdio JSON-RPC 2.0)
========================================================================
[OK] Initialize passed: distributed-order-platform-mcp v1.0.0
[PASS] Discovered 6 tools: get_system_health, check_inventory, create_order, get_order_status, get_s3_invoices, run_saga_scenario
[PASS] Health check returned status: UP
[PASS] Inventory check returned 4 products in catalog.
[PASS] Happy path Saga executed via MCP tool
[PASS] Resources list returned 2 resources.
[PASS] Resource 'order://catalog' read successfully.
========================================================================
[PASS] ALL 6 MCP SERVER TESTS PASSED (100% SUCCESS)!
========================================================================
```
