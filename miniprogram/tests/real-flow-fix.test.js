const test=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const path=require('node:path')
const root=path.join(__dirname,'..')

test('畜主英文名在页面和提交校验中均为可选',()=>{
 const page=fs.readFileSync(path.join(root,'pages/order-add-pet/order-add-pet.js'),'utf8')
 const wxml=fs.readFileSync(path.join(root,'pages/order-add-pet/order-add-pet.wxml'),'utf8')
 assert.doesNotMatch(page,/请填写畜主英文名/)
 assert.doesNotMatch(wxml,/aria-label="畜主英文名"[\s\S]{0,150}aria-required="true"/)
})

test('报告弹窗预览和下载都携带登录认证',()=>{
 const source=fs.readFileSync(path.join(root,'components/report-modal/report-modal.js'),'utf8')
 const downloads=[...source.matchAll(/wx\.downloadFile\(\{([\s\S]*?)\n\s*\}\)/g)]
 assert.equal(downloads.length,2)
 for(const match of downloads)assert.match(match[1],/header:\s*request\.getHeaders\(true\)/)
})
