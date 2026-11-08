import test from 'node:test';
import assert from 'node:assert/strict';
import {articleFields,articleData} from '../src/components/articles.js';
test('编辑资讯读取数据库字段并将标题与正文按表单文本转义',()=>{
 const html=articleFields({title:'<script>x</script>',category:'临床',type:'live',content:'</textarea><img src=x>',live_at:'2026-11-10 12:30:00',live_url:'https://example.test/live',popup_enabled:1});
 assert.ok(html.includes('2026-11-10T12:30'));assert.ok(html.includes('https://example.test/live'));assert.ok(!html.includes('<script>'));assert.ok(!html.includes('<img'));assert.ok(html.includes('&lt;/textarea&gt;'));
});
test('保存草稿允许空正文并且剔除操作原因等非文章字段',()=>{
 const data=articleData({title:'草稿',category:'临床',type:'live',reason:'修改原因',popupEnabled:'true'});
 assert.equal(data.content,'');assert.equal(data.liveAt,null);assert.equal(data.popupEnabled,true);assert.equal(data.reason,undefined);
});
