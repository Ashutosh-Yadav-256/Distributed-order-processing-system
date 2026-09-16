#!/usr/bin/env bash
# Start the Distributed Order Processing Platform MCP Server
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/mcp-server/server.py"
