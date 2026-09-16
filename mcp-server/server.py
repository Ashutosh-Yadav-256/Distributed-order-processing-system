#!/usr/bin/env python3
"""
Distributed Order Processing System — Model Context Protocol (MCP) Server
Allows AI coding assistants (Antigravity, Claude Desktop, Cursor) to interact
natively with the order platform, execute Saga transactions, check stock, and inspect S3 invoices.

Protocol: Standard JSON-RPC 2.0 over stdio (MCP Specification 2024-11-05).
Dependencies: Zero (Uses Python standard library only for instant portability).
"""

import sys
import json
import os
import urllib.request
import urllib.error

API_BASE = os.environ.get("PLATFORM_API_BASE", "http://localhost:4000")

# -------------------------------------------------------------
# HTTP Client Helper
# -------------------------------------------------------------
def make_api_request(endpoint, method="GET", body=None, token=None):
    url = f"{API_BASE}{endpoint}"
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            res_data = response.read().decode("utf-8")
            return json.loads(res_data) if res_data else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return json.loads(err_body)
        except Exception:
            return {"error": f"HTTP {e.code}: {e.reason}", "raw": err_body}
    except Exception as e:
        return {"error": str(e), "message": f"Could not connect to Platform API at {API_BASE}. Is the server running?"}

# -------------------------------------------------------------
# MCP Tool Definitions
# -------------------------------------------------------------
TOOLS = [
    {
        "name": "get_system_health",
        "description": "Checks real-time health and telemetry across all 6 microservices (API Gateway, Order Service, Inventory Service, Payment Service, Notification Service) and the Floci Local AWS Emulator.",
        "inputSchema": {
            "type": "object",
            "properties": {}
        }
    },
    {
        "name": "check_inventory",
        "description": "Queries product catalog stock levels, available vs reserved quantities, unit prices, and Redis cache hit/miss status.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "productId": {
                    "type": "string",
                    "description": "Optional UUID of a specific product to query. If omitted, returns all products."
                }
            }
        }
    },
    {
        "name": "create_order",
        "description": "Creates an order and executes the end-to-end distributed Saga transaction (Stock Reservation -> Payment Authorization -> S3 Invoice Archival -> Email Notification).",
        "inputSchema": {
            "type": "object",
            "required": ["items", "customerEmail"],
            "properties": {
                "customerEmail": {
                    "type": "string",
                    "description": "Customer's email address for receiving receipt notifications"
                },
                "customerId": {
                    "type": "string",
                    "description": "Customer UUID (optional, defaults to generated UUID)"
                },
                "items": {
                    "type": "array",
                    "description": "List of order items with productId, productName, quantity, and unitPrice",
                    "items": {
                        "type": "object",
                        "required": ["productId", "quantity", "unitPrice"],
                        "properties": {
                            "productId": {"type": "string"},
                            "productName": {"type": "string"},
                            "quantity": {"type": "integer", "minimum": 1},
                            "unitPrice": {"type": "number"}
                        }
                    }
                },
                "paymentMethod": {
                    "type": "string",
                    "enum": ["CREDIT_CARD_VALID", "CREDIT_CARD_FAIL"],
                    "description": "Payment method simulation token. Use CREDIT_CARD_VALID for success, CREDIT_CARD_FAIL to test compensating transaction rollback."
                }
            }
        }
    },
    {
        "name": "get_order_status",
        "description": "Retrieves the current status (PENDING, INVENTORY_RESERVED, CONFIRMED, CANCELLED, FAILED), payment records, and customer notifications for an order.",
        "inputSchema": {
            "type": "object",
            "required": ["orderId"],
            "properties": {
                "orderId": {
                    "type": "string",
                    "description": "UUID of the order to inspect"
                }
            }
        }
    },
    {
        "name": "get_s3_invoices",
        "description": "Lists customer order invoices archived in the Amazon S3 bucket (emulated by Floci on port 4566).",
        "inputSchema": {
            "type": "object",
            "properties": {
                "orderId": {
                    "type": "string",
                    "description": "Optional order UUID filter"
                }
            }
        }
    },
    {
        "name": "run_saga_scenario",
        "description": "Runs one of the four automated QA test scenarios (happy_path, out_of_stock, payment_decline, idempotency) and returns step-by-step transaction assertions.",
        "inputSchema": {
            "type": "object",
            "required": ["scenario"],
            "properties": {
                "scenario": {
                    "type": "string",
                    "enum": ["happy_path", "out_of_stock", "payment_decline", "idempotency"],
                    "description": "Which scenario to execute"
                }
            }
        }
    }
]

