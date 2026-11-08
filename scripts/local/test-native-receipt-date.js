// Existing simulator, normal password flow and actual page load. No request mocks or app relaunch.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');
const originalPath=root+'/simulator-fixture.json',original=JSON.parse(fs.readFileSync(originalPath));
const doctor=JSON.parse(fs.readFileSync(root+'/qa/native-sf-history-private.json')),proof=JSON.parse(fs.readFileSync(root+'/qa/receipt-date-php.json'));
const deadline=setTimeout(()=>{console.error('Receipt UI RPC deadline exceeded');process.exit(2)},90000);
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Receipt UI readiness timeout')};
 const login=async account=>{if(await m.evaluate(()=>getCurrentPages().slice(-1)[0].route)!=='pages/login/login')await m.evaluate(async()=>{await getApp().logout();return true});await wait(()=>m.evaluate(()=>getCurrentPages().slice(-1)[0].route==='pages/login/login'));await m.evaluate(async c=>{const p=getCurrentPages().slice(-1)[0];for(const field of ['account','password'])p.field({currentTarget:{dataset:{field}},detail:{value:c[field]}});await p.submit();return true},{account:account.account,password:account.password});await wait(()=>m.evaluate(id=>{const u=getApp().globalData.userInfo||{};return Number(u.userId||u.id)===Number(id)&&getCurrentPages().slice(-1)[0].route==='pages/home/home'},account.userId))};
 try {
  assert.equal(await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId),JSON.parse(fs.readFileSync('miniprogram/project.config.json')).appid);
  await login(doctor);
  await m.evaluate(url=>new Promise((resolve,reject)=>wx.navigateTo({url,success:()=>resolve(true),fail:reject})),'/pages/order-detail/order-detail?id='+proof.orderId);
  await wait(()=>m.evaluate(()=>{const d=getCurrentPages().slice(-1)[0].data;return !d.loading&&!!d.shipment}));
  const data=await m.evaluate(()=>{const d=getCurrentPages().slice(-1)[0].data;return {orderId:d.orderData.orderId,receiveSampleDt:d.orderData.receiveSampleDt,shipmentId:d.shipment.id,shipmentStatus:d.shipment.status,receipt:d.timeline.find(x=>x.title==='收到样本')}});
  assert.equal(Number(data.orderId),proof.orderId);assert.equal(Number(data.shipmentId),proof.shipmentId);assert.equal(data.shipmentStatus,'RECEIVED');assert.equal(data.receiveSampleDt,proof.phpReceiveSampleDt.slice(0,10));assert.equal(data.receipt.completed,true);assert.equal(data.receipt.pending,false);assert.equal(data.receipt.time,data.receiveSampleDt);
  await m.screenshot({path:root+'/qa/native-receipt-date.png'});
  fs.writeFileSync(root+'/qa/native-receipt-date.json',JSON.stringify({...data,noNetworkMocks:true,loginViaActualPageHandlers:true},null,2),{mode:0o600});
  console.log(JSON.stringify({orderId:proof.orderId,receiptDateAndCompletedTimelineVerified:true}));
 }finally{try{await login(original);original.session=await m.evaluate(()=>({...getApp().globalData.userInfo,token:getApp().globalData.token}));fs.writeFileSync(originalPath,JSON.stringify(original,null,2),{mode:0o600})}finally{await m.disconnect();clearTimeout(deadline)}}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
