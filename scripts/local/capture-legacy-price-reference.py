"""Execute only three unmodified pure PHP helpers, no framework/database/network calls."""
from pathlib import Path
import hashlib,json,re,subprocess,random
root=Path(__file__).resolve().parents[2]
source=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application/common.php')
text=source.read_text();names=['DiscountToInt','OrginalDiscount','showPrice'];chunks=[]
for name in names:
 match=re.search(r'\bfunction\s+'+name+r'\s*\(',text);assert match
 start=match.start();opening=text.index('{',match.end());depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 chunks.append(text[start:end])
rng=random.Random(20261108)
prices=["12.50","1.005","2.675","-0.005","10000.00","0.125","0.375","-0.125","-0.0001","-0","0.005","9999999999999999.99","90071992547409.91"]
for _ in range(200):
 scale=rng.randint(0,6);amount=rng.randint(-999999999999,999999999999)
 sign='-' if amount<0 else '';digits=str(abs(amount)).rjust(scale+1,'0')
 prices.append(sign+(digits[:-scale]+'.'+digits[-scale:] if scale else digits))
program='<?php\n'+'\n'.join(chunks)+'''\nforeach (["原价","其它折扣","其他折扣","9","7.55","0","",null,0,9] as $value) {echo json_encode(["input"=>$value,"originalDiscount"=>OrginalDiscount($value),"discountToInt"=>DiscountToInt($value)],JSON_UNESCAPED_UNICODE),PHP_EOL;}
foreach (''' + json.dumps(prices,ensure_ascii=False) + ''' as $value) {echo json_encode(["input"=>$value,"showPrice"=>showPrice($value)],JSON_UNESCAPED_UNICODE),PHP_EOL;}
'''
result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4'],input=program,text=True,capture_output=True,check=True)
rows=[json.loads(line) for line in result.stdout.splitlines()]
assert len(rows)==10+len(prices)
output={'source':'application/common.php','sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runtime':'existing local PHP7.4; isolated pure functions only','rows':rows,'bcMathExecuted':False,'completionClaimed':False}
(root/'docs/php-parity/pricing-reference.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'pureReferenceCases':len(rows),'sourceUnmodified':True,'bcMathExecuted':False}))
