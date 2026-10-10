import re,html,sys
t=open(sys.argv[1],encoding='utf-8',errors='replace').read()
for k in ['citation_title','citation_authors','citation_journal_title','citation_volume','citation_issue','citation_firstpage','citation_lastpage','citation_year','citation_date']:
    m=re.search(r'name="%s" content="([^"]*)"'%k,t)
    if m: print(k,':',html.unescape(m.group(1)))
m=re.search(r'<div id="abstract-body">(.*?)</div>',t,re.S)
print('ABSTRACT:',html.unescape(re.sub('<[^>]+>','',m.group(1))).strip() if m else 'none')
