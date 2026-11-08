// First-party anonymous event counts. Never pass identities, IDs, errors, URLs or clinical content.
const config = require('../config/config')
const KEY = 'petlis_events_v1'
const PREFERENCE_KEY = 'petlis_analytics_enabled_v1'
const rules = {
  order_step: {steps:['pet','tests','sample','confirm'],outcomes:['view']},
  order_submit: {steps:['confirm'],outcomes:['success','failure']},
  payment_result: {steps:['checkout'],outcomes:['success','cancelled','failure','pending']},
  report_open: {steps:['report'],outcomes:['success','failure']},
  registration_submit: {steps:['registration'],outcomes:['success','failure']},
  auth_result: {steps:['login'],outcomes:['success','failure']}
}
let inflight = false, nextTry = 0, task = null
function available() { try { return (wx.getExtConfigSync() || {}).analyticsEnabled !== false } catch (_) { return false } }
function enabled() { try { return available() && wx.getStorageSync(PREFERENCE_KEY) !== false } catch (_) { return false } }
function getSettings(){return {available:available(),enabled:enabled()}}
function setEnabled(value){
  if(typeof value!=='boolean')throw new TypeError('Invalid analytics preference')
  // Do not resurrect a queue that could not be cleared while statistics were disabled.
  if(value && !enabled())wx.setStorageSync(KEY,[])
  // Persist first: storage failure must be visible instead of claiming the choice was saved.
  wx.setStorageSync(PREFERENCE_KEY,value)
  if(!enabled()){
    try{wx.setStorageSync(KEY,[])}finally{
      if(task && typeof task.abort==='function')try{task.abort()}catch(_){}
    }
  }
  return getSettings()
}
function valid(row) {
  return row && Object.keys(row).sort().join(',') === 'environment,event,id,outcome,step' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(row.id) &&
    rules[row.event] && rules[row.event].steps.includes(row.step) && rules[row.event].outcomes.includes(row.outcome) &&
    ['develop','trial','release'].includes(row.environment)
}
function read() { try { const rows=wx.getStorageSync(KEY); return Array.isArray(rows)?rows.filter(valid).slice(-50):[] } catch (_) { return [] } }
function save(rows) { try { wx.setStorageSync(KEY,rows.slice(-50));return true } catch (_) {return false} }
function uuid() { return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,c=>{const n=Math.floor(Math.random()*16);return (c==='x'?n:(n&3)|8).toString(16)}) }
function track(event, step, outcome) {
  try {
    if (!enabled() || !rules[event] || !rules[event].steps.includes(step) || !rules[event].outcomes.includes(outcome)) return
    const row={id:uuid(),event,step,outcome,environment:config.environment}
    if (!valid(row)) return
    if(save(read().concat(row))) flush()
  } catch (_) { /* Analytics must never interrupt a clinical or payment operation. */ }
}
function flush() {
  try {
    if (!enabled()) { save([]); return }
    if (inflight || Date.now()<nextTry) return
    const rows=read().slice(0,20)
    let token=wx.getStorageSync(config.storageKeys.TOKEN)
    try { token=(getApp().globalData || {}).token || token } catch (_) {}
    if (!rows.length || !token) return
    inflight=true
    task=wx.request({url:config.baseURL+'/client-events',method:'POST',timeout:3000,
      header:{'Content-Type':'application/json',Authorization:'Bearer '+token},data:{events:rows},
      success(res) {
        if(res.statusCode===200 && res.data && res.data.code===200) {
          const ids=new Set(rows.map(row=>row.id));nextTry=save(read().filter(row=>!ids.has(row.id)))?0:Date.now()+30000
        } else nextTry=Date.now()+30000
      },
      fail(){nextTry=Date.now()+30000},
      complete(){inflight=false;if(!nextTry && read().length)flush()}
    })
  } catch (_) { inflight=false;nextTry=Date.now()+30000 }
}
module.exports={track,flush,getSettings,setEnabled}
