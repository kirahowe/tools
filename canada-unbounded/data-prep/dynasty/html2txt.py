import re,html,sys
t=open(sys.argv[1],encoding='utf-8',errors='replace').read()
t=re.sub(r'(?is)<(script|style|nav|header|footer)[^>]*>.*?</\1>',' ',t)
t=re.sub(r'(?i)<br\s*/?>|</p>|</h[1-6]>|</li>','\n',t)
t=re.sub(r'<[^>]+>',' ',t)
t=html.unescape(t)
lines=[re.sub(r'[ \t]+',' ',l).strip() for l in t.split('\n')]
print('\n'.join(l for l in lines if len(l)>40))
