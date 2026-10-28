const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')
const { root, loadPage, loadApp, freshRequire } = require('./helpers')

const originalRoutes = [
  'login/login','register/register','review/review','home/home','network-test/network-test','pets/pets','orders/orders',
  'notifications/notifications','mine/mine','order-select-pet/order-select-pet','order-add-pet/order-add-pet',
  'order-test-items/order-test-items','order-preview/order-preview','order-detail/order-detail','pet-add/pet-add',
  'breed-select/breed-select','sample-type-select/sample-type-select','species-select/species-select','pet-detail/pet-detail',
  'pet-health/pet-health','report-list/report-list','report-view/report-view','pdf-view/pdf-view','icon-download/icon-download',
  'icon-export/icon-export','error-page/error-page','account/account','transactions/transactions','coupons/coupons',
  'preferences/preferences','mall/mall','invoices/invoices','articles/articles','article-detail/article-detail','profile/profile',
  'drafts/drafts','recharge/recharge','security/security'
]

test('确认版38路由和5个Tab完整保留', () => {
  const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json')))
  assert.equal(originalRoutes.length, 38)
  originalRoutes.forEach(route => assert.ok(app.pages.includes(`pages/${route}`), route))
  assert.equal(app.tabBar.list.length, 5)
  assert.deepEqual(app.tabBar.list.map(x => x.text), ['首页','检测','报告','消息','我的'])
  assert.deepEqual(freshRequire('config/routes.js'), app.pages)
})

test('环境地址不含局域网硬编码且保留/api和存储契约', () => {
  global.wx = { getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }), getExtConfigSync: () => ({}) }
  const config = freshRequire('config/config.js')
  assert.equal(config.baseURL, 'http://127.0.0.1:8082/api')
  assert.equal(config.storageKeys.TOKEN, 'token')
  const sources = ['config/config.js','pages/network-test/network-test.js'].map(x => fs.readFileSync(path.join(root,x),'utf8')).join('\n')
  assert.doesNotMatch(sources, /192\.168\./)
})

test('refresh与logout都携带Bearer认证头', async () => {
  const calls = []
  const request = { post(url, data, needAuth) { calls.push({url,data,needAuth}); return Promise.resolve({}) } }
  const originalLoad = Module._load
  Module._load = function(name, parent, main) { if (name.endsWith('utils/request.js')) return request; return originalLoad.call(this,name,parent,main) }
  try {
    const api = freshRequire('api/index.js')
    await api.auth.refreshToken('old')
    await api.auth.logout()
  } finally { Module._load = originalLoad }
  assert.deepEqual(calls.map(x => x.needAuth), [true, true])
  assert.equal(calls[0].data.token, 'old')
})

test('病史页按petId加载真实病史和报告并支持空态', async () => {
  const urls = []
  const { instance } = loadPage('pet-health/pet-health', { get(url) {
    urls.push(url)
    return Promise.resolve({ petName: '咪咪', medicalHistory: [{orderId:3,medicalHistory:'过敏'}], reports: {items:[],total:0,page:0,size:20} })
  } })
  await instance.onLoad({petId:'7'})
  assert.deepEqual(urls, ['/pets/7/health'])
  assert.equal(instance.data.selectedPet, '咪咪')
  assert.equal(instance.data.medicalRecords[0].diagnosis, '过敏')
  assert.deepEqual(instance.data.reports, [])
  assert.equal(fs.readFileSync(path.join(root,'pages/pet-health/pet-health.js'),'utf8').includes('猫三联'), false)
})

test('优惠券详情展示门槛实验室有效期状态', async () => {
  const { instance } = loadPage('coupons/coupons', { get: () => Promise.resolve([]) })
  instance.showDetail({currentTarget:{dataset:{id:4}}})
  instance.setData({items:[{id:4,name:'检测券',minimumAmount:100,laboratoryName:'中心实验室',startsAt:'2026-01-01',expiresAt:'2026-12-31',status:'available'}]})
  instance.showDetail({currentTarget:{dataset:{id:4}}})
  assert.equal(instance.data.detailVisible, true)
  assert.equal(instance.data.selectedCoupon.laboratoryName, '中心实验室')
  const wxml = fs.readFileSync(path.join(root,'pages/coupons/coupons.wxml'),'utf8')
  assert.match(wxml, /closeDetail/)
  assert.match(wxml, /使用门槛/)
})

