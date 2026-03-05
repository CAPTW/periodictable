#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)/antigravity"
DST_DIR="${HOME}/.gemini/antigravity"

mkdir -p "$DST_DIR"

cp -f "$SRC_DIR/denylist.txt" "$DST_DIR/denylist.txt"
cp -f "$SRC_DIR/browserAllowlist.txt" "$DST_DIR/browserAllowlist.txt"

echo "✅ Antigravity denylist/allowlist installed to:"
echo "   $DST_DIR"
echo ""
echo "Files:"
echo " - $DST_DIR/denylist.txt"
echo " - $DST_DIR/browserAllowlist.txt"
