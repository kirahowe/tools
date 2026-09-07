#!/usr/bin/env bash
# Runs every bug with the proof-of-concept printer installed (REPL mode, and script mode via the uncaught handler).
set -u
export PATH=/tmp/claude-0/-home-user-tools/cf1a5c87-7190-585e-aa36-f0aa813f98cc/scratchpad/cljcli/bin:$PATH
cd "$(dirname "$0")"
OUT=out/prototype; mkdir -p $OUT
DEPS='{:deps {nice-errors/nice-errors {:local/root "../prototype"} expound/expound {:mvn/version "0.9.0"}}}'
strip() { grep -v '^Picked up JAVA_TOOL_OPTIONS' | grep -v '^Downloading'; }
for f in bugs/*.clj; do b=$(basename "$f" .clj)
  # dev/user.clj (on the :proto alias classpath) calls install! before the REPL starts
  echo "(load-file \"$f\")" | clojure -A:proto -M -r 2>&1 | strip | grep -v '^user=> $' > "$OUT/$b.repl.txt"
  echo "done $b"
done
