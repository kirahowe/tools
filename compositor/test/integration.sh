#!/usr/bin/env bash
# End-to-end check of the jj/tmux command SEQUENCES the babashka code issues —
# the operations from session.clj / graph.clj, run as raw CLI. Passing this
# means the tool's substrate usage is correct even though bb itself can't be
# executed in every environment. Needs `jj` and `tmux` on PATH.
#
# Mirrors the M1 success criterion (spec §11): two sessions edit different
# files; both appear in the composite; toggling one off makes it disappear.
set -uo pipefail
export JJ_CONFIG=/dev/null JJ_USER=t JJ_EMAIL=t@example.com
SB=$(mktemp -d "${TMPDIR:-/tmp}/comp-integration.XXXXXX")
trap 'rm -rf "$SB"' EXIT
cd "$SB"

jj() { command jj --no-pager --color never \
         --config 'revset-aliases."immutable_heads()"="present(main)"' "$@"; }
newid() { grep -oE 'Created new commit [a-z]+' | awk '{print $4}'; }
wsat()  { ( cd "$1" && command jj --no-pager log -r @ --no-graph -T 'change_id.short()' ); }
FAIL=0
ck() { if [ "$2" = "$3" ]; then echo "  PASS: $1"; else echo "  FAIL: $1 (got '$2' want '$3')"; FAIL=1; fi; }

echo "== comp init: colocate, megamerge on trunk, dev workspace = scratch"
mkdir repo && cd repo && jj git init --colocate >/dev/null 2>&1
mkdir src
echo 'export const header = "v1";'  > src/header.js
echo 'export const footer = "v1";'  > src/footer.js
echo 'node_modules/' > .gitignore
jj describe -m "trunk: initial" >/dev/null 2>&1
jj bookmark create main -r @ >/dev/null 2>&1
jj new >/dev/null 2>&1
# graph/init!: megamerge parented on trunk only, dev workspace off it
MM=$(jj new --no-edit -m "compositor: composite" main 2>&1 | newid)
jj workspace add --name dev ../ws-dev --revision "$MM" >/dev/null 2>&1
ck "megamerge exists" "$(jj log -r "$MM" --no-graph -T 'if(empty,"e","x")')" "e"

echo "== comp new x2: sessions are workspace @ off trunk (review F1)"
# session.clj create!: workspace-add off trunk, describe @, edit, snapshot
jj workspace add --name s1 ../ws-s1 --revision main >/dev/null 2>&1
( cd ../ws-s1 && jj describe -m "s1: header" >/dev/null 2>&1 )
S1=$(wsat ../ws-s1)
jj workspace add --name s2 ../ws-s2 --revision main >/dev/null 2>&1
( cd ../ws-s2 && jj describe -m "s2: footer" >/dev/null 2>&1 )
S2=$(wsat ../ws-s2)
ck "s1 change id captured" "$([ -n "$S1" ] && echo ok)" "ok"

echo "== agents edit different files; daemon snapshots (jj status)"
( cd ../ws-s1 && sed -i.bak 's/header = "v1"/header = "v2"/' src/header.js && rm -f src/*.bak && jj status >/dev/null 2>&1 )
( cd ../ws-s2 && sed -i.bak 's/footer = "v1"/footer = "v2"/' src/footer.js && rm -f src/*.bak && jj status >/dev/null 2>&1 )

echo "== graph/rebuild!: rebase -s MM -d main -d s1 -d s2 (in place, id-stable)"
jj rebase -s "$MM" -d main -d "$S1" -d "$S2" >/dev/null 2>&1
ck "megamerge id stable after rebuild" "$(jj log -r "$MM" --no-graph -T 'change_id.short(8)')" "$MM"
ck "composite clean" "$(jj log -r "$MM" --no-graph -T 'if(conflict,"c","clean")')" "clean"

echo "== graph/materialize!: update-stale dev; BOTH edits present (M1 criterion)"
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
ck "s1 edit in composite" "$( cd ../ws-dev && grep -c 'header = "v2"' src/header.js )" "1"
ck "s2 edit in composite" "$( cd ../ws-dev && grep -c 'footer = "v2"' src/footer.js )" "1"

echo "== comp toggle 2 off: rebuild with s2 unapplied; edit disappears"
jj rebase -s "$MM" -d main -d "$S1" >/dev/null 2>&1
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
ck "s2 edit gone" "$( cd ../ws-dev && grep -c 'footer = "v2"' src/footer.js )" "0"
ck "s1 edit remains" "$( cd ../ws-dev && grep -c 'header = "v2"' src/header.js )" "1"

echo "== comp keep 1: bookmark move + rebuild; composite byte-identical (§1)"
# re-apply s2 first so keep has a survivor to rebase
jj rebase -s "$MM" -d main -d "$S1" -d "$S2" >/dev/null 2>&1
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
BEFORE=$( cd ../ws-dev && cat src/header.js src/footer.js | sha256sum | cut -c1-16 )
# session/keep!: gate, bookmark move, rebase survivors, rebuild, materialize
ck "kept take is conflict-free (gate passes)" "$(jj log -r "$S1" --no-graph -T 'if(conflict,"c","ok")')" "ok"
jj bookmark move main --to "$S1" --allow-backwards >/dev/null 2>&1
jj rebase -s "$S2" -d main >/dev/null 2>&1
jj rebase -s "$MM" -d main -d "$S2" >/dev/null 2>&1
( cd ../ws-dev && jj workspace update-stale >/dev/null 2>&1 )
AFTER=$( cd ../ws-dev && cat src/header.js src/footer.js | sha256sum | cut -c1-16 )
ck "keep is a visual no-op" "$AFTER" "$BEFORE"

echo "== conflict is data: two applied sessions change the same line; MM survives"
# s2 changed footer v1->v2; s3 (off trunk, footer still v1) changes it v1->v9.
# Both applied => the composite conflicts on footer, but the commit still exists.
jj workspace add --name s3 ../ws-s3 --revision main >/dev/null 2>&1
( cd ../ws-s3 && jj describe -m "s3: footer differently" >/dev/null 2>&1 \
  && sed -i.bak 's/footer = "v1"/footer = "v9"/' src/footer.js && rm -f src/*.bak && jj status >/dev/null 2>&1 )
S3=$(wsat ../ws-s3)
jj rebase -s "$MM" -d main -d "$S2" -d "$S3" >/dev/null 2>&1
ck "conflicted megamerge still exists" "$(jj log -r "$MM" --no-graph -T 'if(conflict,"c","clean")')" "c"
ck "collision names the file" "$(jj resolve --list -r "$MM" 2>/dev/null | grep -c footer)" "1"

echo
[ "$FAIL" = 0 ] && echo "INTEGRATION: ALL PASS (jj $(command jj --version | awk '{print $2}'))" \
                || { echo "INTEGRATION: FAILURES"; exit 1; }
