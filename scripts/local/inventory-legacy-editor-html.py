"""Read-only local template HTML inventory. Emits structure counts, never template text or URLs."""
import importlib.util,json,re
from pathlib import Path
from collections import Counter
from html.parser import HTMLParser
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'))
staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
class Inventory(HTMLParser):
 def __init__(self):
  super().__init__(convert_charrefs=False);self.tags=Counter();self.attrs=Counter();self.styles=Counter();self.markers=Counter();self.spans=Counter();self.images=Counter()
 def handle_starttag(self,tag,attrs):
  self.tags[tag]+=1
  for key,value in attrs:
   self.attrs[tag+'.'+key]+=1
   if key in ('rowspan','colspan') and re.fullmatch(r'\d+',value or ''):self.spans[tag+'.'+key+'.'+value]+=1
   if tag=='img' and key=='src':self.images['embedded-data' if (value or '').startswith('data:') else 'http' if (value or '').startswith(('http://','https://')) else 'other']+=1
   if key=='style':
    for item in (value or '').split(';'):
     name=item.partition(':')[0].strip().lower()
     if re.fullmatch(r'[-a-z]+',name):self.styles[name]+=1
 def handle_data(self,data):
  for marker in ('----分页符号----','{报告链接}','{用户付款链接}','{当前时间}','{诊断项名称}'):
   self.markers[marker]+=data.count(marker)
report={}
for name,query in {
 'reportTemplates':"SELECT HEX(IFNULL(ModelInfo,'')) FROM lin_lb_infotesttypemodel",
 'mailTemplates':"SELECT HEX(IFNULL(model_content,'')) FROM lin_lb_dictmaildiagnosisreport"
}.items():
 parser=Inventory();count=0
 for line in staff.config.sql(query).splitlines():
  if not line:continue
  body=bytes.fromhex(line).decode('utf-8');body=re.sub(r'\\u([0-9a-fA-F]{4})',lambda m:chr(int(m[1],16)),body)
  parser.feed(body);count+=1
 report[name]={'nonEmptyDocuments':count,'tags':dict(sorted(parser.tags.items())),'attributes':dict(sorted(parser.attrs.items())),'styleProperties':dict(sorted(parser.styles.items())),'markers':dict(parser.markers),'spanValues':dict(sorted(parser.spans.items())),'imageSourceKinds':dict(parser.images)}
path=Path('docs/php-parity/editor-html-inventory.json');path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'output':str(path),'documents':{k:v['nonEmptyDocuments'] for k,v in report.items()},'readOnly':True}))
