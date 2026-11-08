import assert from 'node:assert/strict';
export async function layoutSmoke(page){
 const html='<table style="width:200px;border:1px solid black"><tbody><tr><td>格</td></tr></tbody></table>';
 const same=await page.evaluate(({a,b})=>window.petlisCompareLayouts(a,b),{a:html,b:html});assert.equal(same.equal,true);
 const width=await page.evaluate(({a,b})=>window.petlisCompareLayouts(a,b),{a:html,b:html.replace('200px','300px')});assert.equal(width.equal,false);assert.ok(width.properties.width>0);
 const color=await page.evaluate(({a,b})=>window.petlisCompareLayouts(a,b),{a:html,b:html.replace('solid black','solid red')});assert.equal(color.equal,false);assert.ok(color.properties.borderTopColor>0);
 const gif='data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
 const image=await page.evaluate(html=>window.petlisSnapshotLayout(html),'<img src="'+gif+'">');assert.equal(image.nodes[0].width,1);assert.equal(image.nodes[0].height,1);assert.equal(image.dataImagesFailed,0);
 return {scope:'synthetic layout control fixtures only',identicalLayoutAccepted:true,widthChangeDetected:true,borderColorChangeDetected:true,inlineDataImageDecoded:true,fullLayoutApproved:false};
}
