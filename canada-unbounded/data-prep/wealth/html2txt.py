import re,sys,html
t=open(sys.argv[1],encoding='utf-8',errors='ignore').read()
m=re.search(r'<main.*?</main>',t,re.S)
t=m.group(0) if m else t
t=re.sub(r'<(script|style).*?</\1>','',t,flags=re.S)
t=re.sub(r'<(br|p|div|h\d|li|tr|table)[^>]*>','\n',t)
t=re.sub(r'<t[dh][^>]*>',' | ',t)
t=re.sub(r'<[^>]+>','',t)
t=html.unescape(t)
t=re.sub(r'[ \t]+',' ',t); t=re.sub(r'\n\s*\n+','\n',t)
print(t)
