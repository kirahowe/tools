#!/usr/bin/env bash
# Render every notebook to Quarto markdown with Clay, then build the website with Quarto.
# Requires the Clojure CLI and Quarto. Output: site/_site/
set -euo pipefail
cd "$(dirname "$0")"
notebooks=(src/economics/wealth_distribution/canada/c*_*.clj)
clojure -M:clay -A:markdown "${notebooks[@]}"
cp _quarto.yml index.qmd site/
quarto render site
echo "Site built in site/_site"
