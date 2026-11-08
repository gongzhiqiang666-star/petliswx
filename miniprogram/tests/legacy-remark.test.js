const test=require('node:test'),assert=require('node:assert/strict')
const {fromHtml,toHtml}=require('../utils/legacy-remark')
test('备注段落、嵌套格式与实体转为原生delta并保留内容',()=>{
 const d=fromHtml('<p>冷藏 <strong>血清&amp;<em>复检</em></strong></p><p><u>当天寄出</u>&nbsp;&#x1F436;</p>')
 assert.equal(toHtml(d),'<p>冷藏 <strong>血清&amp;</strong><strong><em>复检</em></strong></p><p><u>当天寄出</u> 🐶</p>')
 assert.equal(toHtml(fromHtml('')),'');assert.equal(toHtml({ops:[{insert:'\n'}]}),'')
})
test('未支持的原表格图片样式与不完整标签不能被降级为可编辑内容',()=>{
 for(const html of ['<table><tr><td>原表</td></tr></table>','<p><img src="https://never-fetch.invalid"></p>','<p style="color:red">原色</p>','<p><strong>缺闭合</p>','<p>第一行<br>第二行</p>','<p>&copy;</p>','<p><script>1</script></p>'])assert.throws(()=>fromHtml(html),/保留/)
})
test('原生delta未知属性与图片嵌入拒绝保存，换行和用户文本必须转义',()=>{
 assert.equal(toHtml({ops:[{insert:'<img> & "文本"\n第二段\n'}]}),'<p>&lt;img&gt; &amp; "文本"</p><p>第二段</p>')
 assert.throws(()=>toHtml({ops:[{insert:{image:'wxfile://a'}},{insert:'\n'}]}),/保留/)
 assert.throws(()=>toHtml({ops:[{insert:'对齐',attributes:{align:'center'}},{insert:'\n'}]}),/保留/)
 assert.throws(()=>toHtml({ops:[{insert:'没有终止换行'}]}),/保留/)
})

test('原生输出的空段落可以重开，相邻同格式delta合并后比较一致',()=>{const d={ops:[{insert:'甲',attributes:{bold:true}},{insert:'乙',attributes:{bold:true}},{insert:'\n\n末段\n'}]};const html=toHtml(d);assert.equal(html,'<p><strong>甲乙</strong></p><p><br></p><p>末段</p>');assert.equal(toHtml(fromHtml(html)),html)})