test('月结账单API使用0基分页与真实对端点', async () => {
  const calls = []
  const request = { get(url,params){calls.push(['get',url,params]);return Promise.resolve({})}, post(url,data){calls.push(['post',url,data]);return Promise.resolve({})} }
  const originalLoad = Module._load
  Module._load = function(name,parent,main){if(name.endsWith('utils/request.js'))return request;return originalLoad.call(this,name,parent,main)}
  try { const api=freshRequire('api/index.js'); await api.bills.getList({page:0,size:20,arrears:true}); await api.bills.getDetail(8); await api.bills.reconcile(8,'CONFIRM','') } finally {Module._load=originalLoad}
  assert.deepEqual(calls[0],['get','/bills',{page:0,size:20,arrears:true}])
  assert.equal(calls[1][1],'/bills/8')
  assert.deepEqual(calls[2],['post','/bills/8/reconciliation',{action:'CONFIRM',reason:''}])
})

test('医生不显示账单入口且直接访问不会请求账单', async () => {
  const { instance } = loadPage('bills/bills', { get(){throw new Error('DOCTOR不应发起请求')} }, {roles:['DOCTOR']})
  await instance.onLoad()
  assert.equal(instance.data.forbidden, true)
  assert.equal(instance.data.items.length, 0)
  const mine = loadPage('mine/mine', {get(url){if(url==='/user/profile')return Promise.resolve({id:1,roles:['DOCTOR']});return Promise.resolve({})}}, {roles:['DOCTOR']})
  await mine.instance.loadUserInfo()
  assert.equal(mine.instance.data.canViewBills, false)
})

test('医院管理员可分页加载账单及欠款', async () => {
  const queries=[]
  const {instance}=loadPage('bills/bills',{get(url,params){queries.push({url,params});return Promise.resolve({items:[{id:1,billNo:'B1',dueAmount:100,outstandingAmount:100}],total:1,page:0,size:20})}},{roles:['HOSPITAL_ADMIN']})
  await instance.onLoad()
  await instance.showArrears()
  assert.equal(instance.data.items[0].id,1)
  assert.equal(queries[1].params.arrears,true)
  assert.equal(queries[1].params.page,0)
})

test('资料无角色时从真实角色接口判定医院管理员', async () => {
  const urls=[]
  const {instance}=loadPage('bills/bills',{get(url){urls.push(url);if(url==='/user/roles')return Promise.resolve(['HOSPITAL_ADMIN']);return Promise.resolve({items:[],total:0,page:0,size:20})}}, {})
  await instance.onLoad()
  assert.deepEqual(urls,['/user/roles','/bills'])
  assert.equal(instance.data.forbidden,false)
})

test('物流适配保留时间线预计到达和异常', () => {
  const logistics=freshRequire('utils/logistics.js')
  const result=logistics.adaptShipment({trackingNo:'SF1',status:'IN_TRANSIT',expectedArrivedAt:'2026-10-30T10:00:00',exceptionType:'DELAY',exceptionNote:'天气延误',timeline:[{action:'PICKED_UP',reason:'已揽收',createdAt:'2026-10-28T08:00:00'}]})
  assert.equal(result.trackingNo,'SF1')
  assert.equal(result.timeline[0].title,'已揽收')
  assert.match(result.expectedArrivedAt,/2026-10-30/)
  assert.equal(result.exceptionNote,'天气延误')
})

test('订单详情物流失败不覆盖订单主流程', async () => {
  const order={orderId:9,userNO:'A9',sampleStatus:2,items:[]}
  const {instance}=loadPage('order-detail/order-detail',{get(url){if(url==='/orders/9')return Promise.resolve(order);if(url==='/logistics/orders/9')return Promise.reject(new Error('请求的资源不存在'));throw new Error(url)}})
  await instance.loadOrderDetail(9)
  assert.equal(instance.data.orderData.orderId,9)
  assert.equal(instance.data.error,false)
  assert.deepEqual(instance.data.shipmentTimeline,[])
})

