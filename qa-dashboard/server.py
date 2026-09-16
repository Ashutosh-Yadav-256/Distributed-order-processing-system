"""
Senior QA Test Server & Mock Platform for Distributed Order Processing System
Simulates complete microservices ecosystem, Saga choreography, Redis cache, and RabbitMQ events.
"""

import http.server
import socketserver
import json
import uuid
import time
import os
import mimetypes
from datetime import datetime
from urllib.parse import urlparse, parse_qs

PORT = 4000
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

# -------------------------------------------------------------
# In-Memory State Emulating Databases, Cache & Event Bus
# -------------------------------------------------------------
STATE = {
    "products": {
        "11111111-1111-1111-1111-111111111111": {
            "productId": "11111111-1111-1111-1111-111111111111",
            "sku": "PROD-LAPTOP-001",
            "name": "UltraBook Pro 16",
            "availableQuantity": 50,
            "reservedQuantity": 0,
            "unitPrice": 1299.99
        },
        "22222222-2222-2222-2222-222222222222": {
            "productId": "22222222-2222-2222-2222-222222222222",
            "sku": "PROD-HEADPHONES-002",
            "name": "Noise-Cancelling Wireless Headphones",
            "availableQuantity": 100,
            "reservedQuantity": 0,
            "unitPrice": 199.99
        },
        "33333333-3333-3333-3333-333333333333": {
            "productId": "33333333-3333-3333-3333-333333333333",
            "sku": "PROD-KEYBOARD-003",
            "name": "Mechanical RGB Gaming Keyboard",
            "availableQuantity": 5,
            "reservedQuantity": 0,
            "unitPrice": 149.99
        },
        "44444444-4444-4444-4444-444444444444": {
            "productId": "44444444-4444-4444-4444-444444444444",
            "sku": "PROD-MONITOR-004",
            "name": "4K UltraWide Curved Monitor",
            "availableQuantity": 25,
            "reservedQuantity": 0,
            "unitPrice": 799.99
        }
    },
    "redis_cache": {},
    "orders": {},
    "payments": {},
    "notifications": [],
    "events": [],
    "processed_events": set(),
    "s3_invoices": []
}


def log_event(event_type, exchange, routing_key, payload):
    event_record = {
        "id": str(uuid.uuid4()),
        "type": event_type,
        "exchange": exchange,
        "routingKey": routing_key,
        "payload": payload,
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }
    STATE["events"].append(event_record)
    return event_record

