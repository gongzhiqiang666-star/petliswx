import test from 'node:test';import assert from 'node:assert/strict';
import {JSDOM} from 'jsdom';
import {textDiagnostic,compareBreakAwareText} from './text-diagnostic.js';
test('diagnostic classifies text changes without emitting report text',()=>{
 const result=textDiagnostic('私密\n报告','私密 报告');assert.deepEqual(result.sourceChangeKinds,{newline:1});assert.deepEqual(result.outputChangeKinds,{space:1});assert.ok(!JSON.stringify(result).includes('私密'));
});
test('explicit BR and newline are compared without hiding removed business text',()=>{
 const {window}=new JSDOM('<!doctype html><html><body></body></html>');
 assert.equal(compareBreakAwareText(window.document,'<pre><br></pre><p>报告</p>','<pre><code>\n</code></pre><p>报告</p>'),true);
 assert.equal(compareBreakAwareText(window.document,'<p>报告<br>金额</p>','<p>报告\n</p>'),false);
});
