import test from 'node:test';
import assert from 'node:assert/strict';
import {JSDOM} from 'jsdom';
import {assertEditableHtml} from './admin-editor-safety.js';
const document=new JSDOM('').window.document;
test('editor accepts report text, table geometry, unicode and images without stripping content',()=>{
 const html='<table style="width:800px"><colgroup><col width="120"></colgroup><tr><td><font face="宋体">检测结果</font><img src="https://example.invalid/report.png"></td></tr></table>';
 assert.equal(assertEditableHtml(document,html),html);
});
test('unsafe source refuses editing with a clear error instead of silently removing report content',()=>{
 for(const html of ['<script>alert(1)</script>','<img src=x onerror=alert(1)>','<a href="javascript:alert(1)">正文</a>','<iframe src="https://example.invalid"></iframe>','<span style="background:url(https://example.invalid/a)">原文</span>'])assert.throws(()=>assertEditableHtml(document,html),/暂不能安全编辑/);
});