test('mine退出先调用服务端登出再清本地', async () => {
  const {instance,storage,wx}=loadPage('mine/mine')
  let resolveModal
  wx.showModal=options=>{options.success({confirm:true});resolveModal=true}
  instance.onLogout()
  await new Promise(resolve=>setImmediate(resolve))
  assert.equal(resolveModal,true)
  assert.equal(storage.token,null)
})

test('请求工具为认证请求生成Bearer头且401仍清理会话', () => {
  const storage={token:'abc',userInfo:{id:1},hospitalId:2}
  global.wx={getStorageSync:key=>storage[key],removeStorageSync:key=>delete storage[key],reLaunch:()=>{}}
  global.getApp=()=>({globalData:{token:'abc'}})
  global.getCurrentPages=()=>[{route:'pages/home/home'}]
  const request=freshRequire('utils/request.js')
  assert.equal(request.getHeaders(true).Authorization,'Bearer abc')
  assert.throws(()=>request.handleResponse({statusCode:401,data:{}}),/登录已过期/)
  assert.equal(storage.token,undefined)
})

test('只有开发环境允许mock支付且保留支付轮询和草稿模块', () => {
  global.wx={getAccountInfoSync:()=>({miniProgram:{envVersion:'release'}}),getExtConfigSync:()=>({baseURL:'https://example.com/api'})}
  const config=freshRequire('config/config.js')
  assert.equal(config.allowMockPayment,false)
  assert.match(fs.readFileSync(path.join(root,'utils/payment.js'),'utf8'),/payment\/status/)
  assert.ok(fs.existsSync(path.join(root,'utils/drafts.js')))
})

test('宠物详情可进入对应宠物健康档案', () => {
  const {instance,wx}=loadPage('pet-detail/pet-detail')
  instance.setData({petData:{id:7}})
  let url
  wx.navigateTo=options=>{url=options.url}
  instance.onViewHealth()
  assert.match(url,/petId=7/)
})

test('账单明细超过100条时续页加载完整才允许确认对账', async () => {
  const pages=[]
  const {instance,wx}=loadPage('bill-detail/bill-detail',{get(url,params){
    if(url==='/user/roles')return Promise.resolve(['HOSPITAL_ADMIN'])
    if(url==='/bills/1')return Promise.resolve({id:1,status:'PENDING_RECONCILIATION'})
    pages.push(params.page)
    return Promise.resolve({items:Array.from({length:params.page===0?100:1},(_,i)=>({id:params.page*100+i+1})),total:101,page:params.page,size:100})
  }},{roles:['HOSPITAL_ADMIN']})
  await instance.onLoad({id:'1'})
  assert.equal(instance.data.items.length,101)
  assert.deepEqual(pages,[0,1])
  assert.equal(instance.data.canReconcile,true)
  assert.ok(wx)
})

test('明细未加载完整时确认对账不发服务端请求', async () => {
  let posted=false
  const {instance,wx}=loadPage('bill-detail/bill-detail',{get(url,params){if(url==='/bills/1/items'){if(params.page===1)return Promise.reject(new Error('第二页失败'));return Promise.resolve({items:[{id:1}],total:2,page:0,size:100})}return Promise.resolve({id:1,status:'PENDING_RECONCILIATION'})},post(){posted=true}},{roles:['HOSPITAL_ADMIN']})
  instance.billId='1'
  await instance.load()
  let modal=false
  wx.showModal=()=>{modal=true}
  instance.confirm()
  assert.equal(posted,false)
  assert.equal(modal,false)
  assert.equal(instance.data.canReconcile,false)
})

