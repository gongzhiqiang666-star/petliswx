// Bounded local diagnostics. Never persist raw messages, payloads, URL queries or identities.
const routes = new Set(require('../config/routes'))
const KEY = 'petlis_diagnostics_v1'
const kinds = new Set(['Error','TypeError','ReferenceError','SyntaxError','RangeError','URIError'])
function valid(row) {
  return row && Object.keys(row).sort().join(',') === 'frames,kind,route,time,type' &&
    ['error','rejection'].includes(row.kind) && kinds.has(row.type) &&
    (row.route === 'unknown' || routes.has(row.route)) &&
    typeof row.time === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(row.time) && Number.isFinite(Date.parse(row.time)) &&
    Array.isArray(row.frames) && row.frames.length <= 5 && row.frames.every(frame => typeof frame === 'string' && /^[A-Za-z0-9_-]{1,80}\.js:[0-9]{1,9}:[0-9]{1,9}$/.test(frame))
}
function read() {
  try { const rows=wx.getStorageSync(KEY);return Array.isArray(rows)?rows.slice(-20).filter(valid):[] } catch (_) {return []}
}
function capture(kind, error) {
  try {
    const pages=getCurrentPages(), route=pages.length?pages[pages.length-1].route:''
    const value=typeof error==='string'?error:String(error && (error.stack || error.message) || '')
    const frames=[]
    // Keep only source file basenames and coordinates, never text from the exception itself.
    for(const line of value.split('\n').slice(1,30)) {
      const match=line.match(/(?:^|[\s/(])([A-Za-z0-9_-]+\.js):(\d+):(\d+)/)
      if(match && match[1].length<=83 && match[2].length<=9 && match[3].length<=9)frames.push(match[1]+':'+match[2]+':'+match[3])
      if(frames.length===5)break
    }
    const candidate=error && typeof error==='object'?error.name:value.split(':')[0]
    const row={time:new Date().toISOString(),kind:kind==='rejection'?'rejection':'error',type:kinds.has(candidate)?candidate:'Error',route:routes.has(route)?route:'unknown',frames}
    wx.setStorageSync(KEY,read().concat(row).slice(-20))
  } catch (_) { /* Diagnostics must never cause a second application error. */ }
}
function clear(){try{wx.removeStorageSync(KEY)}catch(_){}}
module.exports={capture,read,clear}