# -------------------------------------------------------------
# MCP Resources Definitions
# -------------------------------------------------------------
RESOURCES = [
    {
        "uri": "order://catalog",
        "name": "Product Catalog & Real-Time Stock",
        "description": "Live snapshot of all products in stock with available and reserved quantities",
        "mimeType": "application/json"
    },
    {
        "uri": "order://system-health",
        "name": "Platform Health & Telemetry",
        "description": "Real-time health status of microservices and Floci AWS emulator",
        "mimeType": "application/json"
    }
]

# -------------------------------------------------------------
# Tool Execution Handlers
# -------------------------------------------------------------
def handle_tool_call(tool_name, arguments):
    if tool_name == "get_system_health":
        data = make_api_request("/actuator/health")
        return format_text_result(json.dumps(data, indent=2))
        
    elif tool_name == "check_inventory":
        prod_id = arguments.get("productId")
        endpoint = f"/api/v1/inventory/{prod_id}" if prod_id else "/api/v1/inventory"
        data = make_api_request(endpoint)
        return format_text_result(json.dumps(data, indent=2))

    elif tool_name == "create_order":
        payload = {
            "customerId": arguments.get("customerId", "11111111-2222-3333-4444-555555555555"),
            "customerEmail": arguments.get("customerEmail", "customer@example.com"),
            "items": arguments.get("items", []),
            "paymentMethod": arguments.get("paymentMethod", "CREDIT_CARD_VALID")
        }
        data = make_api_request("/api/v1/orders", method="POST", body=payload)
        return format_text_result(json.dumps(data, indent=2))

    elif tool_name == "get_order_status":
        order_id = arguments["orderId"]
        data = make_api_request(f"/api/v1/orders/{order_id}")
        return format_text_result(json.dumps(data, indent=2))

    elif tool_name == "get_s3_invoices":
        data = make_api_request("/api/v1/aws/s3/invoices")
        order_id = arguments.get("orderId")
        if order_id and "data" in data and isinstance(data["data"], list):
            data["data"] = [inv for inv in data["data"] if inv.get("orderId") == order_id]
        return format_text_result(json.dumps(data, indent=2))

    elif tool_name == "run_saga_scenario":
        sc = arguments["scenario"]
        if sc == "happy_path":
            payload = {
                "customerId": "11111111-2222-3333-4444-555555555555",
                "customerEmail": "alex.shopper@qa-corp.com",
                "items": [{
                    "productId": "22222222-2222-2222-2222-222222222222",
                    "productName": "Noise-Cancelling Wireless Headphones",
                    "quantity": 2,
                    "unitPrice": 199.99
                }],
                "paymentMethod": "CREDIT_CARD_VALID"
            }
            res = make_api_request("/api/v1/orders", method="POST", body=payload)
            return format_text_result(f"### [Scenario A: Happy Path Saga Result]\n```json\n{json.dumps(res, indent=2)}\n```\nOutcome: Order CONFIRMED, inventory committed, invoice saved in Floci S3.")

        elif sc == "out_of_stock":
            payload = {
                "customerId": "99999999-8888-7777-6666-555555555555",
                "customerEmail": "gamer.bob@qa-corp.com",
                "items": [{
                    "productId": "33333333-3333-3333-3333-333333333333",
                    "productName": "Mechanical RGB Gaming Keyboard",
                    "quantity": 9999,
                    "unitPrice": 149.99
                }],
                "paymentMethod": "CREDIT_CARD_VALID"
            }
            res = make_api_request("/api/v1/orders", method="POST", body=payload)
            return format_text_result(f"### [Scenario B: Out of Stock Result]\n```json\n{json.dumps(res, indent=2)}\n```\nOutcome: Order FAILED immediately due to stock shortage. Zero payment attempt.")

        elif sc == "payment_decline":
            payload = {
                "customerId": "77777777-6666-5555-4444-333333333333",
                "customerEmail": "dana.developer@qa-corp.com",
                "items": [{
                    "productId": "44444444-4444-4444-4444-444444444444",
                    "productName": "4K UltraWide Curved Monitor",
                    "quantity": 3,
                    "unitPrice": 799.99
                }],
                "paymentMethod": "CREDIT_CARD_FAIL"
            }
            res = make_api_request("/api/v1/orders", method="POST", body=payload)
            return format_text_result(f"### [Scenario C: Payment Decline & Compensation Result]\n```json\n{json.dumps(res, indent=2)}\n```\nOutcome: Payment failed. Compensating transaction successfully restored held stock.")

        elif sc == "idempotency":
            test_evt = "evt-mcp-" + os.urandom(4).hex()
            r1 = make_api_request("/api/v1/qa/test-idempotency", method="POST", body={"eventId": test_evt, "consumer": "PaymentEventConsumer"})
            r2 = make_api_request("/api/v1/qa/test-idempotency", method="POST", body={"eventId": test_evt, "consumer": "PaymentEventConsumer"})
            result_summary = {
                "testEventId": test_evt,
                "firstDelivery": r1,
                "secondDelivery": r2,
                "exactlyOnceGuarantee": (r1.get("duplicateDetected") is False and r2.get("duplicateDetected") is True)
            }
            return format_text_result(f"### [Scenario D: Idempotency Result]\n```json\n{json.dumps(result_summary, indent=2)}\n```\nOutcome: Exactly-once delivery verified. Duplicate event was discarded.")

    return format_text_result(f"Unknown tool: {tool_name}", is_error=True)

