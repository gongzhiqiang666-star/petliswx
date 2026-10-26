// Real WeChat simulator smoke test. Uses only the synthetic local fixture.
const fs=require('fs'),path=require('path'),assert=require('assert/strict')
const privateRoot=path.join(process.env.HOME,'.local/share/petlis')
const automator=require(path.join(privateRoot,'tools/miniprogram-qa/node_modules/miniprogram-automator'))
const fixture=JSON.parse(fs.readFileSync(path.join(privateRoot,'simulator-fixture.json'),'utf8'))
assert.match(fixture.account,/^petlis_qa_[a-f0-9]+$/)
;(async()=>{
 const mini=await automator.connect({wsEndpoint:process.env.PETLIS_AUTOMATOR_ENDPOINT || 'ws://127.0.0.1:9420'})
 const visiblePause=()=>new Promise(resolve=>setTimeout(resolve,2000))
 // Navigation can destroy page contexts and make the automation channel briefly
 // unresponsive. Treat those transient failures as retryable instead of failing the run.
 const transientError=/timeout waiting for automator response|Connection closed|timed out|WebSocket/i
 async function again(fn,attempts=8,delay=800){
  let error
  for(let i=0;i<attempts;i++){try{return await fn()}catch(e){error=e;if(!transientError.test(String(e&&e.message)))throw e;await new Promise(resolve=>setTimeout(resolve,delay))}}
  throw error
 }
 const currentPage=()=>again(()=>mini.currentPage())
 const pageData=key=>again(async()=>(await mini.currentPage()).data(key))
 try{
  console.log('Automation connected')
  await again(()=>mini.reLaunch('/pages/login/login'))
  let page
  for(let i=0;i<30;i++){page=await mini.currentPage();if(page.path==='pages/login/login')break;await new Promise(resolve=>setTimeout(resolve,250))}
  assert.equal(page.path,'pages/login/login','Start with the logged-out login page')
  console.log('Login page: '+page.path)
  await page.waitFor('input[data-field="account"]')
  console.log('Account input located')
  await (await page.$('input[data-field="account"]')).trigger('input',{value:fixture.account})
  await (await page.$('input[data-field="password"]')).trigger('input',{value:fixture.password})
  console.log('Input events sent')
  await (await page.$('.row')).tap() // Exercise the bound UI event; do not persist QA sessions.
  console.log('Remember control clicked')
  for(let i=0;i<40 && (await page.data('remember'))!==false;i++)await new Promise(resolve=>setTimeout(resolve,200))
  assert.equal(await page.data('remember'),false)
  console.log('Remember state verified');await visiblePause()
  for(let attempt=0;attempt<3;attempt++){
   await (await page.$('button[type="primary"]')).tap()
   for(let i=0;i<30;i++){
    page=await mini.currentPage();if(page.path==='pages/home/home')break
    await new Promise(resolve=>setTimeout(resolve,250))
   }
   if(page.path==='pages/home/home')break
   if(attempt===2)throw new Error('Login tap did not navigate to home')
  }
  console.log('Post-login page: '+page.path)
  assert.equal(page.path,'pages/home/home','Synthetic doctor login must navigate to home')
  const shots=path.join(privateRoot,'qa');fs.mkdirSync(shots,{recursive:true,mode:0o700})
  await mini.screenshot({path:path.join(shots,'home.png')})
  for(const name of (process.env.PETLIS_QA_SCOPE==='draft'?[]:['account','notifications','invoices','drafts','coupons'])){
   page=name==='notifications'?await mini.switchTab('/pages/'+name+'/'+name):await mini.navigateTo('/pages/'+name+'/'+name)
   for(let i=0;i<30;i++){if(!(await page.data('busy')) && !(await page.data('loading')))break;await new Promise(resolve=>setTimeout(resolve,250))}
   assert.equal((await page.data('error'))||'','',name+' must load without API error')
   assert.ok(!(await page.data('busy')) && !(await page.data('loading')),name+' must finish loading')
   if(name==='account')assert.ok(await page.data('account'), 'Account response must be populated')
   if(name==='invoices'){
    const tapText=async text=>{for(const button of await page.$$('button'))if((await button.text()).trim()===text){await button.tap();return}throw new Error('Button missing: '+text)}
    const fill=async(field,value)=>{let input;for(let i=0;i<40;i++){input=await page.$('input[data-field="'+field+'"]');if(input)break;await new Promise(resolve=>setTimeout(resolve,200))}assert.ok(input,'Input missing: '+field);await input.trigger('input',{value})}
    const waitSaved=async()=>{for(let i=0;i<40;i++){if(!(await page.data('busy')) && !(await page.data('adding')))break;await new Promise(resolve=>setTimeout(resolve,200))}assert.equal(await page.data('error'),'');assert.equal(await page.data('adding'),false)}
    await tapText('添加开票信息')
    for(let i=0;i<40 && !(await page.data('adding'));i++)await new Promise(resolve=>setTimeout(resolve,200))
    assert.equal(await page.data('adding'),true)
    await visiblePause()
    await fill('title','Simulator invoice');await fill('email','simulator@example.test');await fill('bankAccount','0012345')
    await visiblePause();await tapText('保存开票信息');await waitSaved()
    let profiles=await page.data('profiles');assert.equal(profiles.length,1);assert.equal(profiles[0].bankAccount,'0012345')
    const id=profiles[0].id
    await tapText('编辑当前抬头');await fill('title','Simulator invoice edited');await visiblePause();await tapText('保存开票信息');await waitSaved()
    profiles=await page.data('profiles');assert.equal(profiles[0].id,id);assert.equal(profiles[0].title,'Simulator invoice edited');assert.equal(profiles[0].bankAccount,'0012345')
    console.log('Simulator invoice profile create/edit/save passed')
   }
   await mini.screenshot({path:path.join(shots,name+'.png')})
   console.log('Simulator page loaded: '+name);await visiblePause()
   if(name==='notifications')await mini.switchTab('/pages/home/home');else await mini.navigateBack()
  }
  page=await mini.navigateTo('/pages/order-add-pet/order-add-pet?petId='+fixture.petId)
  for(let i=0;i<40 && !(await page.data('petInfo'));i++)await new Promise(resolve=>setTimeout(resolve,200))
  assert.ok(await page.data('petInfo'),'Pet detail must load');console.log('Send-test pet detail loaded');await mini.screenshot({path:path.join(shots,'order-form.png')})
  await (await page.$('input[placeholder="请输入采样部位"]')).trigger('input',{value:'Simulator blood sample'})
  await mini.pageScrollTo(800)
  await (await page.$('input[placeholder="请输入送检医师姓名"]')).trigger('input',{value:'Simulator Doctor'})
  await visiblePause();console.log('Send-test partial fields entered')
  await mini.pageScrollTo(0)
  for(const button of await page.$$('button'))if((await button.text()).trim()==='保存为草稿'){await button.tap();break}
  let saved
  for(let i=0;i<40;i++){saved=await mini.evaluate(()=>wx.getStorageSync('order_form'));if(saved && saved._draftId)break;await new Promise(resolve=>setTimeout(resolve,200))}
  assert.ok(saved && saved._draftId,'Save draft must return a persisted draft ID')
  console.log('Draft persistence confirmed')
  await mini.navigateBack();console.log('Returned from send-test form')
  page=await mini.navigateTo('/pages/drafts/drafts');console.log('Draft list opened')
  for(let i=0;i<40;i++){if(!(await pageData('busy')) && (await pageData('items')).length)break;await new Promise(resolve=>setTimeout(resolve,200))}
  assert.equal((await pageData('items')).length,1)
  await visiblePause()
  // Navigation destroys the tapped element before some tool versions acknowledge it.
  // Verify the destination independently rather than waiting for that stale element.
  page=await currentPage()
  const resumeButton=await again(()=>page.$('button[type="primary"]'))
  assert.ok(resumeButton,'Resume button missing on draft list')
  const resumeTap=resumeButton.tap().then(()=>null,error=>error)
  await Promise.race([resumeTap,new Promise(resolve=>setTimeout(resolve,1500))])
  for(let i=0;i<40;i++){page=await currentPage();if(page.path==='pages/order-add-pet/order-add-pet')break;await new Promise(resolve=>setTimeout(resolve,200))}
  assert.equal(page.path,'pages/order-add-pet/order-add-pet')
  // The restored form fills asynchronously; poll the restored values instead of single reads.
  let restoredArea='',restoredDoctor=''
  for(let i=0;i<40;i++){
   restoredArea=await pageData('sampleInfo.sampleArea');restoredDoctor=await pageData('deliveryInfo.doctorName')
   if(restoredArea==='Simulator blood sample' && restoredDoctor==='Simulator Doctor')break
   await new Promise(resolve=>setTimeout(resolve,250))
  }
  assert.equal(restoredArea,'Simulator blood sample')
  assert.equal(restoredDoctor,'Simulator Doctor')
  await again(()=>mini.screenshot({path:path.join(shots,'restored-order-draft.png')}));await visiblePause()
  console.log('Simulator partial send-test form saved and restored from persisted draft')
  console.log('Selected simulator checks passed; no real payment')
 }catch(error){
  const failed=path.join(privateRoot,'qa','failed.png');await mini.screenshot({path:failed});console.log('Failure screenshot: '+failed);throw error
 }finally{
  await mini.evaluate(()=>getApp().clearSession())
  await mini.disconnect()
 }
})().catch(error=>{console.error(error.message);process.exitCode=1})
