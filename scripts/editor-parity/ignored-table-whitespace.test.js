import test from 'node:test';import assert from 'node:assert/strict';import {JSDOM} from 'jsdom';
import {removeIgnoredTableWhitespace,tableContentText} from './ignored-table-whitespace.js';
const document=new JSDOM('<!doctype html><html><body></body></html>').window.document;
test('only table structural indentation is omitted, cell spaces and line breaks survive',()=>{
 const source='<table>\n<tbody>\n<tr>\n<td> \t A\nB </td>\n</tr>\n</tbody>\n</table>';
 assert.equal(tableContentText(document,source),' \t A\nB ');
});
test('outside table whitespace, pre text and non-ASCII spaces survive',()=>{
 const root=document.createElement('div');root.innerHTML='<div> \n </div><pre>\n\n原文</pre><table><tbody><tr><td>\u00a0</td></tr></tbody></table>';
 const text=root.textContent;removeIgnoredTableWhitespace(root);assert.equal(root.textContent,text);
});
test('table with an explicit non-table display retains its direct whitespace',()=>{
 const root=document.createElement('div');root.innerHTML='<table style="display:block;white-space:pre">\n<tbody><tr><td>格</td></tr></tbody></table>';
 removeIgnoredTableWhitespace(root);assert.equal(root.querySelector('table').firstChild.data,'\n');
});
