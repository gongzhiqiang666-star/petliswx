import test from 'node:test';
import assert from 'node:assert/strict';
import {checkFidelity} from './fidelity-gate.js';
import {roundTrip} from './baseline-probe.js';
test('unchanged legacy markup passes without exposing content',()=>{
 const html='<table width="640"><tbody><tr><td rowspan="2" style="line-height:24px">隐私内容</td></tr></tbody></table>';
 assert.deepEqual(checkFidelity(html,html),{passed:true,textChanged:false,missing:[]});
});
test('gate detects style removed from one cell even when another cell retains it',()=>{
 const before='<table><tbody><tr><td style="color:red">A</td><td style="color:red">B</td></tr></tbody></table>';
 const after='<table><tbody><tr><td>A</td><td style="color:red">B</td></tr></tbody></table>';
 const result=checkFidelity(before,after);assert.equal(result.passed,false);assert.ok(result.missing.some(v=>v.kind==='style'&&v.name==='color'&&v.count===1));
});
test('baseline extensions fail legacy widths, pagination and font wrapper fidelity',()=>{
 const html='<div style="page-break-after:always"><table width="640"><colgroup><col width="120"></colgroup><tbody><tr height="40"><td width="120" valign="top"><font face="宋体">原内容</font></td></tr></tbody></table></div>';
 const result=checkFidelity(html,roundTrip(html));assert.equal(result.passed,false);assert.ok(result.missing.some(v=>v.name==='width'));assert.ok(result.missing.some(v=>v.name==='page-break-after'));assert.ok(!JSON.stringify(result).includes('原内容'));assert.ok(!JSON.stringify(result).includes('宋体'));
});
test('text and placeholder changes reject adoption',()=>{
 assert.equal(checkFidelity('<p>{宠物姓名} ----分页符号----</p>','<p>宠物姓名</p>').textChanged,true);
});
test('HTML default spans of one are equivalent to omission; real merges remain required',()=>{
 const input='<table><tbody><tr><td rowspan="1" colspan="1">A</td></tr></tbody></table>';
 const plain='<table><tbody><tr><td>A</td></tr></tbody></table>';
 assert.equal(checkFidelity(input,plain).passed,true);
 assert.equal(checkFidelity(input.replace('rowspan="1"','rowspan="2"'),plain).passed,false);
});