test('欠款列表onReachBottom加载后续页', async () => {
  const pages=[]
  const {instance}=loadPage('bill-arrears/bill-arrears',{get(url,params){pages.push(params.page);return Promise.resolve({items:Array.from({length:params.page===0?100:1},(_,i)=>({id:params.page*100+i+1})),total:101,page:params.page,size:100})}},{roles:['HOSPITAL_ADMIN']})
  await instance.onLoad()
  await instance.onReachBottom()
  assert.equal(instance.data.items.length,101)
  assert.deepEqual(pages,[0,1])
})

test('角色接口失败不阻塞account旧导航与账户数据', async () => {
  const {instance}=loadPage('account/account',{get(url){if(url==='/user/roles')return Promise.reject(new Error('角色失败'));return Promise.resolve({balance:10,points:2})}}, {})
  await instance.onShow()
  assert.equal(instance.data.account.balance,10)
  assert.ok(instance.data.links.some(x=>x.page==='security'))
  assert.equal(instance.data.links.some(x=>x.page==='bills'),false)
})

test('账单页角色失败显示可读错误且不请求账单', async () => {
  const urls=[]
  const {instance}=loadPage('bills/bills',{get(url){urls.push(url);return Promise.reject(new Error('角色失败'))}}, {})
  await instance.onLoad()
  assert.match(instance.data.error,/角色信息加载失败/)
  await instance.retryLoad()
  assert.deepEqual(urls,['/user/roles','/user/roles'])
})

test('服务端登出成功后才清理本地会话', async () => {
  const events=[]
  const app=loadApp(()=>Promise.resolve({}),events)
  await app.logout()
  assert.deepEqual(events.map(x=>x[0]),['post','remove','remove','remove','remove','reLaunch'])
})

test('服务端登出失败保留Token并允许重试', async () => {
  const events=[]
  const app=loadApp(()=>Promise.reject(new Error('网络失败')),events)
  await assert.rejects(app.logout(),/网络失败/)
  assert.equal(app.globalData.token,'token')
  assert.deepEqual(events.map(x=>x[0]),['post'])
})

test('mine登出失败不提示成功且保留会话', async () => {
  const {instance,app,storage,wx}=loadPage('mine/mine')
  app.logout=()=>Promise.reject(new Error('网络失败'))
  wx.showModal=options=>options.success({confirm:true})
  const titles=[]
  wx.showToast=options=>titles.push(options.title)
  instance.onLogout()
  await new Promise(resolve=>setImmediate(resolve))
  assert.deepEqual(titles,['退出失败，请重试'])
  assert.equal(storage.token,'stored-token')
})

test('所有账单状态显示中文badge而不暴露英文枚举', () => {
  const {adaptBill}=freshRequire('utils/bills.js')
  const expected={PENDING_RECONCILIATION:'待对账',DISPUTED:'有异议',RECONCILED:'已对账',SETTLED:'已结算',OVERDUE:'已逾期'}
  for(const [status,label] of Object.entries(expected))assert.equal(adaptBill({status}).statusLabel,label)
  for(const page of ['bills/bills','bill-detail/bill-detail','bill-arrears/bill-arrears']){
    const wxml=fs.readFileSync(path.join(root,'pages',page+'.wxml'),'utf8')
    assert.doesNotMatch(wxml,/\{\{(?:item|bill)\.status\}\}/)
    assert.match(wxml,/statusLabel/)
  }
})

test('所有页面WXML绑定事件都有Page方法', () => {
  const app=JSON.parse(fs.readFileSync(path.join(root,'app.json')))
  for(const route of app.pages){
    const base=path.join(root,route)
    const wxml=fs.readFileSync(`${base}.wxml`,'utf8')
    const js=fs.readFileSync(`${base}.js`,'utf8')
    for(const match of wxml.matchAll(/(?:bind|catch)(?:tap|input|change|submit|scrolltolower|confirm|close)="([A-Za-z_$][\w$]*)"/g)){
      assert.ok(new RegExp(`(?:^|[\\s,{])${match[1]}\\s*\\(`).test(js)||new RegExp(`${match[1]}\\s*:`).test(js),`${route}缺少${match[1]}`)
    }
  }
})
