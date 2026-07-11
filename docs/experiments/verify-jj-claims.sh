#!/usr/bin/env bash
# Verifies every load-bearing jj claim behind the Compositor design.
# Verified against jj 0.43.0. Run anywhere; uses a throwaway sandbox in $TMPDIR.
#
# Findings cross-referenced in docs/REVIEW.md:
#   E1  -> F1   workspace add --revision puts @ on a CHILD of the revision
#   E2  -> §4   octopus megamerge composites isolated session edits
#   E3  -> F2   rebase -s re-parents the megamerge in place (rebase -r would not)
#   E4  -> §2.4 conflicts are data; rebases succeed; template probe works
#   E5  -> F7   conflict markers materialize in the colliding session's workspace
#   E6  -> §4   absorb routes a scratch hunk through the merge into the owning session
#   E7  -> F3   without config, absorb rewrites TRUNK; immutable_heads() blocks it
#   E8  -> §1   keep is a byte-identical visual no-op (hash compared)
#   E9  -> §6   op restore rewinds keep/rebase/bookmark in one command
#   E10 -> F10  undoing `workspace forget` resurrects the record w/o the directory
#   E11 -> F11  snapshot refuses >1MiB new files (warning, not error)
set -uo pipefail

SB=$(mktemp -d "${TMPDIR:-/tmp}/compositor-verify.XXXXXX")
trap 'rm -rf "$SB"' EXIT
cd "$SB"

jj()   { command jj --no-pager --color never "$@"; }
jjimm(){ command jj --no-pager --color never \
           --config 'revset-aliases."immutable_heads()"="present(main)"' "$@"; }
wsat() { ( cd "$1" && jj log -r @ --no-graph -T 'change_id.short()' ); }
newid(){ grep -oE 'Created new commit [a-z]+' | awk '{print $4}'; }
say()  { printf '\n== %s\n' "$*"; }
check(){ [ "$2" = "$3" ] && echo "   PASS: $1" || { echo "   FAIL: $1 (got: $2, want: $3)"; FAILED=1; }; }
FAILED=0

export JJ_CONFIG=/dev/null JJ_USER=verify JJ_EMAIL=verify@example.com

mkdir repo && cd repo && jj git init >/dev/null 2>&1
mkdir src
echo 'export const header = "header v1";'   > src/header.js
echo 'export const footer = "footer v1";'   > src/footer.js
echo 'export const sidebar = "sidebar v1";' > src/sidebar.js
jj describe -m "trunk: initial" >/dev/null 2>&1
jj bookmark create main -r @ >/dev/null 2>&1
jj new >/dev/null 2>&1     # park default workspace @ off trunk

say "E1: workspace add --revision X puts @ on a child of X (spec F1)"
PRE=$(jj new main -m probe --no-edit 2>&1 | newid)
jj workspace add --name probe ../ws-probe --revision "$PRE" >/dev/null 2>&1
AT=$(wsat ../ws-probe)
check "workspace @ != captured change id" "$([ "${AT:0:8}" = "$PRE" ] && echo same || echo child)" "child"
jj workspace forget probe >/dev/null 2>&1; rm -rf ../ws-probe; jj abandon "$PRE" >/dev/null 2>&1

