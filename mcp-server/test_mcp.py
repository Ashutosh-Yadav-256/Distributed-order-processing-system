#!/usr/bin/env python3

import subprocess
import json
import sys
import os

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

SERVER_PATH = os.path.join(SCRIPT_DIR, "server.py")

def send_recv(proc, request):
    line = json.dumps(request) + "\n"
    proc.stdin.write(line)
    proc.stdin.flush()
    res_line = proc.stdout.readline()
    if not res_line:
        raise RuntimeError("MCP server terminated unexpectedly or returned empty response.")
    return json.loads(res_line.strip())

def run_tests():
    print("========================================================================")
    print("Starting Automated MCP Server Test Suite (stdio JSON-RPC 2.0)")
    print("========================================================================")

    proc = subprocess.Popen(
        [sys.executable, SERVER_PATH],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding='utf-8'
    )

    try:
        print("\n[Test 1] Sending 'initialize' request...")
        req1 = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {"clientInfo": {"name": "test-client", "version": "1.0"}}
        }
        res1 = send_recv(proc, req1)
        assert res1.get("id") == 1, f"Expected id 1, got {res1.get('id')}"
        assert "serverInfo" in res1["result"], "Missing serverInfo"
        print(f"[OK] Initialize passed: {res1['result']['serverInfo']['name']} v{res1['result']['serverInfo']['version']}")

        print("\n[Test 2] Sending 'tools/list' request...")
        req2 = {"jsonrpc": "2.0", "id": 2, "method": "tools/list"}
        res2 = send_recv(proc, req2)
        tools = res2["result"]["tools"]
        tool_names = [t["name"] for t in tools]
        print(f"[PASS] Discovered {len(tools)} tools: {', '.join(tool_names)}")
        assert "get_system_health" in tool_names
        assert "create_order" in tool_names
        assert "check_inventory" in tool_names
        assert "get_s3_invoices" in tool_names
        assert "run_saga_scenario" in tool_names

        print("\n[Test 3] Calling tool 'get_system_health'...")
        req3 = {
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {"name": "get_system_health", "arguments": {}}
        }
        res3 = send_recv(proc, req3)
        assert "content" in res3["result"]
        health_text = res3["result"]["content"][0]["text"]
        health_json = json.loads(health_text)
        print(f"[PASS] Health check returned status: {health_json.get('status')}")
        assert health_json.get("status") == "UP"

        print("\n[Test 4] Calling tool 'check_inventory'...")
        req4 = {
            "jsonrpc": "2.0",
            "id": 4,
            "method": "tools/call",
            "params": {"name": "check_inventory", "arguments": {}}
        }
        res4 = send_recv(proc, req4)
        inv_data = json.loads(res4["result"]["content"][0]["text"])
        products = inv_data.get("data", [])
        print(f"[PASS] Inventory check returned {len(products)} products in catalog.")
        assert len(products) >= 4

        print("\n[Test 5] Calling tool 'run_saga_scenario' (happy_path)...")
        req5 = {
            "jsonrpc": "2.0",
            "id": 5,
            "method": "tools/call",
            "params": {"name": "run_saga_scenario", "arguments": {"scenario": "happy_path"}}
        }
        res5 = send_recv(proc, req5)
        scenario_text = res5["result"]["content"][0]["text"]
        print("[PASS] Happy path Saga executed via MCP tool:")
        print(scenario_text[:200] + "...")
        assert "CONFIRMED" in scenario_text

        print("\n[Test 6] Testing 'resources/list' and 'resources/read'...")
        req6 = {"jsonrpc": "2.0", "id": 6, "method": "resources/list"}
        res6 = send_recv(proc, req6)
        assert len(res6["result"]["resources"]) >= 2
        print(f"[PASS] Resources list returned {len(res6['result']['resources'])} resources.")

        req7 = {"jsonrpc": "2.0", "id": 7, "method": "resources/read", "params": {"uri": "order://catalog"}}
        res7 = send_recv(proc, req7)
        assert "contents" in res7["result"]
        print(f"[PASS] Resource 'order://catalog' read successfully.")

        print("\n========================================================================")
        print("[PASS] ALL 6 MCP SERVER TESTS PASSED (100% SUCCESS)!")
        print("========================================================================")


    finally:
        proc.stdin.close()
        proc.terminate()
        proc.wait(timeout=3)

if __name__ == "__main__":
    run_tests()
