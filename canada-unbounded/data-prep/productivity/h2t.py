import re,html,sys
s=open(sys.argv[1],encoding='utf-8',errors='ignore').read()
s=re.sub(r'(?s)<(script|style).*?</\1>','',s); t=html.unescape(re.sub(r'<[^>]+>','\n',s)); t=re.sub(r'\n\s*\n+','\n',t)
open(sys.argv[2],'w').write(t)
