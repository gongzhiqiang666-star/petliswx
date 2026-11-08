import test from 'node:test';
import assert from 'node:assert/strict';
import {JSDOM} from 'jsdom';
import {compareTextFormats} from './text-format.js';
const document=new JSDOM('<!doctype html><html><body></body></html>').window.document;
test('equivalent nested spans compare by inherited format and text rather than wrapper count',()=>{
 const a='<p><span style="color:rgb(255,0,0)"><span style="font-size:14px">报告</span></span></p>';
 const b='<p><span style="color:red;font-size:14px">报</span><span style="color:red;font-size:14px">告</span></p>';
 assert.equal(compareTextFormats(document,a,b).equivalent,true);
});
test('format moved to different text fails even when global attribute counts are identical',()=>{
 const a='<p><span style="color:red">甲</span><span style="color:blue">乙</span></p>';
 const b='<p><span style="color:blue">甲</span><span style="color:red">乙</span></p>';
 const result=compareTextFormats(document,a,b);assert.equal(result.equivalent,false);assert.deepEqual(result.changedProperties,['color']);assert.ok(!JSON.stringify(result).includes('甲'));assert.ok(!JSON.stringify(result).includes('red'));
});
test('table cell vertical alignment is not confused with inline text alignment',()=>{
 const a='<table><tbody><tr><td style="vertical-align:top">报告</td></tr></tbody></table>';
 const b='<table><tbody><tr><td style="vertical-align:top"><p>报告</p></td></tr></tbody></table>';
 assert.equal(compareTextFormats(document,a,b).equivalent,true);
 assert.equal(compareTextFormats(document,'<p><span style="vertical-align:super">报告</span></p>','<p>报告</p>').equivalent,false);
});
test('underline propagated from an ancestor is compared as visible decoration',()=>{
 const a='<p style="text-decoration-line:underline"><span>报告</span></p>';
 const b='<p><span style="text-decoration-line:underline">报告</span></p>';
 assert.equal(compareTextFormats(document,a,b).equivalent,true);
 assert.equal(compareTextFormats(document,a,'<p><span>报告</span></p>').equivalent,false);
 const atomic='<p style="text-decoration-line:underline"><span style="display:inline-block">报告</span></p>';
 assert.equal(compareTextFormats(document,atomic,'<p><span style="display:inline-block">报告</span></p>').equivalent,true);
});
test('color diagnostics expose numeric transitions and counts, never report text',()=>{
 const result=compareTextFormats(document,'<p style="color:red">私密</p>','<p style="color:blue">私密</p>');
 assert.ok(result.transitions.some(item=>item.property==='color'&&item.characters===2));assert.ok(!JSON.stringify(result.transitions).includes('私密'));
});
test('font diagnostics redact unknown names and distinguish whitespace from visible text',()=>{
 const result=compareTextFormats(document,'<p style="font-family:PrivateFont123">甲 </p>','<p style="font-family:Arial">甲 </p>');
 const font=result.transitions.find(item=>item.property==='font-family');assert.equal(font.before,'other');assert.equal(font.after,'Arial');assert.equal(font.characters,2);assert.equal(font.nonWhitespaceCharacters,1);assert.ok(!JSON.stringify(font).includes('PrivateFont123'));
});
test('font sample refers to the actual changed text node after adjacent equal formats',()=>{
 const a='<p style="font-family:Arial;font-style:normal"><span>甲</span><em style="font-style:normal">乙</em></p>';
 const b='<p style="font-family:Arial;font-style:normal"><span>甲</span><em style="font-style:normal;font-family:Times">乙</em></p>';
 assert.equal(compareTextFormats(document,a,b).fontSample.before[0].tag,'em');
});