class QATestHandler(http.server.SimpleHTTPRequestHandler):

    def send_json(self, status_code, data, headers=None):
        payload = json.dumps(data, indent=2).encode('utf-8')
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("X-Correlation-Id", str(uuid.uuid4()))
        if headers:
            for k, v in headers.items():
                self.send_header(k, v)
        self.end_headers()
        self.wfile.write(payload)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        # Health endpoints
        if path == "/actuator/health":
            self.send_json(200, {
                "status": "UP",
                "components": {
                    "apiGateway": {"status": "UP", "details": {"rateLimiter": "Redis Token-Bucket active"}},
                    "orderService": {"status": "UP", "details": {"database": "order_db", "saga": "Choreography"}},
                    "inventoryService": {"status": "UP", "details": {"database": "inventory_db", "cache": "Redis"}},
                    "paymentService": {"status": "UP", "details": {"database": "payment_db", "dlq": "payment.dlq"}},
                    "notificationService": {"status": "UP", "details": {"database": "notification_db"}},
                    "flociAws": {"status": "UP", "details": {"emulator": "Floci Local AWS (Port 4566)", "services": "S3, SQS, SNS, SecretsManager"}}
                }
            })
            return

        # Microservices state dump for dashboard
        if path == "/api/v1/qa/state":
            self.send_json(200, {
                "success": True,
                "data": {
                    "products": list(STATE["products"].values()),
                    "orders": list(STATE["orders"].values()),
                    "payments": list(STATE["payments"].values()),
                    "notifications": STATE["notifications"],
                    "events": STATE["events"],
                    "redisKeys": list(STATE["redis_cache"].keys()),
                    "s3Invoices": STATE["s3_invoices"]
                }
            })
            return

        # S3 Invoices endpoint
        if path == "/api/v1/aws/s3/invoices":
            self.send_json(200, {
                "success": True,
                "bucket": "ecommerce-order-invoices",
                "emulator": "Floci AWS S3 (Port 4566)",
                "data": STATE["s3_invoices"]
            })
            return


        # Inventory endpoints
        if path == "/api/v1/inventory":
            self.send_json(200, {"success": True, "data": list(STATE["products"].values())})
            return

        if path.startswith("/api/v1/inventory/"):
            prod_id = path.replace("/api/v1/inventory/", "")
            if prod_id in STATE["products"]:
                # Check Redis
                cache_hit = prod_id in STATE["redis_cache"]
                if not cache_hit:
                    STATE["redis_cache"][prod_id] = STATE["products"][prod_id]["availableQuantity"]
                self.send_json(200, {
                    "success": True,
                    "data": STATE["products"][prod_id],
                    "cacheSource": "REDIS" if cache_hit else "POSTGRESQL"
                })
            else:
                self.send_json(404, {"success": False, "message": "Product not found"})
            return

        # Order endpoints
        if path.startswith("/api/v1/orders/"):
            order_id = path.replace("/api/v1/orders/", "")
            if order_id in STATE["orders"]:
                self.send_json(200, {"success": True, "data": STATE["orders"][order_id]})
            else:
                self.send_json(404, {"success": False, "message": "Order not found"})
            return

        # Notifications endpoint
        if path.startswith("/api/v1/notifications/order/"):
            order_id = path.replace("/api/v1/notifications/order/", "")
            filtered = [n for n in STATE["notifications"] if n["orderId"] == order_id]
            self.send_json(200, {"success": True, "data": filtered})
            return

        # Payments endpoint
        if path.startswith("/api/v1/payments/order/"):
            order_id = path.replace("/api/v1/payments/order/", "")
            if order_id in STATE["payments"]:
                self.send_json(200, {"success": True, "data": STATE["payments"][order_id]})
            else:
                self.send_json(404, {"success": False, "message": "Payment not found"})
            return

        # Serve static files from qa-dashboard directory
        if path == "/" or path == "":
            path = "/index.html"

        file_path = os.path.join(DIRECTORY, path.lstrip("/"))
        if os.path.exists(file_path) and os.path.isfile(file_path):
            mime_type, _ = mimetypes.guess_type(file_path)
            self.send_response(200)
            self.send_header("Content-Type", mime_type or "application/octet-stream")
            with open(file_path, "rb") as f:
                content = f.read()
            self.send_header("Content-Length", str(len(content)))
            self.end_headers()
            self.wfile.write(content)
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        length = int(self.headers.get('Content-Length', 0))
        body = json.loads(self.rfile.read(length)) if length > 0 else {}

        # 1. JWT Token Generator
        if path == "/api/v1/auth/token":
            email = body.get("email", "qa_engineer@platform.com")
            roles = body.get("roles", ["ROLE_ADMIN", "ROLE_USER"])
            token = f"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.{uuid.uuid4().hex[:32]}.sig"
            self.send_json(200, {
                "success": True,
                "message": "Token generated successfully",
                "data": {
                    "token": token,
                    "tokenType": "Bearer",
                    "userId": str(uuid.uuid4()),
                    "email": email,
                    "roles": roles,
                    "expiresIn": 86400
                }
            })
            return

        # 2. Reset Test State
        if path == "/api/v1/qa/reset":
            STATE["orders"].clear()
            STATE["payments"].clear()
            STATE["notifications"].clear()
            STATE["events"].clear()
            STATE["redis_cache"].clear()
            STATE["processed_events"].clear()
            STATE["s3_invoices"].clear()
            # Reset stock

            STATE["products"]["11111111-1111-1111-1111-111111111111"]["availableQuantity"] = 50
            STATE["products"]["11111111-1111-1111-1111-111111111111"]["reservedQuantity"] = 0
            STATE["products"]["22222222-2222-2222-2222-222222222222"]["availableQuantity"] = 100
            STATE["products"]["22222222-2222-2222-2222-222222222222"]["reservedQuantity"] = 0
            STATE["products"]["33333333-3333-3333-3333-333333333333"]["availableQuantity"] = 5
            STATE["products"]["33333333-3333-3333-3333-333333333333"]["reservedQuantity"] = 0
            STATE["products"]["44444444-4444-4444-4444-444444444444"]["availableQuantity"] = 25
            STATE["products"]["44444444-4444-4444-4444-444444444444"]["reservedQuantity"] = 0
            self.send_json(200, {"success": True, "message": "Platform state reset cleanly"})
            return

        # 3. Create Order & Run Saga Simulation
        if path == "/api/v1/orders":
            order_id = str(uuid.uuid4())
            customer_id = body.get("customerId", str(uuid.uuid4()))
            customer_email = body.get("customerEmail", "customer@example.com")
            items = body.get("items", [])
            currency = body.get("currency", "USD")
            payment_method = body.get("paymentMethod", "CREDIT_CARD")

            total_amount = sum(float(item["unitPrice"]) * int(item["quantity"]) for item in items)

            order = {
                "id": order_id,
                "customerId": customer_id,
                "customerEmail": customer_email,
                "status": "PENDING",
                "totalAmount": total_amount,
                "currency": currency,
                "paymentMethod": payment_method,
                "items": items,
                "failureReason": None,
                "createdAt": datetime.utcnow().isoformat() + "Z",
                "updatedAt": datetime.utcnow().isoformat() + "Z"
            }
            STATE["orders"][order_id] = order

            # Event 1: OrderCreatedEvent published to RabbitMQ
            log_event("OrderCreatedEvent", "order.events.exchange", "order.created", {
                "orderId": order_id, "customerId": customer_id, "items": items, "total": total_amount
            })

            # Send initial order placed notification
            STATE["notifications"].append({
                "id": str(uuid.uuid4()),
                "orderId": order_id,
                "channel": "EMAIL",
                "recipient": customer_email,
                "subject": f"Order #{order_id[:8]} Received",
                "content": "Thank you for your order! We are reserving your items.",
                "status": "SENT",
                "sentAt": datetime.utcnow().isoformat() + "Z"
            })

            # Saga Step 2: Inventory Service Reservation
            stock_ok = True
            failed_prod_name = ""
            for item in items:
                p_id = item["productId"]
                qty = int(item["quantity"])
                if p_id in STATE["products"] and STATE["products"][p_id]["availableQuantity"] >= qty:
                    STATE["products"][p_id]["availableQuantity"] -= qty
                    STATE["products"][p_id]["reservedQuantity"] += qty
                    STATE["redis_cache"].pop(p_id, None) # Evict cache
                else:
                    stock_ok = False
                    failed_prod_name = item.get("productName", p_id)
                    break

            if not stock_ok:
                # Inventory Reservation FAILED
                order["status"] = "FAILED"
                order["failureReason"] = f"Insufficient stock for product: {failed_prod_name}"
                order["updatedAt"] = datetime.utcnow().isoformat() + "Z"
                log_event("InventoryReservationFailedEvent", "inventory.events.exchange", "inventory.reservation.failed", {
                    "orderId": order_id, "reason": order["failureReason"]
                })
                self.send_json(201, {"success": True, "data": order, "message": "Order failed due to inventory shortage"})
                return

            # Inventory Reservation SUCCEEDED
            order["status"] = "INVENTORY_RESERVED"
            log_event("InventoryReservedEvent", "inventory.events.exchange", "inventory.reserved", {
                "orderId": order_id, "items": items
            })

            # Saga Step 3: Payment Service Processing
            if "FAIL" in payment_method.upper() or total_amount == 999.99:
                # Payment FAILED
                payment = {
                    "id": str(uuid.uuid4()),
                    "orderId": order_id,
                    "transactionId": f"failed_{uuid.uuid4().hex[:12]}",
                    "amount": total_amount,
                    "status": "FAILED",
                    "errorMessage": "Card declined: Simulated failure token",
                    "createdAt": datetime.utcnow().isoformat() + "Z"
                }
                STATE["payments"][order_id] = payment

                log_event("PaymentFailedEvent", "payment.events.exchange", "payment.failed", {
                    "orderId": order_id, "amount": total_amount, "reason": payment["errorMessage"]
                })

                # Order transitions to CANCELLED
                order["status"] = "CANCELLED"
                order["failureReason"] = "Payment declined: Simulated failure token"
                order["updatedAt"] = datetime.utcnow().isoformat() + "Z"

                # Compensating Transaction: OrderCancelledEvent triggers Inventory release!
                log_event("OrderCancelledEvent", "order.events.exchange", "order.cancelled", {
                    "orderId": order_id, "compensationRequired": True
                })

                for item in items:
                    p_id = item["productId"]
                    qty = int(item["quantity"])
                    if p_id in STATE["products"]:
                        STATE["products"][p_id]["availableQuantity"] += qty
                        STATE["products"][p_id]["reservedQuantity"] -= qty
                        STATE["redis_cache"].pop(p_id, None)

                STATE["notifications"].append({
                    "id": str(uuid.uuid4()),
                    "orderId": order_id,
                    "channel": "EMAIL",
                    "recipient": customer_email,
                    "subject": f"Order #{order_id[:8]} Cancelled",
                    "content": "Your payment authorization failed. Any held inventory has been released.",
                    "status": "SENT",
                    "sentAt": datetime.utcnow().isoformat() + "Z"
                })

                self.send_json(201, {"success": True, "data": order, "message": "Payment failed. Compensating transaction restored stock."})
                return

            # Payment SUCCEEDED
            txn_id = f"txn_{uuid.uuid4().hex[:16]}"
            payment = {
                "id": str(uuid.uuid4()),
                "orderId": order_id,
                "transactionId": txn_id,
                "amount": total_amount,
                "status": "SUCCESS",
                "createdAt": datetime.utcnow().isoformat() + "Z"
            }
            STATE["payments"][order_id] = payment

            log_event("PaymentCompletedEvent", "payment.events.exchange", "payment.completed", {
                "orderId": order_id, "transactionId": txn_id, "amount": total_amount
            })

            # Saga Step 4: Confirm Order
            order["status"] = "CONFIRMED"
            order["updatedAt"] = datetime.utcnow().isoformat() + "Z"

            log_event("OrderConfirmedEvent", "order.events.exchange", "order.confirmed", {
                "orderId": order_id, "amount": total_amount
            })

            # Commit inventory reservations
            for item in items:
                p_id = item["productId"]
                qty = int(item["quantity"])
                if p_id in STATE["products"]:
                    STATE["products"][p_id]["reservedQuantity"] -= qty

            # Send confirmation receipt
            STATE["notifications"].append({
                "id": str(uuid.uuid4()),
                "orderId": order_id,
                "channel": "EMAIL",
                "recipient": customer_email,
                "subject": f"Receipt & Order Confirmation #{order_id[:8]}",
                "content": f"Payment of ${total_amount:.2f} confirmed (Txn: {txn_id}). Your order is being prepared for dispatch!",
                "status": "SENT",
                "sentAt": datetime.utcnow().isoformat() + "Z"
            })

            # Archive customer invoice to Floci AWS S3 emulator
            inv_id = f"INV-{uuid.uuid4().hex[:8].upper()}"
            STATE["s3_invoices"].append({
                "invoiceId": inv_id,
                "orderId": order_id,
                "bucket": "ecommerce-order-invoices",
                "key": f"invoices/order-{order_id[:8]}.json",
                "amount": total_amount,
                "currency": currency,
                "status": "STORED",
                "storageProvider": "Floci Local AWS S3 (Port 4566)",
                "archivedAt": datetime.utcnow().isoformat() + "Z"
            })

            self.send_json(201, {"success": True, "data": order, "message": "Order successfully confirmed via distributed Saga"})
            return


        # 4. Idempotency Test Trigger
        if path == "/api/v1/qa/test-idempotency":
            event_id = body.get("eventId", str(uuid.uuid4()))
            consumer = body.get("consumer", "PaymentEventConsumer")
            key = f"{event_id}:{consumer}"

            if key in STATE["processed_events"]:
                self.send_json(200, {
                    "success": True,
                    "duplicateDetected": True,
                    "action": "SKIPPED_DUPLICATE_ACKNOWLEDGED",
                    "message": f"Event {event_id} was ALREADY processed by {consumer}. Discarded without re-executing."
                })
            else:
                STATE["processed_events"].add(key)
                self.send_json(200, {
                    "success": True,
                    "duplicateDetected": False,
                    "action": "PROCESSED_AND_RECORDED",
                    "message": f"Event {event_id} processed for the first time by {consumer} and marked in processed_events table."
                })
            return

        self.send_json(404, {"error": "Not Found", "path": path})

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    server = socketserver.TCPServer(("", PORT), QATestHandler)
    print(f"QA Testing Server active at http://localhost:{PORT}")
    server.serve_forever()

