// Five-tab navigation and Mine entries acceptance. Uses only the synthetic local fixture.
const fs=require('fs'),path=require('path'),assert=require('assert/strict')
const privateRoot=path.join(process.env.HOME,'.local/share/petlis')
const automator=require(path.join(privateRoot,'tools/miniprogram-qa/node_modules/miniprogram-automator'))
const fixture=JSON.parse(fs.readFileSync(path.join(privateRoot,'simulator-fixture.json'),'utf8'))
assert.match(fixture.account,/^petlis_qa_[a-f0-9]+$/)
;(async()=>{
 let mini
 const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms))
 const transientError=/timeout waiting for automator response|Connection closed|timed out|WebSocket|destroyed|stale/i
 async function again(fn,attempts=8,delay=800){let error;for(let i=0;i<attempts;i++){try{return await fn()}catch(e){error=e;if(!transientError.test(String(e&&e.message)))throw e;await sleep(delay)}}throw error}
 const currentPage=()=>again(()=>mini.currentPage())
 const pageData=key=>again(async()=>(await mini.currentPage()).data(key))
 const shots=path.join(privateRoot,'qa');fs.mkdirSync(shots,{recursive:true,mode:0o700})
 const shot=name=>again(()=>mini.screenshot({path:path.join(shots,'tabs-'+name+'.png')}))
 async function waitFor(check,label,timeout=20000){const end=Date.now()+timeout;let value,lastError;while(Date.now()<end){try{value=await check();if(value)return value}catch(e){lastError=e;if(!transientError.test(String(e&&e.message)))throw e}await sleep(1000)}throw new Error('Timed out: '+label+(lastError?'; '+lastError.message:''))}
 async function waitPath(expected){return waitFor(async()=>{const page=await currentPage();return page.path===expected?page:null},'page '+expected)}
 async function switchTab(url){
  const expected=url.replace(/^\//,'')
  for(let attempt=0;attempt<3;attempt++){
   await again(()=>mini.switchTab(url))
   try{return await waitFor(async()=>{const page=await currentPage();return page.path===expected?page:null},'page '+expected,8000)}
   catch(error){const page=await currentPage();console.log('switchTab '+url+' landed on '+page.path+' (attempt '+(attempt+1)+')');if(attempt===2)throw error}
  }
 }
 async function tapEntry(text){await again(async()=>{const page=await currentPage();for(const entry of await page.$$('.quick-entry')){if(((await entry.text())||'').includes(text)){await entry.tap();return}}throw new Error('Entry missing: '+text)})}
 try{
  mini=await automator.connect({wsEndpoint:process.env.PETLIS_AUTOMATOR_ENDPOINT||'ws://127.0.0.1:9420'})
  console.log('Automation connected')
  await again(()=>mini.reLaunch('/pages/login/login'))
  let page=await waitPath('pages/login/login')
  await again(()=>page.waitFor('input[data-field="account"]'))
  await again(async()=>{(await page.$('input[data-field="account"]')).trigger('input',{value:fixture.account})})
  await again(async()=>{(await page.$('input[data-field="password"]')).trigger('input',{value:fixture.password})})
  for(let attempt=0;attempt<3;attempt++){
   await again(async()=>{(await page.$('button[type="primary"]')).tap()})
   try{await waitFor(async()=>{const p=await currentPage();return p.path==='pages/home/home'?p:null},'login home',8000);break}
   catch(error){if(attempt===2)throw error;console.log('Login tap '+(attempt+1)+' did not navigate, retrying')}
  }
  console.log('Login passed')
  const tabs=[
   ['home','pages/home/home',null],
   ['test','pages/order-select-pet/order-select-pet','loading'],
   ['report','pages/report-list/report-list','loading'],
   ['message','pages/notifications/notifications','busy'],
   ['mine','pages/mine/mine',null]
  ]
  for(const [name,path,busyKey] of tabs){
   await switchTab('/'+path)
   if(busyKey)await waitFor(async()=>(await pageData(busyKey))===false,name+' loaded')
   await sleep(1500);await shot(name);console.log('Tab loaded and captured: '+name)
  }
  await tapEntry('宠物档案');await waitPath('pages/pets/pets');await sleep(1500);await shot('mine-pets')
  await again(()=>mini.navigateBack());await waitPath('pages/mine/mine');console.log('Mine -> pets entry passed')
  await tapEntry('送检记录');await waitPath('pages/orders/orders');await sleep(1500);await shot('mine-orders')
  await again(()=>mini.navigateBack());await waitPath('pages/mine/mine');console.log('Mine -> orders entry passed')
  console.log('FIVE TAB NAVIGATION PASSED')
 }catch(error){
  console.error('TABS FAILED: '+error.stack)
  if(mini){try{await again(()=>mini.screenshot({path:path.join(shots,'tabs-failed.png')}))}catch(e){console.error('Failure screenshot unavailable: '+e.message)}}
  throw error
 }finally{
  if(mini){try{await again(()=>mini.evaluate(()=>getApp().clearSession()))}catch(e){console.error('Session cleanup warning: '+e.message)}try{await mini.disconnect()}catch(e){console.error('Disconnect warning: '+e.message)}}
 }
})().catch(()=>{process.exitCode=1})
