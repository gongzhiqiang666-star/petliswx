import assert from 'node:assert/strict';
export async function columnEditingSmoke(page){
 const mount=html=>page.evaluate(html=>window.petlisMountEditor(html),html);
 const html=()=>page.evaluate(()=>window.petlisLiveEditor.getHTML());
 const select=text=>page.evaluate(text=>{const editor=window.petlisLiveEditor;editor.view.focus();let pos;editor.state.doc.descendants((node,p)=>{if(node.isText&&node.text===text)pos=p;});editor.commands.setTextSelection(pos+1);},text);
 const widths=()=>page.evaluate(()=>{const root=document.createElement('div');root.innerHTML=window.petlisLiveEditor.getHTML();return [...root.querySelectorAll('col')].map(col=>col.getAttribute('width'));});
 const source='<table><colgroup class="left"><col width="100"></colgroup><colgroup class="right"><col width="300"></colgroup><tbody><tr><td>一</td><td>二</td></tr></tbody></table><p>尾</p>';
 await mount(source);let initial=await html();await select('一');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.addColumnAfter()),true);
 assert.deepEqual(await widths(),['100',null,'300']);assert.equal(await page.evaluate(()=>window.petlisLiveEditor.view.dom.querySelector('tr').cells.length),3);
 await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 await mount(source);initial=await html();await select('二');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.addColumnBefore()),true);assert.deepEqual(await widths(),['100',null,'300']);await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 const spanned='<table><colgroup><col width="80" span="2"></colgroup><colgroup><col width="300"></colgroup><tbody><tr><td colspan="2">合并</td><td>二</td></tr></tbody></table><p>尾</p>';
 await mount(spanned);initial=await html();await select('合并');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.addColumnAfter()),true);assert.deepEqual(await widths(),['80','80',null,'300']);await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 await mount(spanned);initial=await html();await select('合并');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.deleteColumn()),true);assert.deepEqual(await widths(),['300']);await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 const three='<table><colgroup class="left"><col width="100"><col></colgroup><colgroup class="right"><col width="300"></colgroup><tbody><tr><td>一</td><td>新</td><td>二</td></tr></tbody></table><p>尾</p>';
 await mount(three);initial=await html();await select('新');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.deleteColumn()),true);assert.deepEqual(await widths(),['100','300']);
 assert.deepEqual(await page.evaluate(()=>[...window.petlisLiveEditor.view.dom.querySelectorAll('td')].map(cell=>cell.textContent)),['一','二']);
 await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 const twoTables='<table><colgroup><col width="100"></colgroup><tbody><tr><td>删</td></tr></tbody></table><table><colgroup><col width="900"></colgroup><tbody><tr><td>保留</td></tr></tbody></table><p>尾</p>';
 await mount(twoTables);initial=await html();await select('删');assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.deleteColumn()),false);assert.equal(await html(),initial);assert.equal(await page.evaluate(()=>window.petlisLiveEditor.commands.deleteTable()),true);assert.deepEqual(await widths(),['900']);
 await page.keyboard.press('ControlOrMeta+z');assert.equal(await html(),initial);
 return {columnInsertWidthsAndUndo:true,columnInsertBeforeAndUndo:true,spannedColumnsInsertDeleteUndo:true,columnDeleteWidthsAndUndo:true,lastColumnRefusalKeepsBothTables:true,deletingWholeTableKeepsNextColumns:true};
}
