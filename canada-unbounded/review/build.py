"""Build one self-contained, commentable review page per post from the rendered Quarto site.

Usage: python3 -I build.py <site_dir> <out_dir> [urls.json]
urls.json maps post basenames (e.g. "c03_labour_hours") to published artifact URLs,
plus "index" for the contents page; links fall back to "#" when unknown.
"""
import base64, json, os, re, sys
from bs4 import BeautifulSoup

site, out = sys.argv[1], sys.argv[2]
urls = json.load(open(sys.argv[3])) if len(sys.argv) > 3 and os.path.exists(sys.argv[3]) else {}
post_dir = os.path.join(site, "economics/wealth_distribution/canada")
posts = sorted(f[:-5] for f in os.listdir(post_dir) if re.match(r"c\d\d_.*\.html$", f))
os.makedirs(out, exist_ok=True)

CSS = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "page.css")).read()
JS = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "page.js")).read()


def title_of(soup):
    t = soup.select_one("h1.title").get_text(strip=True)
    return re.sub(r"^Unbounded North:\s*", "", t)


titles = {}
for p in posts:
    s = BeautifulSoup(open(os.path.join(post_dir, p + ".html")).read(), "html.parser")
    titles[p] = title_of(s)


def link_for(name):
    return urls.get(name, "#")


for i, p in enumerate(posts):
    soup = BeautifulSoup(open(os.path.join(post_dir, p + ".html")).read(), "html.parser")
    main = soup.find("main")
    part = int(p[1:3])
    title = titles[p]
    header = main.find("header", id="title-block-header")
    abstract = header.select_one(".abstract")
    abstract_text = abstract.get_text(" ", strip=True).replace("Abstract", "", 1).strip() if abstract else ""
    header.decompose()

    # Scripts Clay adds for interactive output we don't use.
    for s in main.find_all("script", src=True):
        if "jquery" in s["src"]:
            s.decompose()
    # Clay's source footer and the grey rule above it.
    for pre in main.find_all("pre"):
        if pre.get_text().strip().startswith("source:"):
            box = pre.parent
            prev = box.find_previous_sibling("div")
            if prev is not None and "background-color:grey" in (prev.get("style") or ""):
                prev.decompose()
            box.decompose()

    # Footnotes: pull every margin note into an ordered list at the end.
    notes = {}
    for box in main.select("div.column-margin"):
        for fn in box.find_all("div", id=re.compile(r"^fn\d+$"), recursive=False):
            n = int(fn["id"][2:])
            body = fn.find("p") or fn
            sup = body.find("sup")
            if sup is not None:
                nxt = sup.next_sibling
                sup.decompose()
                if isinstance(nxt, str):
                    nxt.replace_with(nxt.lstrip("  "))
            notes[n] = body.decode_contents()
            fn.decompose()
        if not box.find(True):
            box.decompose()
    for a in main.select("a.footnote-ref"):
        n = re.sub(r"\D", "", a.get("href", ""))
        a["href"] = "#note-" + n
        a["id"] = "ref-" + n
        for attr in ("role", "class"):
            a.attrs.pop(attr, None)
        a["class"] = "fnref"

    # Charts: inline each SVG file as a data URI so the page is one file.
    for img in main.find_all("img"):
        src = img.get("src", "")
        path = os.path.normpath(os.path.join(post_dir, src))
        if src.endswith(".svg") and os.path.exists(path):
            img["src"] = "data:image/svg+xml;base64," + base64.b64encode(open(path, "rb").read()).decode()
            img["alt"] = img.get("alt") or "Chart"

    # Links between posts go to the matching review page.
    for a in main.find_all("a", href=True):
        m = re.match(r"^(c\d\d_[a-z_]+)\.html(#.*)?$", a["href"])
        if m:
            a["href"] = link_for(m.group(1))
            a["target"] = "_top"
        elif a["href"].startswith("http"):
            a["target"] = "_blank"
            a["rel"] = "noopener"

    # Tables scroll sideways inside their own box on a phone.
    for t in main.find_all("table"):
        wrap = soup.new_tag("div", attrs={"class": "table-scroll"})
        t.wrap(wrap)

    # Strip Quarto layout classes that assume its grid.
    for el in main.find_all(class_=True):
        el["class"] = [c for c in el["class"] if not c.startswith(("page-columns", "page-full", "column-"))]

    prev_p = posts[i - 1] if i > 0 else None
    next_p = posts[i + 1] if i + 1 < len(posts) else None
    nav = ['<nav class="pager" data-uncommentable aria-label="Parts">']
    nav.append(f'<a href="{link_for("index")}" target="_top">All parts</a>')
    nav.append(f'<span class="pos">Part {part} of {len(posts) - 1}</span>' if part else '<span class="pos">Introduction</span>')
    nav.append(f'<a href="{link_for(next_p)}" target="_top">Next →</a>' if next_p else '<span></span>')
    nav.append("</nav>")
    nav_html = "".join(nav)

    foot = ['<nav class="next" data-uncommentable aria-label="Next part">']
    if prev_p:
        foot.append(f'<a class="prev" href="{link_for(prev_p)}" target="_top"><span>Previous</span>{titles[prev_p]}</a>')
    if next_p:
        foot.append(f'<a class="nxt" href="{link_for(next_p)}" target="_top"><span>Next</span>{titles[next_p]}</a>')
    foot.append("</nav>")

    notes_html = ""
    if notes:
        items = "".join(
            f'<li id="note-{n}" value="{n}">{notes[n]} <a class="back" href="#ref-{n}" aria-label="Back to text">↩</a></li>'
            for n in sorted(notes))
        notes_html = f'<section class="notes"><h2>Notes</h2><ol>{items}</ol></section>'

    eyebrow = f"Unbounded North · Part {part}" if part else "Unbounded North · Introduction"
    html = f"""<title>{title}</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,500;0,6..72,600;1,6..72,400&family=Public+Sans:ital,wght@0,400;0,600;1,400&display=swap">
<style>{CSS}</style>
<div class="wrap">
{nav_html}
<header class="post-head">
  <p class="eyebrow">{eyebrow} · Review draft</p>
  <h1>{title}</h1>
  {f'<p class="abstract">{abstract_text}</p>' if abstract_text else ''}
  <p class="hint" data-uncommentable>To comment, tap <b>Comment</b> and then any paragraph, chart or table. To comment on a phrase, select it first.</p>
</header>
<article class="post">
{main.decode_contents()}
</article>
{notes_html}
{"".join(foot)}
</div>
<button class="comment-fab" type="button" data-uncommentable hidden aria-pressed="false">Comment</button>
<p class="comment-tip" data-uncommentable hidden role="status"></p>
<script>{JS}</script>
"""
    open(os.path.join(out, p + ".html"), "w").write(html)
    print(p, title, len(html) // 1024, "KB", len(notes), "notes")

json.dump(titles, open(os.path.join(out, "titles.json"), "w"), indent=1)
