import {JSDOM} from 'jsdom';
import test from 'node:test';import assert from 'node:assert/strict';
import {roundTrip} from './baseline-probe.js';import {legacyExtensions} from './legacy-extensions.js';import {LegacyInline} from './legacy-inline.js';
const parse=html=>new JSDOM(html).window.document;
test('inline hierarchy retains CSS overrides inside strong and native link color ordering',()=>{
 const html='<p><strong><span style="font-weight:700">标题</span></strong><span style="color:rgb(13,13,13)"><a href="https://petlis.invalid/report">报告</a></span></p>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);
 assert.equal(parse(output).querySelector('strong > span').style.fontWeight,'700');
 assert.equal(parse(output).querySelector('p > span > a').parentElement.style.color,'rgb(13, 13, 13)');
});
test('inline font wrapper directly inside a div is not discarded while making a text block',()=>{
 const html='<div><span style="font-family:Microsoft YaHei">正文</span></div>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);assert.ok(parse(output).querySelector('span').style.fontFamily.includes('Microsoft YaHei'));assert.equal(parse(output).querySelector('span').textContent,'正文');
});
test('legacy span around paragraphs preserves its font for leading and nested text',()=>{
 const html='<div><span style="font-family:Microsoft YaHei">前文<p>正文</p>后文</span></div>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);const span=parse(output).querySelector('div > span');assert.ok(span.style.fontFamily.includes('Microsoft YaHei'));assert.equal(span.firstElementChild.textContent,'前文');assert.equal(span.firstElementChild.style.margin,'0px');assert.equal(span.textContent,'前文正文后文');
});
test('hierarchy import does not delete a literal newline inside a styled inline container',()=>{
 const html='<p><span style="font-family:Arial">\n</span>正文</p>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);assert.ok(output.includes('>\n</span>正文'));
});
test('legacy pre keeps its inline formatting and explicit break without forcing a code wrapper',()=>{
 const html='<pre><span style="color:red">原文</span><br></pre>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);assert.equal(parse(output).querySelector('pre > span').style.color,'red');assert.ok(parse(output).querySelector('pre > br'));assert.ok(!output.includes('<code>'));
});
test('legacy pre compatibility does not become the default filler for empty table cells',()=>{
 const output=roundTrip('<table><tbody><tr><td> </td></tr></tbody></table>',[...legacyExtensions(),LegacyInline]);assert.ok(!output.includes('<pre'));
});
test('ordinary paragraph stays ahead of legacy pre when schema needs an implicit text block',async()=>{
 const {getSchema}=await import('@tiptap/core');const {editorConfiguration}=await import('./editor-configuration.js');
 const schema=getSchema(editorConfiguration([...legacyExtensions(),LegacyInline]).extensions);
 assert.equal(Object.values(schema.nodes).find(type=>type.isTextblock&&type.defaultAttrs)?.name,'paragraph');
});
test('inline report image does not split away the surrounding font container',()=>{
 const html='<div><span style="font-family:Microsoft YaHei"><img src="https://petlis.invalid/seal.png" width="80"><br><br>正文<br></span></div>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]);const span=parse(output).querySelector('img').closest('span');assert.ok(span.style.fontFamily.includes('Microsoft YaHei'));assert.equal(span.textContent,'正文');assert.equal(span.querySelectorAll('br').length,3);
});
test('legacy inline retains original CSS declarations including Office metadata and duplicate declarations',()=>{
 const style='font-family:Arial;mso-bidi-font-family:SimSun;color:red;color:blue;';
 const output=roundTrip(`<p><span style="${style}">正文</span></p>`,[...legacyExtensions(),LegacyInline]);
 assert.ok(output.includes(`style="${style}"`));
});
test('layout declarations retain separate borders, margins and page-break spelling',()=>{
 const html='<div style="page-break-after:always"><p style="margin-top:0;margin-right:0;margin-bottom:0;margin-left:0;mso-pagination:widow-orphan">正文</p><table><tbody><tr><td style="border-left:1px solid red;border-top:0 none blue;">格</td></tr></tbody></table></div>';
 const output=roundTrip(html,[...legacyExtensions(),LegacyInline]),root=parse(output);
 assert.equal(root.querySelector('div').getAttribute('style'),'page-break-after:always');
 assert.equal(root.querySelector('p').getAttribute('style'),'margin-top:0;margin-right:0;margin-bottom:0;margin-left:0;mso-pagination:widow-orphan');
 assert.equal(root.querySelector('td').getAttribute('style'),'border-left:1px solid red;border-top:0 none blue;');
});
test('custom inline data metadata survives editor import and export',()=>{
 const html='<p><span data-bm="bookmark-1" data-business-field="report-value" aria-label="报告字段">正文</span></p>';
 const span=parse(roundTrip(html,[...legacyExtensions(),LegacyInline])).querySelector('span');assert.equal(span.getAttribute('data-bm'),'bookmark-1');assert.equal(span.getAttribute('data-business-field'),'report-value');assert.equal(span.getAttribute('aria-label'),'报告字段');
});
test('legacy empty spacer row is preserved without inventing a new cell',()=>{
 const source='<table><tbody><tr height="40"></tr><tr><td>格</td></tr></tbody></table>';
 const root=parse(roundTrip(source,[...legacyExtensions(),LegacyInline]));assert.equal(root.querySelector('tr').children.length,0);assert.equal(root.querySelector('tr').getAttribute('height'),'40');
});
test('table indentation does not become extra report rows or cells',()=>{
 const source='<table>\n<tbody>\n<tr>\n<td> A B </td>\n</tr>\n</tbody>\n</table>';
 const root=parse(roundTrip(source,[...legacyExtensions(),LegacyInline])),table=root.querySelector('table');assert.equal(table.rows.length,1);assert.equal(table.rows[0].cells.length,1);assert.equal(table.rows[0].cells[0].textContent,' A B ');
});
