import test from 'node:test';
import assert from 'node:assert/strict';
import {hospitalDocumentTable} from '../src/components/hospital-documents.js';
test('医院资料历史保留替换关系、中文类型且转义文件名和原因',()=>{
 const html=hospitalDocumentTable([{id:7,originalName:'<img src=x>.pdf',purpose:'LICENSE',reason:'<script>x</script>',fileSize:2048,replaced:1,previousFileId:3,createdAt:'2026-11-08T12:00:00'}]);
 for(const text of ['历史版本','替换 #3','执照资料','2 KB','data-hospital-document-download="7"'])assert.ok(html.includes(text));assert.ok(!html.includes('<img'));assert.ok(!html.includes('<script'));assert.ok(html.includes('type="button"'));
});
