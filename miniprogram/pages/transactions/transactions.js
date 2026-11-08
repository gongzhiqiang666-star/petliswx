const request=require('../../utils/request')
Page({
 data:{kind:'points',hospitalId:null,category:'all',items:[],page:0,more:false,busy:false,error:'',filters:[{id:'all',title:'全部'},{id:'income',title:'收入'},{id:'expense',title:'支出'}]},
 onLoad(o){
  const kind=o.kind==='hospital'?'hospital':o.kind==='wallet'?'wallet':'points'
  if(kind==='hospital'&&(!/^\d+$/.test(String(o.hospitalId))||!Number.isSafeInteger(Number(o.hospitalId))||Number(o.hospitalId)<1)){this.setData({kind,error:'医院编号无效'});return}
  this.setData({kind,hospitalId:kind==='hospital'?Number(o.hospitalId):null})
  wx.setNavigationBarTitle({title:kind==='hospital'?'医院资金流水':kind==='wallet'?'个人余额流水':'积分流水'})
  return this.load(true)
 },
 async load(reset){
  if(this.data.busy||this.data.kind==='hospital'&&!this.data.hospitalId)return
  const page=reset?0:this.data.page+1
  this.setData({busy:true,error:'',...(reset?{items:[],more:false}:{})})
  try{
   const path=this.data.kind==='hospital'?'/account/hospital-wallets/'+this.data.hospitalId+'/transactions':'/account/'+this.data.kind+'/transactions'
   const r=await request.get(path,{page,size:20,category:this.data.category})
   const items=r.items.map(x=>({...x,time:x.createdAt?new Date(x.createdAt*1000).toLocaleString():'无'}))
   this.setData({items:reset?items:this.data.items.concat(items),page,more:(page+1)*20<r.total})
  }catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}
 },
 filter(e){if(this.data.busy)return;this.setData({category:e.currentTarget.dataset.id});return this.load(true)},
 retry(){return this.load(true)},more(){return this.load(false)},
 async exportFile(){
  if(this.data.busy||this.data.kind==='hospital')return
  this.setData({busy:true,error:''})
  try{const path=await request.downloadFile('/account/transactions/export?points='+(this.data.kind==='points')+'&category='+this.data.category);await wx.shareFileMessage({filePath:path,fileName:(this.data.kind==='points'?'积分':'余额')+'流水.csv'})}
  catch(e){if(!String(e.errMsg||'').includes('cancel'))this.setData({error:e.message||'导出失败'})}finally{this.setData({busy:false})}
 }
})
