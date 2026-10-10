import sys,re,html
for f in sys.argv[1:]:
    t=open(f,encoding='utf-8',errors='ignore').read()
    for m in re.finditer(r'<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>',t,re.S):
        txt=html.unescape(re.sub("<[^>]+>","",m.group(2))).strip().split("\n")[0]
        if re.search(r'wealth|billionaire|inequal|capital gain|luxury|bank|dividend|minimum tax|surtax',txt,re.I):
            print(f.split('/')[-1],'|',txt[:150],'|',m.group(1))
