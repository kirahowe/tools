#!/usr/bin/env python3
"""Classify the frames of each captured Java stack trace (out/<alias>/*.trace.txt)."""
import re, sys, glob, os, json
alias = sys.argv[1] if len(sys.argv) > 1 else "default"
def classify(frame):
    if frame.startswith("bugs."): return "user"
    if frame.startswith("clojure.core$") or frame.startswith("clojure.core."): return "clojure.core"
    if frame.startswith("clojure.main$") or frame.startswith("clojure.main."): return "clojure.main"
    if frame.startswith("clojure.spec") : return "clojure.spec"
    if frame.startswith("clojure.lang."): return "clojure.lang"
    if re.match(r"(java|jdk|sun)\.", frame): return "jdk"
    return "other"
rows = []
for path in sorted(glob.glob(f"out/{alias}/*.trace.txt")):
    bug = os.path.basename(path).replace(".trace.txt", "")
    text = open(path, errors="replace").read()
    frames = re.findall(r"^\s+at (\S+)\(", text, re.M)
    causes = len(re.findall(r"^Caused by:", text, re.M))
    cats = [classify(f) for f in frames]
    first_user = next((i for i, c in enumerate(cats) if c == "user"), None)
    counts = {k: cats.count(k) for k in ["user","clojure.core","clojure.lang","clojure.main","clojure.spec","jdk","other"]}
    rows.append(dict(bug=bug, lines=text.count("\n"), frames=len(frames), causes=causes+1,
                     first_user_frame=(first_user+1 if first_user is not None else None), **counts))
print(json.dumps(rows, indent=1))
print()
print("| bug | output lines | frames | exceptions in chain | 1st user frame at | user | clojure.core | clojure.lang | clojure.main | spec | jdk |")
print("|---|---|---|---|---|---|---|---|---|---|---|")
for r in rows:
    print(f"| {r['bug']} | {r['lines']} | {r['frames']} | {r['causes']} | {r['first_user_frame']} | {r['user']} | {r['clojure.core']} | {r['clojure.lang']} | {r['clojure.main']} | {r['clojure.spec']} | {r['jdk']} |")
tot = sum(r['frames'] for r in rows); usr = sum(r['user'] for r in rows)
print(f"\nTOTAL frames={tot} user={usr} ({100*usr/tot:.1f}%)")
