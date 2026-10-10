import sys,re,html
t=open(sys.argv[1],encoding='utf-8',errors='ignore').read()
t=re.sub(r'(?s)<script.*?</script>|<style.*?</style>','',t); t=re.sub(r'<[^>]+>',' ',t); t=html.unescape(re.sub(r'\s+',' ',t))
open(sys.argv[1]+'.txt','w').write(t)
