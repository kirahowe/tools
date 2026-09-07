#!/usr/bin/env bash
# Runs every bug in bugs/ under several Clojure error-display modes and captures the output.
# Usage: ./run.sh [alias]   e.g. ./run.sh clj110  (default: the deps.edn default, Clojure 1.12.6)
set -u
export PATH=/tmp/claude-0/-home-user-tools/cf1a5c87-7190-585e-aa36-f0aa813f98cc/scratchpad/cljcli/bin:$PATH
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-}"
ALIAS="${1:-default}"
A=""; [ "$ALIAS" != "default" ] && A="-A:$ALIAS"
OUT="out/$ALIAS"; mkdir -p "$OUT"
strip() { grep -v '^Picked up JAVA_TOOL_OPTIONS' ; }
for f in bugs/*.clj; do
  b=$(basename "$f" .clj)
  # 1. script mode: `clojure -M bugs/x.clj` — an uncaught exception in a script
  clojure $A -M "$f" > "$OUT/$b.script.txt" 2>&1 < /dev/null || true
  strip < "$OUT/$b.script.txt" > "$OUT/$b.script.tmp" && mv "$OUT/$b.script.tmp" "$OUT/$b.script.txt"
  rep=$(grep -A1 'Full report at:' "$OUT/$b.script.txt" | tail -1 | tr -d ' ' || true)
  if [ -n "${rep:-}" ] && [ -f "$rep" ]; then cp "$rep" "$OUT/$b.report.edn"; rm -f "$rep"; fi
  # 2. REPL mode: what `clj` prints when you load the file at the REPL
  echo "(load-file \"$f\")" | clojure $A -M -r 2>&1 | strip > "$OUT/$b.repl.txt" || true
  # 3. full Java stack trace (what you see in logs / test runners / pre-1.10 clojure.main)
  clojure $A -M -e "(try (load-file \"$f\") (catch Throwable t (.printStackTrace t)))" 2>&1 < /dev/null | strip > "$OUT/$b.trace.txt" || true
  echo "done $b"
done
