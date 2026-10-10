import sys, re, html
from html.parser import HTMLParser
src=open(sys.argv[1],encoding='utf-8').read()
class P(HTMLParser):
    def __init__(s):
        super().__init__(); s.tables=[]; s.cur=None; s.row=None; s.cell=None; s.cap=None; s.incap=False
    def handle_starttag(s,t,a):
        if t=='table': s.cur={'caption':'','rows':[]}
        elif t=='caption' and s.cur is not None: s.incap=True
        elif t=='tr' and s.cur is not None: s.row=[]
        elif t in('td','th') and s.row is not None: s.cell=''
    def handle_endtag(s,t):
        if t=='table' and s.cur is not None: s.tables.append(s.cur); s.cur=None
        elif t=='caption': s.incap=False
        elif t=='tr' and s.row is not None: s.cur['rows'].append(s.row); s.row=None
        elif t in('td','th') and s.cell is not None: s.row.append(' '.join(s.cell.split())); s.cell=None
    def handle_data(s,d):
        if s.incap: s.cur['caption']+=d
        elif s.cell is not None: s.cell+=d
p=P(); p.feed(src)
for i,t in enumerate(p.tables):
    print('=== TABLE',i,' '.join(t['caption'].split()))
    for r in t['rows']: print(' | '.join(r))
