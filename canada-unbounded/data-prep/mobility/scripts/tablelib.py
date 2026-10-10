from html.parser import HTMLParser
class P(HTMLParser):
    def __init__(s):
        super().__init__(); s.tables=[]; s.cur=None; s.row=None; s.cell=None; s.incap=False
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
def tables(path):
    p=P(); p.feed(open(path,encoding='utf-8').read()); return p.tables
def find(path,startswith):
    for t in tables(path):
        if ' '.join(t['caption'].split()).startswith(startswith): return t
    raise KeyError(startswith)
