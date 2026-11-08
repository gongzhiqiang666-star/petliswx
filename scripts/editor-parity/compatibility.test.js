import test from 'node:test';
import assert from 'node:assert/strict';
import {roundTrip} from './baseline-probe.js';
import {legacyExtensions} from './legacy-extensions.js';
import {checkFidelity} from './fidelity-gate.js';
test('legacy block, font and table cell attributes survive without edits',()=>{
 const source='<div style="page-break-after:always"><p align="center" style="margin:0;letter-spacing:1px"><font face="宋体" color="#123456">原文</font></p><table width="640" border="1" cellpadding="3" cellspacing="0"><tbody><tr height="40"><td width="120" valign="top" style="height:40px;line-height:24px">{宠物姓名}</td></tr></tbody></table></div>';
 const result=checkFidelity(source,roundTrip(source,legacyExtensions()));assert.equal(result.passed,true,JSON.stringify(result));
});
test('legacy column group and inline report image survive import',()=>{
 const source='<table width="640"><colgroup><col width="120"><col width="520" style="width:520px"></colgroup><tbody><tr><td>一</td><td>二</td></tr></tbody></table><p>签章<img src="https://petlis.invalid/seal.png" width="80" height="40" alt="印章">末尾</p>';
 const result=checkFidelity(source,roundTrip(source,legacyExtensions()));assert.equal(result.passed,true,JSON.stringify(result));
});
test('embedded PNG report image is not silently removed',()=>{
 const source='<p>签章<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB" width="80" height="40" alt="印章"></p>';
 assert.equal(checkFidelity(source,roundTrip(source,legacyExtensions())).passed,true);
});
test('CSS bold weight is not promoted into a nested strong that makes text bolder twice',()=>{
 const source='<p><span style="font-weight:700">报告</span></p>';
 const result=roundTrip(source,legacyExtensions());assert.ok(!result.includes('<strong'));assert.ok(result.includes('font-weight: 700'));
 const originalStrong=roundTrip('<p><strong>标题</strong></p>',legacyExtensions());assert.ok(originalStrong.includes('<strong>'));
});
test('nested legacy strong retains both weight levels',()=>{
 const source='<p><strong>外层<strong>内层</strong>末尾</strong></p>';
 const output=roundTrip(source,legacyExtensions());assert.equal((output.match(/<strong/g)??[]).length,4);assert.ok(output.includes('<strong><strong>内层</strong></strong>'));
});
test('CSS vertical alignment does not add implicit smaller superscript or subscript wrappers',()=>{
 for(const [alignment,tag] of [['super','sup'],['sub','sub']]){
  const output=roundTrip(`<p><span style="font-size:14px;vertical-align:${alignment}">项目</span></p>`,legacyExtensions());assert.ok(!output.includes('<'+tag));assert.ok(output.includes('vertical-align: '+alignment));
  assert.ok(roundTrip(`<p><${tag}>原标记</${tag}></p>`,legacyExtensions()).includes('<'+tag+'>'));
 }
});
test('all legacy column groups are retained rather than only the first one',()=>{
 const source='<table><colgroup><col width="120"></colgroup><colgroup><col width="200"><col width="300"></colgroup><tbody><tr><td>一</td><td>二</td><td>三</td></tr></tbody></table>';
 const result=checkFidelity(source,roundTrip(source,legacyExtensions()));assert.equal(result.passed,true,JSON.stringify(result));
});
test('legacy table without a column group does not get generated column widths on export',()=>{
 const output=roundTrip('<table width="400"><tbody><tr><td width="100">一</td><td width="300">二</td></tr></tbody></table>',legacyExtensions());assert.ok(!output.includes('<colgroup'));
});
test('legacy empty layout table does not get an invented row',()=>{
 const output=roundTrip('<table style="width:100%"></table>',legacyExtensions());assert.ok(!output.includes('<tr'));
});
