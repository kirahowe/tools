# Review pages

`build.py` turns each rendered post in `site/_site` into a self-contained page for review on a phone:
charts inlined, footnotes collected at the end, and a Comment button that opens the claude.ai
comment composer on whatever paragraph, chart or table the reader taps.

    ./build.sh
    python3 -I review/build.py site/_site review/out [urls.json]

`urls.json` (not committed) maps post names to the published review-page URLs so Next/Previous links work.
