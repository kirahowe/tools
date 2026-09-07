#!/usr/bin/env python3
"""Stacked horizontal bars: who owns the frames of each script-mode stack trace (from out/default/frame-stats.md)."""
import re, sys
rows = []
for line in open("out/default/frame-stats.md"):
    m = re.match(r"\| (\d\d_\S+) \| (\d+) \| \d+ \| \S+ \| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|", line)
    if m:
        bug, total, user, core, lang, main, spec, jdk = m.group(1), *map(int, m.groups()[1:])
        rows.append((bug, total, user, core, lang, main, spec + jdk))
rows.sort(key=lambda r: (-r[2] / r[1], r[0]))
series = [("user", "your code", "#2a78d6"), ("core", "clojure.core", "#eb6834"), ("lang", "clojure.lang", "#1baf7a"),
          ("main", "clojure.main (launcher)", "#eda100"), ("other", "spec / JDK", "#e87ba4")]
W, left, right, top, rh, gap = 760, 150, 92, 46, 22, 6
maxf = max(r[1] for r in rows)
plot_w = W - left - right
H = top + len(rows) * (rh + gap) + 34
out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" role="img" aria-label="For each of 15 bugs, how many stack frames belong to the user\'s code versus Clojure internals: on average one frame in eight is the user\'s." font-family="JetBrains Mono, ui-monospace, Menlo, monospace" font-size="12">']
x = 0
for key, label, col in series:
    out.append(f'<rect class="s-{key}" x="{x}" y="12" width="12" height="12" rx="2" fill="{col}"/>')
    out.append(f'<text class="ink" x="{x+17}" y="22" fill="#1b2430">{label}</text>')
    x += 17 + 7.2 * len(label) + 18
for i, (bug, total, user, core, lang, main, other) in enumerate(rows):
    y = top + i * (rh + gap)
    out.append(f'<text class="ink" x="{left-8}" y="{y+15}" text-anchor="end" fill="#1b2430">{bug}</text>')
    x = left
    for key, val in zip(["user","core","lang","main","other"], [user, core, lang, main, other]):
        if val == 0: continue
        w = val / maxf * plot_w
        col = dict((k, c) for k, _, c in series)[key]
        out.append(f'<rect class="s-{key}" x="{x:.1f}" y="{y}" width="{max(w-2,1):.1f}" height="{rh}" rx="2" fill="{col}"/>')
        x += w
    pct = round(100 * user / total)
    out.append(f'<text class="ink-muted" x="{x+6:.1f}" y="{y+15}" fill="#5b6673">{user}/{total} · {pct}%</text>')
# axis
ay = top + len(rows) * (rh + gap) + 4
out.append(f'<line x1="{left}" y1="{ay}" x2="{left+plot_w}" y2="{ay}" stroke="#5b6673" stroke-width="1" class="axis"/>')
for t in range(0, maxf + 1, 5):
    tx = left + t / maxf * plot_w
    out.append(f'<line x1="{tx:.1f}" y1="{ay}" x2="{tx:.1f}" y2="{ay+4}" stroke="#5b6673" class="axis"/>')
    out.append(f'<text class="ink-muted" x="{tx:.1f}" y="{ay+18}" text-anchor="middle" fill="#5b6673">{t}</text>')
out.append("</svg>")
open("../figures/frame-composition.svg", "w").write("\n".join(out))
print("rows:", [(r[0], r[2], r[1]) for r in rows])
