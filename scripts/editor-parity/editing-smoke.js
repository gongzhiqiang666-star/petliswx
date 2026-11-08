import {columnEditingSmoke} from './column-editing-smoke.js';
import {tableEditingSmoke} from './table-editing-smoke.js';
import assert from 'node:assert/strict';
export async function editingSmoke(page){
 const errors=[];const onError=error=>errors.push(error.name);page.on('pageerror',onError);
 const html='<p>前<strong><span style="font-family:Arial">报告</span></strong>后</p>';
 await page.evaluate(html=>window.petlisMountEditor(html),html);
 const snapshot=()=>page.evaluate(()=>({text:window.petlisLiveEditor.getText(),html:window.petlisLiveEditor.getHTML()}));
 try{
  const initial=await snapshot();assert.equal(initial.text,'前报告后');
  await page.evaluate(()=>{const editor=window.petlisLiveEditor;editor.view.focus();const node=editor.view.dom.querySelector('strong span').firstChild;const range=document.createRange();range.setStart(node,1);range.collapse(true);const selection=window.getSelection();selection.removeAllRanges();selection.addRange(range);});
  await page.keyboard.type('X');assert.equal((await snapshot()).text,'前报X告后');
  await page.keyboard.press('ControlOrMeta+z');assert.equal((await snapshot()).text,initial.text);
  await page.keyboard.press('ControlOrMeta+Shift+z');assert.equal((await snapshot()).text,'前报X告后');
  await page.keyboard.press('Backspace');assert.equal((await snapshot()).text,initial.text);
  await page.evaluate(html=>window.petlisMountEditor(html),html);
  await page.evaluate(()=>{const editor=window.petlisLiveEditor;editor.view.focus();const node=editor.view.dom.querySelector('strong span').firstChild,last=editor.view.dom.querySelector('p').lastChild;const range=document.createRange();range.setStart(node,1);range.setEnd(last,1);const selection=window.getSelection();selection.removeAllRanges();selection.addRange(range);});
  await page.keyboard.type('Y');assert.equal((await snapshot()).text,'前报Y');
  await page.keyboard.press('ControlOrMeta+z');assert.equal((await snapshot()).text,initial.text);
  const final=await snapshot();assert.equal(final.html,initial.html);assert.equal(errors.length,0);assert.ok(final.html.includes('<strong>'));assert.equal(await page.evaluate(()=>window.petlisLiveEditor.view.dom.querySelector('strong span').style.fontFamily),'Arial');
  const tables=await tableEditingSmoke(page),columns=await columnEditingSmoke(page);assert.equal(errors.length,0);
  return {...tables,...columns,scope:'synthetic only; nested inline and legacy table editing',nativeTyping:true,nativeUndo:true,nativeRedo:true,nativeBackspace:true,crossWrapperSelectionReplacement:true,wrappersRetained:true,htmlRestoredExactly:true,pageErrors:0,fullEditingApproved:false};
 }finally{await page.evaluate(()=>window.petlisUnmountEditor());page.off('pageerror',onError);}
}
