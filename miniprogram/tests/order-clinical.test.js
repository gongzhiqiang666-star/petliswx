const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path')
function load(request,api){let definition;vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../pages/order-detail/order-detail.js'),'utf8'),{Page:x=>definition=x,require:id=>id.includes('api/index')?api:id.includes('logistics')?{adaptShipment:x=>x}:request,wx:{showToast(){}},console:{error(){}},Date,setTimeout,clearTimeout});return {...definition,data:JSON.parse(JSON.stringify(definition.data)),setData(update){Object.assign(this.data,update)}}}
test('临床资料失败仅影响独立卡片，重试后显示最新资料',async()=>{let fail=true;const p=load({get:async url=>{assert.equal(url,'/orders/9/clinical');if(fail)throw Error('network');return {fields:{clinicalSymptoms:'修正后症状'}}}},{});p._orderId=9;await p.loadClinical();assert.equal(p.data.clinicalError,true);assert.equal(p.data.error,false);fail=false;await p.loadClinical();assert.equal(p.data.clinical.clinicalSymptoms,'修正后症状');assert.equal(p.data.clinicalError,false);assert.equal(p.data.clinicalLoading,false)})
test('订单重试使用保存的单号，不把点击事件当作订单ID发送',async()=>{let requested;const p=load({get:async()=>({fields:{}})},{orders:{getDetail:async id=>{requested=id;return {orderId:9,sampleStatus:0,items:[]}}}});p._orderId=9;p.loadLogistics=async()=>{};await p.loadOrderDetail({currentTarget:{dataset:{id:9}}});assert.equal(requested,9);assert.equal(p.data.orderData.orderId,9);assert.equal(p.data.error,false)})

test('退款财务状态与原因在详情保留，原送检状态不被覆盖',async()=>{
 const p=load({get:async()=>({fields:{}})},{orders:{getDetail:async()=>({orderId:9,sampleStatus:2,items:[],refunded:true,refundMock:true,refundAmount:'88.00',refundReason:'PHP管理员#4：物流异常退款',refundedAt:'2026-11-08T00:00:00'})}})
 p.loadLogistics=async()=>{};await p.loadOrderDetail(9)
 assert.equal(p.data.orderData.refunded,true);assert.equal(p.data.orderData.refundMock,true);assert.equal(p.data.orderData.refundReason,'物流异常退款');assert.equal(p.data.orderData.refundAmount,'88.00');assert.equal(p.data.orderData.sampleStatus,2)
})

test('审核记录保留原文并独立格式化时间，不暴露原始ISO展示',async()=>{
 const message='审核意见：'+ '请补充清晰资质材料'.repeat(30),row={id:7,message,createdAt:'2026-11-08T07:57:32'}
 const p=load({get:async()=>({fields:{}})},{orders:{getDetail:async()=>({orderId:9,sampleStatus:0,items:[],reviewHistory:[row]})}})
 p.loadLogistics=async()=>{};await p.loadOrderDetail(9)
 assert.equal(p.data.orderData.reviewHistory[0].createdAtLabel,'2026-11-08 07:57');assert.equal(p.data.orderData.reviewHistory[0].message,message);assert.equal(row.createdAt,'2026-11-08T07:57:32');assert.equal(row.createdAtLabel,undefined)
})
test('审核记录无有效时间时明确提示，不把Invalid Date显示给用户',async()=>{
 const p=load({get:async()=>({fields:{}})},{orders:{getDetail:async()=>({orderId:9,sampleStatus:0,items:[],reviewHistory:[{id:1,message:'资料待补',createdAt:'not-a-date'}]})}})
 p.loadLogistics=async()=>{};await p.loadOrderDetail(9);assert.equal(p.data.orderData.reviewHistory[0].createdAtLabel,'时间未记录')
})
