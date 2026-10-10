#!/usr/bin/env bash
# Render every notebook to Quarto markdown with Clay, then build the site with Quarto.
set -euo pipefail
cd "$(dirname "$0")"
clojure -M:clay -A:markdown
cp _quarto.yml site/
cp src/economics/wealth_distribution/canada/styles.css site/economics/wealth_distribution/canada/ 2>/dev/null || true
quarto render site
echo "Site built in site/_site"
