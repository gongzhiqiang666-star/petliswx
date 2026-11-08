"""Decode the real locally-authenticated HTML label with independent ZXing-C++; synthetic orders only."""
import argparse
import importlib.util
import json
from pathlib import Path
import re
import urllib.request
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw
import zxingcpp
spec=importlib.util.spec_from_file_location('staff_setup',Path(__file__).with_name('setup-php-lab.py'))
setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
parser=argparse.ArgumentParser();parser.add_argument('--order-id',type=int,required=True);parser.add_argument('--saved-html',type=Path);args=parser.parse_args()
assert args.order_id>0
row=setup.config.sql(f"SELECT UserNO,AnimalName FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={args.order_id}").split('\t')
assert len(row)==2 and row[1].startswith('QA_REAL_'),'Only synthetic QA orders'
expected=row[0]
if args.saved_html:
    expected_path=Path.home()/f'.local/share/petlis/qa/barcode-before-{args.order_id}.html'
    assert args.saved_html==expected_path and not args.saved_html.is_symlink(),'Only private synthetic baseline HTML'
    html=args.saved_html.read_text()
else:
    token=setup.administrator(4)
    request=urllib.request.Request(setup.config.BASE+f'/admin/print/specimens/{args.order_id}',headers={'Authorization':'Bearer '+token})
    with setup.opener.open(request,timeout=20) as response:html=response.read().decode()
match=re.search(r'<svg\b.*?</svg>',html,re.S);assert match,'No SVG label returned'
svg=ET.fromstring(match.group(0));view=list(map(float,svg.attrib['viewBox'].split()))
width,height=round(view[2]),round(view[3]);image=Image.new('L',(width*3,height*3),255);draw=ImageDraw.Draw(image)
last=0
for rect in svg:
    if rect.attrib.get('fill')!='black':continue
    x,y,w,h=(float(rect.attrib[k]) for k in ['x','y','width','height'])
    last=max(last,x+w)
    draw.rectangle((round(x*3),round(y*3),round((x+w)*3)-1,round((y+h)*3)-1),fill=0)
result=zxingcpp.read_barcode(image,formats=zxingcpp.BarcodeFormat.Code39)
actual=result.text if result else None
print(json.dumps({'orderId':args.order_id,'expected':expected,'decoded':actual,'matches':actual==expected,'allBarsInsideViewport':last<=width,'width':width,'lastBarEdge':last,'decoder':'ZXing-C++'},ensure_ascii=False,indent=2))
assert actual==expected,'Printed barcode does not decode to the original order identifier'
assert last<=width,'Barcode clipped by SVG viewport'
