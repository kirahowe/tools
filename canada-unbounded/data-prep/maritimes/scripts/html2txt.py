import sys, html, re
from html.parser import HTMLParser
class P(HTMLParser):
    def __init__(s):
        super().__init__(); s.out=[]; s.skip=0
    def handle_starttag(s,t,a):
        if t in ('script','style','noscript'): s.skip+=1
        if t in ('p','h1','h2','h3','li','br','div','time'): s.out.append('\n')
    def handle_endtag(s,t):
        if t in ('script','style','noscript'): s.skip-=1
    def handle_data(s,d):
        if not s.skip: s.out.append(d)
for f in sys.argv[1:]:
    p=P(); p.feed(open(f,encoding='utf-8',errors='replace').read())
    t=re.sub(r'\n\s*\n+','\n',''.join(p.out))
    print('=====',f); print(t)