say "E2: sessions as workspace @; octopus megamerge composites both edits"
jj workspace add --name s1 ../ws-s1 -r main >/dev/null 2>&1
( cd ../ws-s1 && jj describe -m "s1: header" >/dev/null 2>&1 )
S1=$(wsat ../ws-s1)
jj workspace add --name s2 ../ws-s2 -r main >/dev/null 2>&1
( cd ../ws-s2 && jj describe -m "s2: footer" >/dev/null 2>&1 )
S2=$(wsat ../ws-s2)
( cd ../ws-s1 && sed -i.bak 's/header v1/header v2/' src/header.js && rm -f src/*.bak && jj st >/dev/null 2>&1 )
( cd ../ws-s2 && sed -i.bak 's/footer v1/footer v2/' src/footer.js && rm -f src/*.bak && jj st >/dev/null 2>&1 )
MM=$(jj new main "$S1" "$S2" -m MEGAMERGE --no-edit 2>&1 | newid)
jj workspace add --name dev ../ws-dev -r "$MM" >/dev/null 2>&1   # dev @ child = scratch
COMPOSITE=$( cd ../ws-dev && cat src/header.js src/footer.js )
check "both session edits in composite" \
  "$(echo "$COMPOSITE" | grep -c 'v2')" "2"

say "E3: unapply/re-apply via 'jj rebase -s MM' — identity stable, content flips"
jj rebase -s "$MM" -d main -d "$S1" >/dev/null 2>&1
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
check "s2 unapplied -> footer v1" "$( cd ../ws-dev && grep -c 'footer v1' src/footer.js )" "1"
check "megamerge change id stable" "$(jj log -r "$MM" --no-graph -T 'change_id.short(8)')" "$MM"
jj rebase -s "$MM" -d main -d "$S1" -d "$S2" >/dev/null 2>&1
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
check "s2 re-applied -> footer v2" "$( cd ../ws-dev && grep -c 'footer v2' src/footer.js )" "1"

say "E4: conflicts are data — colliding session; megamerge survives, probe works"
jj workspace add --name s3 ../ws-s3 -r main >/dev/null 2>&1
( cd ../ws-s3 && jj describe -m "s3: header too" >/dev/null 2>&1 \
  && sed -i.bak 's/header v1/header v9/' src/header.js && rm -f src/*.bak && jj st >/dev/null 2>&1 )
S3=$(wsat ../ws-s3)
jj rebase -s "$MM" -d main -d "$S1" -d "$S2" -d "$S3" >/dev/null 2>&1
check "megamerge conflicted but exists" \
  "$(jj log -r "$MM" --no-graph -T 'if(conflict, "1", "0")')" "1"
check "resolve --list names the file" \
  "$(jj resolve --list -r "$MM" 2>/dev/null | grep -c header)" "1"

say "E5: stacking s3 on s1 materializes markers in s3's workspace (agent sees them)"
jj rebase -s "$S3" -d "$S1" >/dev/null 2>&1
( cd ../ws-s3 && jj workspace update-stale >/dev/null 2>&1 )
check "markers in agent workspace" \
  "$( cd ../ws-s3 && grep -c '<<<<<<<' src/header.js )" "1"
# agent resolves by writing clean content; snapshot clears the conflict graph-wide
( cd ../ws-s3 && echo 'export const header = "header v2+v9";' > src/header.js && jj st >/dev/null 2>&1 )
check "resolution clears megamerge conflict" \
  "$(jj log -r "$MM" --no-graph -T 'if(conflict, "1", "0")')" "0"

say "E6: absorb — polish in dev workspace lands in owning session (through the merge)"
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 \
  && sed -i.bak 's/footer v2/footer v2 POLISHED/' src/footer.js && rm -f src/*.bak \
  && jjimm absorb >/dev/null 2>&1 )
check "s2 commit gained the hunk" \
  "$(jj diff -r "$S2" 2>/dev/null | grep -c POLISHED)" "1"

say "E7: immutability — absorb of trunk-owned lines must NOT rewrite trunk"
( cd ../ws-dev && jjimm workspace update-stale >/dev/null 2>&1 \
  && sed -i.bak 's/sidebar v1/sidebar HACKED/' src/sidebar.js && rm -f src/*.bak )
OUT=$( cd ../ws-dev && jjimm absorb 2>&1 )
check "absorb refused (Nothing changed)" "$(echo "$OUT" | grep -c 'Nothing changed')" "1"
jjimm describe -r main -m HACK >/dev/null 2>&1   # must be refused
check "trunk not rewritable directly" \
  "$(jj log -r main --no-graph -T 'description.first_line()')" "trunk: initial"
( cd ../ws-dev && sed -i.bak 's/sidebar HACKED/sidebar v1/' src/sidebar.js && rm -f src/*.bak && jjimm st >/dev/null 2>&1 )
# NOTE: without the immutable_heads() config, the same absorb targets the
# trunk commit itself and moves the bookmark ("Absorbed changes into: main*").
# Verified separately; deliberately not reproduced here to keep the sandbox sane.

say "E8: keep s1 — byte-identical composite (visual no-op)"
BEFORE=$( cd ../ws-dev && cat src/*.js | sha256sum | cut -c1-16 )
jjimm bookmark move main --to "$S1" >/dev/null 2>&1
jjimm rebase -s "$S2" -d main >/dev/null 2>&1
jjimm rebase -s "$S3" -d "$S1" >/dev/null 2>&1   # stacked: stays on its base
jjimm rebase -s "$MM" -d main -d "$S2" -d "$S3" >/dev/null 2>&1
( cd ../ws-dev && jjimm workspace update-stale >/dev/null 2>&1 )
AFTER=$( cd ../ws-dev && cat src/*.js | sha256sum | cut -c1-16 )
check "composite hash unchanged" "$AFTER" "$BEFORE"

say "E9: op restore rewinds the keep"
# op restore restores the state AS OF the named op's completion, so to rewind
# the keep we restore to the op immediately BEFORE the bookmark move (op log
# is newest-first: the line after the match is its predecessor).
OP=$(jj op log --no-graph -T 'id.short() ++ " " ++ description.first_line() ++ "\n"' \
     | grep -A1 'point bookmark' | tail -1 | awk '{print $1}')
jjimm op restore "$OP" >/dev/null 2>&1
check "main is back on trunk" \
  "$(jj log -r main --no-graph -T 'description.first_line()' | grep -c 'trunk: initial')" "1"

say "E10: undoing 'workspace forget' resurrects the record without the directory"
jjimm workspace forget s3 >/dev/null 2>&1
rm -rf ../ws-s3
jjimm undo >/dev/null 2>&1
check "record resurrected, dir gone" \
  "$(jj workspace list 2>/dev/null | grep -c '^s3:'),$([ -d ../ws-s3 ] && echo dir || echo nodir)" "1,nodir"

say "E11: snapshot refuses >1MiB new files (warning)"
( cd ../ws-dev && jjimm workspace update-stale >/dev/null 2>&1; mkdir -p dist && head -c 2000000 /dev/urandom > dist/big.bin )
check "size warning emitted" \
  "$( cd ../ws-dev && jjimm st 2>&1 | grep -c 'maximum size' )" "1"

echo
[ "$FAILED" = 0 ] && echo "ALL CHECKS PASSED (jj $(command jj --version | awk '{print $2}'))" \
                  || { echo "SOME CHECKS FAILED"; exit 1; }
