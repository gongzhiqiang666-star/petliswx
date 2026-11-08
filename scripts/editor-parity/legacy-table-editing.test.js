import test from 'node:test';import assert from 'node:assert/strict';import {JSDOM} from 'jsdom';
import {getSchema} from '@tiptap/core';import {EditorState} from '@tiptap/pm/state';
import {editorConfiguration} from './editor-configuration.js';import {legacyExtensions} from './legacy-extensions.js';import {LegacyInline} from './legacy-inline.js';import {hierarchyContent} from './hierarchy-content.js';
import {tableEditing} from '@tiptap/pm/tables';
import {changedLegacyTableNeedsProtection,protectLegacyTableEditing} from './legacy-table-editing.js';
const document=new JSDOM('<!doctype html><html><body></body></html>').window.document;
function state(html){const configuration=editorConfiguration([...legacyExtensions(),LegacyInline]),schema=getSchema(configuration.extensions);return EditorState.create({schema,doc:schema.nodeFromJSON(hierarchyContent(document,html,configuration))});}
test('ordinary typing in an irregular imported table is distinguished from valid tables',()=>{
 const old=state('<table><tbody><tr height="40"></tr><tr><td><p>格</p></td></tr></tbody></table>');let pos;old.doc.descendants((node,p)=>{if(node.isText&&node.text==='格')pos=p;});
 assert.equal(changedLegacyTableNeedsProtection(old.doc,old.tr.insertText('X',pos+1).doc),true);
 const regular=state('<table><tbody><tr><td><p>格</p></td></tr></tbody></table>');regular.doc.descendants((node,p)=>{if(node.isText&&node.text==='格')pos=p;});assert.equal(changedLegacyTableNeedsProtection(regular.doc,regular.tr.insertText('X',pos+1).doc),false);
 assert.equal(changedLegacyTableNeedsProtection(old.doc,old.doc),false);
});
test('new table nodes keep the ordinary table repair policy',()=>{
 const old=state('<p>尾</p>');const table=old.schema.nodes.table.create({legacyColumns:null});const next=old.tr.insert(0,table).doc;
 assert.equal(changedLegacyTableNeedsProtection(old.doc,next),false);
});

test('table policy wrapper is idempotent and preserves the original handlers and state',()=>{
 const original=tableEditing(),protectedPlugin=protectLegacyTableEditing(original);
 assert.equal(protectLegacyTableEditing(protectedPlugin),protectedPlugin);assert.equal(protectedPlugin.spec.props,original.spec.props);assert.equal(protectedPlugin.spec.state,original.spec.state);
});
