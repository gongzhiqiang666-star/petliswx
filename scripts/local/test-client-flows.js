// Logic tests for native mini-program pages. Does not substitute for simulator/device rendering.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert/strict')
const root=path.resolve(__dirname,'../..','miniprogram')
const tick=()=>new Promise(resolve=>setImmediate(resolve))
function loadPage(name,request,api,wx={},app={getHospitalId:()=>1}){
 let definition
 const context={console:{log(){},warn(){},error(){}},require(id){if(id.includes('api/index'))return api;if(id.includes('utils/request'))return request;if(id.includes('utils/age-calculator'))return require(path.join(root,'utils/age-calculator.js'));if(id.includes('utils/pinyin-full'))return require(path.join(root,'utils/pinyin-full.js'));if(id.includes('config/notification'))return {};if(id.includes('logger'))return {log(){},error(){}};throw new Error('Unexpected dependency '+id)},getApp:()=>app,Page(value){definition=value},wx:{showToast(){},showModal:async()=>({confirm:true}),...wx},setTimeout(fn){fn();return 1},clearTimeout(){}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'pages',name,name+'.js'),'utf8'),context)
 const page={...definition,data:JSON.parse(JSON.stringify(definition.data)),setData(update){for(const [key,value] of Object.entries(update)){const parts=key.split('.');let target=this.data;for(const part of parts.slice(0,-1))target=target[part]||(target[part]={});target[parts.at(-1)]=value}}}
 return page
}
async function orderFilters(){
 const queries=[];const api={orders:{getList:async q=>{queries.push(q);return {content:[{orderId:1,sampleStatus:q.status || 0}],last:true}}}}
 const page=loadPage('orders',{},api)
 page.onTabChange({currentTarget:{dataset:{id:-3}}});await tick()
 assert.equal(queries.at(-1).status,-3);assert.equal(page.data.filteredOrders.length,1,'numeric pending-payment tab must show matching orders')
 page.onSearchInput({detail:{value:'older-pet'}});await tick();assert.equal(queries.at(-1).keyword,'older-pet','search must query all matching server pages')
 page.dateChange({currentTarget:{dataset:{key:'startDate'}},detail:{value:'2026-10-01'}});await tick();assert.equal(queries.at(-1).startDate,'2026-10-01')
}
async function staleOrderResponses(){
 const requests=[];const api={orders:{getList:q=>new Promise(resolve=>requests.push({q,resolve}))}}
 const page=loadPage('orders',{},api)
 page.resetAndLoadOrders();page.onTabChange({currentTarget:{dataset:{id:'3'}}})
 assert.equal(requests.length,2)
 requests[1].resolve({content:[{orderId:2,sampleStatus:3}],last:true});await tick()
 requests[0].resolve({content:[{orderId:1,sampleStatus:0}],last:true});await tick()
 assert.equal(page.data.filteredOrders[0].orderId,2,'older response must not overwrite a newer filter')
}
async function mockRechargeCannotPayOrCredit(){
 let paid=0;const request={post:async()=>({paymentNo:'RC-test',mock:true,status:0}),get:async url=>url==='/account'?{balance:10}:{items:[],total:0}}
 const page=loadPage('recharge',request,{}, {requestPayment:async()=>{paid++}})
 await page.submit();await tick()
 assert.equal(paid,0);assert.equal(page.data.balance,10,'development payment cannot invent a new balance')
}
async function expiredSessionReturnsToLogin(){
 const app={globalData:{token:'fixture',hospitalId:1,userInfo:{}}};let destination;const removed=[]
 const context={module:{exports:{}},require:id=>id.includes('config')?{baseURL:'http://127.0.0.1:8082/api',timeout:1000,storageKeys:{TOKEN:'token',USER_INFO:'user',HOSPITAL_ID:'hospital'}}:{},wx:{removeStorageSync:key=>removed.push(key),reLaunch:o=>{destination=o.url;o.complete()}},getApp:()=>app,getCurrentPages:()=>[{route:'pages/orders/orders'}],console:{log(){},error(){}}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'utils/request.js'),'utf8'),context)
 assert.throws(()=>context.module.exports.handleResponse({statusCode:401,data:{}}))
 assert.equal(destination,'/pages/login/login');assert.equal(app.globalData.token,null);assert.equal(removed.length,3)
}
async function reportDownloadsStayOnOwnBackend(){
 let requested;const context={module:{exports:{}},require:()=>({downloadFile:async url=>{requested=url;return '/tmp/fixture.pdf'}}),wx:{openDocument:async()=>{}}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'utils/reports.js'),'utf8'),context)
 await context.module.exports.openReportFromURL('https://untrusted.example/api/pdf/view/12?token=ignored')
 assert.equal(requested,'/pdf/view/12')
}
async function invoiceProfileEditing(){
 let saved
 const request={put:async(url,body)=>{saved={url,body}},get:async url=>url.endsWith('/profiles')?[{id:7,title:'Updated'}]:{items:[],total:0}}
 const page=loadPage('invoices',request,{})
 page.setData({profiles:[{id:7,title:'Original',taxNumber:'123',email:'a@example.com',bankAccount:'00123'}]})
 page.editProfile();assert.equal(page.data.editingId,7);assert.equal(page.data.bankAccount,'00123')
 page.field({currentTarget:{dataset:{field:'title'}},detail:{value:' Updated '}})
 await page.saveProfile();await tick()
 assert.equal(saved.url,'/invoices/profiles/7');assert.equal(saved.body.title,'Updated');assert.equal(saved.body.bankAccount,'00123');assert.equal(page.data.adding,false)
}
async function notificationDeliveryRetry(){
 let configured=false,subscriptions=0,posted=[]
 const request={get:async url=>url==='/inbox/templates'?(configured?[{eventType:'order',templateId:'fixture-template'}]:[]):url.endsWith('/deliveries')?[{status:'failed',errorMessage:'fixture failure'}]:url==='/inbox/unread-count'?1:{items:[{id:9,eventType:'order',pushStatus:'failed'}],total:1},post:async url=>posted.push(url)}
 const page=loadPage('notifications',request,{}, {requestSubscribeMessage:async()=>{subscriptions++;return {'fixture-template':'accept'}}})
 await page.load(true);assert.equal(page.data.items[0].canRetry,true)
 await page.deliveryLogs({currentTarget:{dataset:{id:9}}});assert.equal(page.data.deliveries[0].statusLabel,'发送失败')
 await page.retryDelivery({currentTarget:{dataset:{id:9}}});assert.equal(posted.length,0);assert.equal(subscriptions,0)
 configured=true;await page.retryDelivery({currentTarget:{dataset:{id:9}}});assert.equal(posted[0],'/inbox/9/retry');assert.equal(subscriptions,1);assert.equal(page.data.retryingId,null)
}
async function updatesRequireUserConsent(){
 const handlers={};let apply=0,confirm=false
 const manager={onUpdateReady:fn=>handlers.ready=fn,onUpdateFailed:fn=>handlers.failed=fn,applyUpdate:()=>apply++}
 const context={module:{exports:{}},wx:{getUpdateManager:()=>manager,showModal:async()=>({confirm}),showToast(){}}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'utils/updates.js'),'utf8'),context)
 context.module.exports.installUpdates();await handlers.ready();assert.equal(apply,0)
 confirm=true;await handlers.ready();assert.equal(apply,1)
}
async function boundedPrivateDiagnostics(){
 const storage={};const context={module:{exports:{}},require:id=>{assert.equal(id,'../config/routes');return ['pages/orders/orders']},getCurrentPages:()=>[{route:'pages/orders/orders'}],wx:{getStorageSync:key=>storage[key],setStorageSync:(key,value)=>storage[key]=value,removeStorageSync:key=>delete storage[key]}}
 vm.runInNewContext(fs.readFileSync(path.join(root,'utils/diagnostics.js'),'utf8'),context)
 const diagnostic=context.module.exports
 for(let i=0;i<25;i++)diagnostic.capture('error','TypeError: SECRET_TOKEN mobile=19912345678\n at fn (https://example.test/orders.js:10:2?token=SECRET_TOKEN)')
 const rows=diagnostic.read();assert.equal(rows.length,20);assert.equal(rows[0].type,'TypeError');assert.equal(rows[0].frames[0],'orders.js:10:2');assert.equal(rows[0].route,'pages/orders/orders')
 assert.equal(JSON.stringify(rows).includes('SECRET_TOKEN'),false);assert.equal(JSON.stringify(rows).includes('19912345678'),false)
 diagnostic.clear();assert.equal(diagnostic.read().length,0)
 context.wx.setStorageSync=()=>{throw new Error('storage full')};assert.doesNotThrow(()=>diagnostic.capture('error','fixture'))
}
async function agePickerRespondsToAsyncPetDetails(){
 let definition;const context={Component:value=>definition=value}
 vm.runInNewContext(fs.readFileSync(path.join(root,'components/age-picker/age-picker.js'),'utf8'),context)
 let changed=0;const component={data:JSON.parse(JSON.stringify(definition.data)),properties:{value:''},...definition.methods,setData(update){Object.assign(this.data,update)},triggerEvent(){changed++}}
 definition.lifetimes.attached.call(component);assert.equal(component.data.yearIndex,1)
 definition.properties.value.observer.call(component,'3Y9M')
 assert.equal(component.data.yearIndex,3);assert.equal(component.data.monthIndex,9);assert.equal(changed,0,'Prop sync must not rewrite the parent form')
 definition.properties.value.observer.call(component,'7M');assert.equal(component.data.yearIndex,0);assert.equal(component.data.monthIndex,7)
}
async function homeRefreshesIdentityAndMessages(){
 let refreshes=0,reads=0,unread=2,release
 const profileReady=new Promise(resolve=>release=resolve)
 const app={globalData:{userInfo:{}},isLoggedIn:()=>true,async refreshUserInfo(){refreshes++;await profileReady;this.globalData.userInfo={nickname:'张医生',hospital:{hospitalName:'测试医院'}}}}
 const request={get:async url=>{if(url==='/articles/popup')return {};if(url==='/inbox/unread-count')return unread;if(url==='/inbox'){reads++;return {items:[]}};if(url==='/account')return {balance:0};return {items:[],content:[],totalElements:0}}}
 const api={orders:{getList:q=>request.get('/orders',q)}}
 const page=loadPage('home',request,api, {},app)
 const first=page.waitForLoginAndLoad(),duplicate=page.waitForLoginAndLoad()
 assert.equal(refreshes,1,'Concurrent lifecycle calls share the same refresh')
 release();await Promise.all([first,duplicate])
 assert.equal(page.data.doctorName,'张医生');assert.equal(page.data.hospitalName,'测试医院');assert.equal(page.data.notificationCount,2);assert.equal(reads,1)
 unread=3;await page.waitForLoginAndLoad();assert.equal(page.data.notificationCount,3,'Returning to home refreshes unread messages');assert.equal(refreshes,2);assert.equal(page.data.notificationLoading,false)
}
async function homeLoadsRealDashboard(){
 const queries=[],destinations=[]
 const app={globalData:{userInfo:{nickname:'张医生',hospitalName:'测试医院'}},isLoggedIn:()=>true,async refreshUserInfo(){}}
 const request={get:async url=>{if(url==='/articles/popup')return {};if(url==='/inbox/unread-count')return 0;if(url==='/inbox')return {items:[{id:1,title:'报告已完成',message:'请查看',createdAt:'2026-10-25T08:00:00Z'},{id:2,title:'订单已审核',message:'样品已接收',createdAt:'2026-10-25T07:00:00Z'}]};if(url==='/account')return {balance:128.5};throw new Error('Unexpected request '+url)}}
 const api={orders:{getList:async q=>{queries.push(q);if(q.status!==undefined)return {content:[],totalElements:{[-3]:4,[0]:2,[2]:7}[q.status]};return {content:[{orderId:31,animalName:'豆豆',sampleStatus:2,createDt:'2026-10-25T06:00:00Z'}],total:1}}}}
 const page=loadPage('home',request,api,{navigateTo:o=>destinations.push(o.url),switchTab:o=>destinations.push(o.url)},app)
 await page.waitForLoginAndLoad()
 assert.deepEqual(Array.from(page.data.todoChips,x=>x.value),[4,2,7]);assert.equal(page.data.recentOrders[0].petName,'豆豆');assert.equal(page.data.recentOrders[0].statusLabel,'等待报告');assert.equal(page.data.balance,128.5);assert.equal(page.data.recentActivities.length,2)
 assert.ok(queries.some(q=>q.page===0&&q.size===1&&q.status===-3));assert.ok(queries.some(q=>q.page===0&&q.size===3&&q.status===undefined))
 page.onTodoTap();assert.equal(destinations.at(-1),'/pages/orders/orders')
 page.onOrderSelect();assert.equal(destinations.at(-1),'/pages/order-select-pet/order-select-pet')
 page.openShortcut({currentTarget:{dataset:{id:'coupons'}}});assert.equal(destinations.at(-1),'/pages/coupons/coupons')
 page.onRecentOrderTap({currentTarget:{dataset:{id:31}}});assert.equal(destinations.at(-1),'/pages/order-detail/order-detail?id=31')
}
async function homeFailuresDoNotBecomeZeroCounts(){
 const request={get:async url=>{if(url==='/account')throw new Error('offline');return {items:[]}}}
 const page=loadPage('home',request,{orders:{getList:async()=>{throw new Error('offline')}}})
 assert.equal(typeof page.loadTodoSummary,'function')
 await page.loadTodoSummary();assert.deepEqual(Array.from(page.data.todoChips,x=>x.value),[null,null,null]);assert.ok(page.data.todoError)
 await page.loadAccount();assert.equal(page.data.balance,null);assert.ok(page.data.financeError)
}
async function homeOnlyLatestModuleResponseApplies(){
 async function exercise(loadName,callsPerLoad,createPage,success,assertValue,errorKey,loadingKey){
  const pending=[]
  const next=()=>{let resolve,reject;const promise=new Promise((yes,no)=>{resolve=yes;reject=no});const item={promise,resolve,reject};pending.push(item);return promise}
  const page=createPage(next),groups=[]
  const start=()=>{const promise=page[loadName]();groups.push(pending.splice(0));return promise}
  const settle=(group,label,fail=false)=>group.forEach((item,index)=>fail?item.reject(new Error('stale failure')):item.resolve(success(label,index)))
  const old=start(),newer=start();assert.equal(groups[0].length,callsPerLoad);assert.equal(groups[1].length,callsPerLoad)
  settle(groups[1],'new');await newer;settle(groups[0],'old');await old;assertValue(page,'new')
  const oldFailure=start(),fresh=start();settle(groups[3],'fresh');await fresh;settle(groups[2],'stale',true);await oldFailure
  assertValue(page,'fresh');assert.equal(page.data[errorKey],'',loadName+' stale failure must not set an error');assert.equal(page.data[loadingKey],false)
 }
 await exercise('loadRecentNotifications',2,next=>loadPage('home',{get:url=>url==='/articles/popup'?Promise.resolve({}):next()},{}), (label,index)=>index===0?{items:[{id:label,title:label,message:label,createdAt:'2026-10-25T08:00:00Z'}]}:label==='new'?8:label==='fresh'?9:2,
  (page,label)=>{assert.equal(page.data.recentActivities[0].title,label);assert.equal(page.data.notificationCount,label==='new'?8:9)},'notificationError','notificationLoading')
 await exercise('loadTodoSummary',3,next=>loadPage('home',{}, {orders:{getList:next}}),(label,index)=>({content:[],totalElements:(label==='new'?[4,5,6]:label==='fresh'?[7,8,9]:[1,2,3])[index]}),
  (page,label)=>assert.deepEqual(Array.from(page.data.todoChips,x=>x.value),label==='new'?[4,5,6]:[7,8,9]),'todoError','todoLoading')
 await exercise('loadRecentOrders',1,next=>loadPage('home',{}, {orders:{getList:next}}),label=>({content:[{orderId:label,animalName:label,sampleStatus:2}]}),
  (page,label)=>assert.equal(page.data.recentOrders[0].petName,label),'ordersError','ordersLoading')
 await exercise('loadAccount',1,next=>loadPage('home',{get:next},{}),label=>({balance:label}),
  (page,label)=>assert.equal(page.data.balance,label),'financeError','financeLoading')
}
async function homeQualificationAndOrderStatusViewMapping(){
 let account={balance:20,reviewStatus:'approved'},rows=[]
 const page=loadPage('home',{get:async()=>account},{orders:{getList:async()=>({content:rows})}})
 assert.deepEqual(Array.from(page.data.shortcuts,item=>item.id),['test','pets','reports','notifications'])
 assert.deepEqual(Array.from(page.data.services,item=>item.id),['account','coupons','orders','invoices'])
 await page.loadAccount();assert.equal(page.data.qualificationStatusLabel,'资质审核通过');assert.equal(page.data.qualificationTone,'success')
 account={balance:20,reviewStatus:'pending'};await page.loadAccount();assert.equal(page.data.qualificationStatusLabel,'资质待审核');assert.equal(page.data.qualificationTone,'pending')
 account={balance:20,reviewStatus:'rejected'};await page.loadAccount();assert.equal(page.data.qualificationStatusLabel,'资质已驳回');assert.equal(page.data.qualificationTone,'rejected')
 account={balance:20};await page.loadAccount();assert.equal(page.data.qualificationStatusLabel,'');assert.equal(page.data.qualificationTone,'')
 rows=[-3,1,4].map((sampleStatus,index)=>({orderId:index+1,animalName:'Pet',sampleStatus}));await page.loadRecentOrders()
 assert.deepEqual(Array.from(page.data.recentOrders,item=>item.statusTone),['warn','primary','muted'])
 rows=[0,2,3].map((sampleStatus,index)=>({orderId:index+4,animalName:'Pet',sampleStatus}));await page.loadRecentOrders()
 assert.deepEqual(Array.from(page.data.recentOrders,item=>item.statusTone),['primary','primary','muted'])
}
async function mineLoadsRealBalanceAndHandlesFailure(){
 let fail=false,destination
 const page=loadPage('mine',{get:async()=>{if(fail)throw new Error('offline');return {balance:36.8}}},{},{navigateTo:o=>destination=o.url})
 assert.equal(typeof page.loadAccount,'function')
 await page.loadAccount();assert.equal(page.data.balance,36.8);assert.equal(page.data.financeError,'')
 page.onRecharge();assert.equal(destination,'/pages/recharge/recharge')
 fail=true;await page.loadAccount();assert.equal(page.data.balance,null);assert.ok(page.data.financeError)
}
async function reportPagingAndRetry(){
 const queries=[];let fail=false,destination
 const api={reports:{getList:async q=>{queries.push(q.pageNum);if(fail)throw new Error('network unavailable');return {content:[{reportId:q.pageNum+1,animalName:'Pet',diagnosisStatus:1}],totalElements:40}}}}
 const page=loadPage('report-list',{},api,{navigateTo:o=>destination=o.url})
 await page.loadReports();assert.equal(queries[0],0);assert.equal(page.data.filteredReports[0].reportId,1)
 fail=true;await page.loadReports(true);assert.equal(page.data.currentPage,1);assert.ok(page.data.error)
 fail=false;page.retry();await tick();assert.equal(queries.at(-1),1);assert.equal(page.data.currentPage,2);assert.equal(page.data.filteredReports.length,2)
 page.onViewReport({currentTarget:{dataset:{id:1}}});assert.equal(destination,'/pages/report-view/report-view?id=1')
}
async function devMockPaymentOnlyCompletesLocally(){
 const load=baseURL=>{
  const calls=[];let paid=0
  const context={module:{exports:{}},console:{log(){},error(){}},setTimeout:(fn)=>{fn();return 1},clearTimeout(){},
   require:id=>{if(id.includes('config'))return {baseURL};if(id.includes('request'))return {post:async url=>{calls.push(url);return url.includes('/payment/create')?{paymentNo:'P1',mock:true}:{}},get:async()=>({status:1})};throw new Error('Unexpected dependency '+id)},
   wx:{requestPayment:async()=>{paid++}}}
  vm.runInNewContext(fs.readFileSync(path.join(root,'utils/payment.js'),'utf8'),context)
  return {calls,paid,payOrder:context.module.exports.payOrder}
 }
 const local=load('http://127.0.0.1:8082/api')
 assert.equal(await local.payOrder(123),true,'local development mock callback must confirm the payment')
 assert.equal(local.paid,0,'mock payment must never invoke wx.requestPayment')
 assert.ok(local.calls.includes('/payment/mock-callback/P1'))
 const remote=load('https://api.example.test/api')
 await assert.rejects(remote.payOrder(123),/开发模式/)
 assert.equal(remote.paid,0);assert.equal(remote.calls.some(url=>url.includes('mock-callback')),false,'non-local environment must never call the development mock callback')
}
async function orderAgeDisplayIsFormattedWithoutChangingFormValue(){
 const draft={petId:7,petInfo:{animalName:'Draft Pet'},sampleInfo:{sampleTypeName:''},deliveryInfo:{},otherInfo:{remark:''},animalInfo:{age:'3Y9M',animalWeight:''}}
 const wx={showLoading(){},hideLoading(){},getStorageSync:key=>key==='order_form'?draft:null,removeStorageSync(){}}
 const api={pets:{getDetail:async()=>({animalName:'Pet',birthday:'2023-01-01',ownerName:'Owner'})}}
 const page=loadPage('order-add-pet',{},api,wx)
 page.setData({petId:7});await page.loadPetDetail(7)
 assert.equal(page.data.animalInfo.age,'3Y9M');assert.equal(page.data.displayAge,'3岁9个月','Pet details must derive a Chinese display age')
 for(const [age,label] of [['3Y','3岁'],['9M','9个月'],['3Y9M','3岁9个月']]){
  page.onAgeChange({detail:{value:age}});assert.equal(page.data.animalInfo.age,age);assert.equal(page.data.displayAge,label,'Age selection must only derive the display label')
 }
 const restored=loadPage('order-add-pet',{},api,wx);restored.onLoad({petId:7,draft:true})
 assert.equal(restored.data.animalInfo.age,'3Y9M');assert.equal(restored.data.displayAge,'3岁9个月','Draft restoration must derive the same display age')
}
async function orderPreviewAgeDisplayIsFormattedWithoutChangingFormValue(){
 const draft={petId:7,petInfo:{animalName:'Draft Pet',dictSexId:1},sampleInfo:{sampleTypeName:'蜡块'},deliveryInfo:{},otherInfo:{remark:''},animalInfo:{age:'3Y9M',animalWeight:''},laboratoryId:1,selectedTestItems:[{infoTestTypeId:393,testName:'病理照片',price:400}]}
 const request={get:async()=>[],post:async()=>({prices:{393:400},payableAmount:400})}
 const wx={getStorageSync:()=>draft,setStorageSync(){}}
 const app={globalData:{cache:{}}}
 const page=loadPage('order-preview',request,{},wx,app)
 await page.onLoad();await tick()
 assert.equal(page.data.formData.animalInfo.age,'3Y9M');assert.equal(page.data.formData.animalInfo.ageDisplay,'3岁9个月','Order preview must derive a Chinese display age without changing the form value')
}
async function rabiesOrderSubmitsWithChipNO(){
 let created
 const api={billing:{checkStatus:async()=>({})},orders:{create:async data=>{created=data;return {orderId:1,needPayment:false}}}}
 const wx={showModal:o=>o.success(),removeStorageSync(){},redirectTo(){}}
 const page=loadPage('order-preview',{},api,wx)
 page.setData({quote:{payableAmount:100},formData:{petInfo:{chipNO:'CHIP-1',birthday:'2023-01-01'},sampleInfo:{},deliveryInfo:{},animalInfo:{},otherInfo:{remark:'EXPRESS-1'},selectedTestItems:[{infoTestTypeId:1225}]}})
 await page.onSubmit()
 assert.equal(created.chipNO,'CHIP-1','Rabies submission must accept the backend chipNO field')
}
async function latestCouponQuoteWinsOutOfOrderResponses(){
 const pending={};let stored
 const request={post:async(url,body)=>new Promise(resolve=>{pending[body.userCouponId]=resolve})}
 const wx={setStorageSync:(key,value)=>{stored=JSON.parse(JSON.stringify(value))}}
 const page=loadPage('order-preview',request,{},wx)
 page.setData({formData:{laboratoryId:1,selectedTestItems:[{infoTestTypeId:393,price:400}]}})
 const oldRequest=page.requote('old'),newRequest=page.requote('new')
 pending.new({coupon:'new',prices:{393:180},payableAmount:180});await newRequest
 pending.old({coupon:'old',prices:{393:100},payableAmount:100});await oldRequest
 assert.equal(page.data.quote.coupon,'new');assert.equal(page.data.totalPrice,'180.00');assert.equal(page.data.formData.userCouponId,'new');assert.equal(stored.userCouponId,'new','A stale coupon response must not overwrite the latest quote')
}
async function nullTestPricesRemainUnselectable(){
 const toasts=[]
 const request={get:async()=>({items:[{infoTestTypeId:501,testName:'待维护项目',price:null}],total:1,size:20})}
 const page=loadPage('order-test-items',request,{}, {showToast:o=>toasts.push(o.title)})
 page.setData({laboratoryId:1});await page.loadTestItems()
 assert.equal(page.data.allItemsMap[501].price,null);assert.equal(page.data.filteredItems[0].price,'价格待维护','The render copy must show an unmaintained price instead of zero')
 page.onToggleItem({currentTarget:{dataset:{id:501}}});assert.equal(page.data.selectedCount,0);assert.equal(toasts[0],'该项目价格待维护，请联系实验室')
 await page.performSearch('待维护');assert.equal(page.data.allItemsMap[501].price,null);assert.equal(page.data.filteredItems[0].price,'价格待维护','Search results must preserve and display unmaintained prices')
 page.onClearSearch();assert.equal(page.data.filteredItems[0].price,'价格待维护','Clearing search must retain the unmaintained-price label')
}
async function selectedDraftRestoresDrawerAndTotal(){
 const draft={selectedTestItems:[{infoTestTypeId:501,testName:'项目一',price:100},{infoTestTypeId:502,testName:'项目二',price:200}]}
 const page=loadPage('order-test-items',{}, {}, {getStorageSync:()=>draft})
 page.setData({allItemsMap:{999:{infoTestTypeId:999,price:1}}});page.onShow()
 assert.equal(page.data.selectedCount,2);assert.equal(page.data.selectedItemsList.length,2);assert.equal(page.data.totalPrice,'300.00');assert.equal(page.data.allItemsMap[999].price,1,'Draft restoration must merge the existing catalog map')
}
async function petSelectionLoadFailureCanRetry(){
 let attempts=0
 const api={pets:{getList:async()=>{attempts++;if(attempts===1)throw new Error('offline');return {content:[{id:7,animalName:'Pet'}]}}}}
 const page=loadPage('order-select-pet',{},api)
 await page.loadPets();assert.ok(page.data.loadError,'The failed request must expose an inline error state')
 await page.loadPets();assert.equal(page.data.loadError,'');assert.equal(page.data.filteredPets.length,1,'Retry must reload pets and clear the error')
}
async function testItemsLoadFailureCanRetry(){
 let attempts=0
 const request={get:async()=>{attempts++;if(attempts===1)throw new Error('offline');return {items:[{infoTestTypeId:501,testName:'病理',price:100}],total:1,size:20}}}
 const page=loadPage('order-test-items',request,{})
 page.setData({laboratoryId:1});await page.loadTestItems();assert.ok(page.data.loadError,'The failed catalog request must expose an inline error state')
 await page.onRetryLoad();assert.equal(page.data.loadError,'');assert.equal(page.data.filteredItems.length,1,'Retry must reload the catalog and clear the error')
}
async function sampleTypePickerLoadsFiltersSelectsAndCaches(){
 let calls=0
 const samples=[{infoSampleTypeId:1,sampleName:'全血',sampleNameEn:'Whole blood'},{infoSampleTypeId:2,sampleName:'病理蜡块',sampleNameEn:'Wax block'},{infoSampleTypeId:3,sampleName:'转运拭子',sampleNameEn:'Transport swab'}]
 const api={dictionary:{getSampleTypes:async()=>{calls++;if(calls===1)throw new Error('offline');return samples}}}
 const page=loadPage('order-add-pet',{},api)
 await page.onSelectSampleType();assert.ok(page.data.samplePickerError,'The first picker load must expose its failure inline')
 await page.onRetrySamples();assert.equal(page.data.samplePickerVisible,true);assert.equal(page.data.sampleTypes.length,3)
 page.onSampleTabTap({currentTarget:{dataset:{index:2}}});assert.equal(page.data.sampleFilteredTypes.length,1);assert.equal(page.data.sampleFilteredTypes[0].infoSampleTypeId,2)
 page.onSampleSearchInput({detail:{value:'wax'}});assert.equal(page.data.sampleFilteredTypes.length,1)
 page.onSelectSample({currentTarget:{dataset:{sample:page.data.sampleFilteredTypes[0]}}})
 assert.equal(page.data.selectedSampleTypeName,'病理蜡块');assert.equal(page.data.sampleInfo.infoSampleTypeId,2);assert.equal(page.data.samplePickerVisible,false)
 await page.onSelectSampleType();assert.equal(calls,2,'Reopening the picker must reuse its cached catalog')
}
async function speciesAndBreedPickersWriteBackAndClose(){
 const toasts=[]
 const api={dictionary:{getAnimals:async()=>[{infoAnimalId:1,animalName:'犬',animalNameEn:'Dog'},{infoAnimalId:2,animalName:'猫',animalNameEn:'Cat'}],getVarieties:async()=>[{iid:11,varietyCn:'泰迪',varietyEn:'Teddy'}]}}
 const page=loadPage('order-add-pet',{},api,{showToast:o=>toasts.push(o.title)})
 page.breed();assert.equal(toasts[0],'请先选择物种');assert.equal(page.data.breedPickerVisible,false)
 await page.species();assert.equal(page.data.speciesPickerVisible,true);assert.equal(page.data.speciesOptions.length,2)
 page.onSpeciesSelect({detail:{item:page.data.speciesOptions[0]}});assert.equal(page.data.petInfo.infoAnimalId,1);assert.equal(page.data.petInfo.infoAnimalName,'犬');assert.equal(page.data.speciesPickerVisible,false)
 await page.breed();assert.equal(page.data.breedPickerVisible,true);assert.equal(page.data.breedOptions.length,1)
 page.onBreedSelect({detail:{item:page.data.breedOptions[0]}});assert.equal(page.data.petInfo.infoVarietyId,11);assert.equal(page.data.petInfo.infoVarietyName,'泰迪');assert.equal(page.data.petInfo.infoVarietyNameEn,'Teddy');assert.equal(page.data.breedPickerVisible,false)
}
async function petAddBreedPickerUsesPopupSelector(){
 const api={dictionary:{getVarieties:async()=>[{iid:11,varietyCn:'泰迪',varietyEn:'Teddy'}]}}
 const page=loadPage('pet-add',{},api,{setStorageSync(){}})
 page.setData({'formData.infoAnimalId':1,selectedAnimalName:'犬'});await page.onSelectBreed()
 assert.equal(page.data.showBreedSelector,true);assert.equal(page.data.breedList.length,1)
 page.onBreedSelect({detail:{item:page.data.breedList[0]}});assert.equal(page.data.selectedBreed.varietyCn,'泰迪');assert.equal(page.data.formData.infoVarietyId,11);assert.equal(page.data.showBreedSelector,false)
}
async function popupSelectorFiltersChineseAndEnglish(){
 let definition
 const context={console,setTimeout(fn){fn();return 1},clearTimeout(){},Component:value=>definition=value}
 vm.runInNewContext(fs.readFileSync(path.join(root,'components/popup-selector/popup-selector.js'),'utf8'),context)
 const items=[{iid:1,varietyCn:'泰迪',varietyEn:'Teddy'},{iid:2,varietyCn:'金毛',varietyEn:'Golden Retriever'}]
 const component={data:JSON.parse(JSON.stringify(definition.data)),properties:{items,itemIdKey:'iid',itemNameKey:'varietyCn',itemNameEnKey:'varietyEn'},...definition.methods,setData(update){Object.assign(this.data,update)}}
 definition.observers.items.call(component,items)
 component.onSearch({detail:{value:'teddy'}});await tick();assert.equal(component.data.filteredItems.length,1);assert.equal(component.data.filteredItems[0].iid,1)
 component.onSearch({detail:{value:'金毛'}});await tick();assert.equal(component.data.filteredItems[0].iid,2)
}
;(async()=>{await devMockPaymentOnlyCompletesLocally();await reportPagingAndRetry();await homeRefreshesIdentityAndMessages();await homeLoadsRealDashboard();await homeFailuresDoNotBecomeZeroCounts();await homeOnlyLatestModuleResponseApplies();await homeQualificationAndOrderStatusViewMapping();await mineLoadsRealBalanceAndHandlesFailure();await agePickerRespondsToAsyncPetDetails();await orderAgeDisplayIsFormattedWithoutChangingFormValue();await orderPreviewAgeDisplayIsFormattedWithoutChangingFormValue();await rabiesOrderSubmitsWithChipNO();await latestCouponQuoteWinsOutOfOrderResponses();await nullTestPricesRemainUnselectable();await selectedDraftRestoresDrawerAndTotal();await petSelectionLoadFailureCanRetry();await testItemsLoadFailureCanRetry();await sampleTypePickerLoadsFiltersSelectsAndCaches();await speciesAndBreedPickersWriteBackAndClose();await petAddBreedPickerUsesPopupSelector();await popupSelectorFiltersChineseAndEnglish();await boundedPrivateDiagnostics();await updatesRequireUserConsent();await notificationDeliveryRetry();await invoiceProfileEditing();await orderFilters();await staleOrderResponses();await mockRechargeCannotPayOrCredit();await expiredSessionReturnsToLogin();await reportDownloadsStayOnOwnBackend();console.log('30 native client flow checks passed: local-only mock payment, report paging and retry, home refresh, home dashboard data, home failure states, home stale responses, home qualification and order status mapping, mine balance and failure state, async age picker, order age display, preview age display, rabies chip field, coupon quote ordering, null test prices, selected draft restoration, pet selection load retry, test items load retry, sample type picker, species and breed pickers, pet-add breed picker, popup selector search, bounded private diagnostics, update consent, notification delivery retry, invoice profile editing, filters, stale responses, mock recharge, session expiry, protected report download')})().catch(error=>{console.error(error);process.exitCode=1})
