const test=require('node:test')
const assert=require('node:assert/strict')
const {loadPage}=require('./helpers')

test('新送检先选项目，清除其他宠物的草稿身份',()=>{
 const {instance,storage}=loadPage('order-test-items/order-test-items')
 storage.order_form={petId:1,_draftId:99,selectedTestItems:[{infoTestTypeId:8}]}
 instance.loadLaboratories=()=>{}
 instance.onLoad({petId:'7'})
 assert.equal(storage.order_form.petId,'7')
 assert.equal(storage.order_form.flowVersion,'project-first')
 assert.equal(storage.order_form._draftId,undefined)
})
test('选项目后进入采样页并保留实验室和价格，旧草稿仍可接续',()=>{
 const {instance,storage,wx}=loadPage('order-test-items/order-test-items')
 let destination;wx.navigateTo=o=>destination=o.url
 storage.order_form={petId:7,flowVersion:'project-first'}
 instance.setData({selectedIds:[8],allItemsMap:{8:{infoTestTypeId:8,price:180}},laboratoryId:2,laboratoryName:'实验室'})
 instance.onNext()
 assert.equal(storage.order_form.selectedTestItems[0].price,180)
 assert.equal(storage.order_form.laboratoryId,2)
 assert.equal(destination,'/pages/order-add-pet/order-add-pet?petId=7')
 storage.order_form={petId:7,_draftId:99}
 instance.onNext()
 assert.equal(destination,'/pages/order-preview/order-preview')
 assert.equal(storage.order_form._draftId,99)
})
test('返回修改项目时保留采样输入与草稿版本，不覆盖其他宠物',()=>{
 const {instance,storage}=loadPage('order-add-pet/order-add-pet')
 storage.order_form={petId:7,flowVersion:'project-first',_draftId:99,_draftVersion:2,selectedTestItems:[{infoTestTypeId:8}]}
 instance.setData({projectFirst:true,petId:7,petInfo:{animalName:'豆豆'},sampleInfo:{sampleArea:'前肢',infoSampleTypeId:3},selectedSampleTypeName:'血清',deliveryInfo:{doctorName:'李医生'},animalInfo:{age:'3Y'},otherInfo:{remark:'复查'}})
 instance.onUnload()
 assert.equal(storage.order_form.sampleInfo.sampleArea,'前肢')
 assert.equal(storage.order_form.sampleInfo.sampleTypeName,'血清')
 assert.equal(storage.order_form._draftVersion,2)
 assert.equal(storage.order_form.selectedTestItems[0].infoTestTypeId,8)
 storage.order_form={petId:9}
 instance.onUnload()
 assert.equal(storage.order_form.petId,9)
 assert.equal(storage.order_form.sampleInfo,undefined)
})
test('确认页核价失败可重试或返回项目，不清空填写资料',async()=>{
 const {instance,storage,wx}=loadPage('order-preview/order-preview')
 storage.order_form={petId:7,sampleInfo:{sampleArea:'前肢'}}
 instance.setData({formData:{flowVersion:'project-first',userCouponId:9}})
 global.getCurrentPages=()=>[{route:'pages/order-test-items/order-test-items'},{route:'pages/order-add-pet/order-add-pet'},{route:'pages/order-preview/order-preview'}]
 let coupon,delta;instance.requote=async id=>coupon=id;wx.navigateBack=o=>delta=o.delta
 await instance.retryQuote();assert.equal(coupon,9)
 instance.editProjects();assert.equal(delta,2)
 assert.equal(storage.order_form.sampleInfo.sampleArea,'前肢')
 let destination;global.getCurrentPages=()=>[{route:'pages/drafts/drafts'},{route:'pages/order-add-pet/order-add-pet'},{route:'pages/order-preview/order-preview'}]
 wx.redirectTo=o=>destination=o.url
 instance.editProjects();assert.equal(destination,'/pages/order-test-items/order-test-items')
})

test('退回重提回填原宠物关联、物种、生日、采样和四项临床资料',async()=>{
 const {instance,storage}=loadPage('order-detail/order-detail')
 instance._rawOrder={orderId:9,petId:17,canResubmit:true,infoAnimalId:6,animalTypeId:33,animalType:'猫品种',birthday:'2023-01-02T00:00:00',infoSampleTypeId:4,sampleArea:'前肢',collectDt:'2026-11-07',medicalHistory:'病史',clinicalSymptoms:'症状',diagnosis:'诊断',medication:'用药',items:[]}
 await instance.editAndResubmit()
 const form=storage.order_form;assert.equal(form.petId,17);assert.equal(form.petInfo.infoAnimalId,6);assert.equal(form.petInfo.birthday,'2023-01-02');assert.equal(form.sampleInfo.infoSampleTypeId,4)
 assert.equal(form.clinicalInfo.medicalHistory,'病史');assert.equal(form.clinicalInfo.medication,'用药')
})
test('采样草稿恢复临床资料，换宠物不带入上一只宠物的资料',()=>{
 const {instance,storage}=loadPage('order-add-pet/order-add-pet')
 storage.order_form={petId:7,petInfo:{},sampleInfo:{},deliveryInfo:{},otherInfo:{},animalInfo:{age:'3Y'},clinicalInfo:{clinicalSymptoms:'本单症状'}}
 instance.onLoad({petId:'7',draft:'1'});assert.equal(instance.data.clinicalInfo.clinicalSymptoms,'本单症状')
 const other=loadPage('order-add-pet/order-add-pet');other.storage.order_form=storage.order_form;other.instance.loadPetDetail=()=>{}
 other.instance.onLoad({petId:'8'});assert.equal(other.instance.data.clinicalInfo.clinicalSymptoms,'')
})

test('成功结果保留申请号、移除本地草稿并阻止再次提交，直达同单详情',async()=>{
 const {instance,storage,wx,app}=loadPage('order-preview/order-preview');storage.order_form={petId:7};let destination;
 wx.reLaunch=o=>destination=o.url;
 instance.completeSubmission(123);assert.equal(instance.data.submittedOrderId,123);assert.equal(storage.order_form,undefined);
 await instance.onSubmit();instance.viewSubmittedOrder();assert.equal(destination,'/pages/order-detail/order-detail?id=123');
 wx.removeStorageSync=()=>{throw Error('storage unavailable')};app.clearCachedOrderForm=()=>{throw Error('cache unavailable')};assert.doesNotThrow(()=>instance.completeSubmission(124));assert.equal(instance.data.submittedOrderId,124);
});