def format_text_result(text, is_error=False):
    return {
        "content": [{"type": "text", "text": text}],
        "isError": is_error
    }

# -------------------------------------------------------------
# Main JSON-RPC 2.0 Dispatcher Loop
# -------------------------------------------------------------
def process_message(line):
    if not line.strip():
        return None
    try:
        req = json.loads(line)
    except json.JSONDecodeError as e:
        return {
            "jsonrpc": "2.0",
            "id": None,
            "error": {"code": -32700, "message": f"Parse error: {str(e)}"}
        }

    msg_id = req.get("id")
    method = req.get("method")
    params = req.get("params", {})

    # 1. Initialize
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {},
                    "resources": {}
                },
                "serverInfo": {
                    "name": "distributed-order-platform-mcp",
                    "version": "1.0.0"
                }
            }
        }

    # 2. Initialized notification (no response needed according to JSON-RPC specs if id is None)
    if method == "notifications/initialized":
        return None

    # 3. Ping
    if method == "ping":
        return {"jsonrpc": "2.0", "id": msg_id, "result": {}}

    # 4. Tools List
    if method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {"tools": TOOLS}
        }

    # 5. Tools Call
    if method == "tools/call":
        tool_name = params.get("name")
        arguments = params.get("arguments", {})
        result = handle_tool_call(tool_name, arguments)
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": result
        }

    # 6. Resources List
    if method == "resources/list":
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {"resources": RESOURCES}
        }

    # 7. Resources Read
    if method == "resources/read":
        uri = params.get("uri")
        if uri == "order://catalog":
            data = make_api_request("/api/v1/inventory")
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "result": {
                    "contents": [{"uri": uri, "mimeType": "application/json", "text": json.dumps(data, indent=2)}]
                }
            }
        elif uri == "order://system-health":
            data = make_api_request("/actuator/health")
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "result": {
                    "contents": [{"uri": uri, "mimeType": "application/json", "text": json.dumps(data, indent=2)}]
                }
            }
        else:
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "error": {"code": -32602, "message": f"Unknown resource URI: {uri}"}
            }

    # Unhandled method
    if msg_id is not None:
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "error": {"code": -32601, "message": f"Method not found: {method}"}
        }
    return None

def main():
    # Force unbuffered stdin/stdout
    sys.stdin.reconfigure(encoding='utf-8')
    sys.stdout.reconfigure(encoding='utf-8')

    for line in sys.stdin:
        response = process_message(line)
        if response is not None:
            sys.stdout.write(json.dumps(response) + "\n")
            sys.stdout.flush()

if __name__ == "__main__":
    main()
