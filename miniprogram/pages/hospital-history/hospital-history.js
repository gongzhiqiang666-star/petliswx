const request=require('../../utils/request')
Page({
 data:{cancellingId:null,hospitalId:null,hospitalName:'',enabled:false,loading:false,error:'',items:[],total:0,page:0,hasMore:false,userNo:'',startDate:'',endDate:'',status:'-1',statusIndex:0,statuses:[{value:'-1',label:'全部'},{value:'0',label:'待审核'},{value:'1',label:'审核未通过'},{value:'2',label:'审核通过'},{value:'3',label:'已出报告'},{value:'4',label:'已取消'}]},
 onLoad(){this._unloaded=false;this.setData({hospitalId:getApp().getHospitalId()})},
 onShow(){this.load()},
 onUnload(){this._unloaded=true;this._version=(this._version||0)+1},
 input(e){this.setData({[e.currentTarget.dataset.key]:e.detail.value})},
 statusChange(e){const i=Number(e.detail.value);this.setData({statusIndex:i,status:this.data.statuses[i].value})},
 reset(){this.setData({userNo:'',startDate:'',endDate:'',status:'-1',statusIndex:0});return this.load()},
 search(){return this.load()},
 async load(append=false){
  if(typeof append!=='boolean')append=false;
  if(append&&(!this.data.hasMore||this.data.loading))return;
  const version=(this._version||0)+1;this._version=version;
  const hospitalId=this.data.hospitalId;
  if(!hospitalId){this.setData({error:'账号未关联医院，请先完善医院资料。',items:[],hasMore:false,loading:false});return}
  if(Boolean(this.data.startDate)!==Boolean(this.data.endDate)||this.data.startDate>this.data.endDate){this.setData({error:'请选择完整且顺序正确的起止日期。',loading:false});return}
  const query=append?{...this._appliedQuery,page:this.data.page+1}:{hospitalId,userNo:this.data.userNo.trim(),status:Number(this.data.status),page:0,size:10,...(this.data.startDate?{from:this.data.startDate+'T00:00:00',to:this.data.endDate+'T23:59:59'}:{})};
  this.setData({loading:true,error:'',...(append?{}:{items:[],total:0,hasMore:false})});
  try{
   const caps=await request.get('/orders/hospital-history/capabilities');if(version!==this._version)return;
   this.setData({enabled:caps.legacyHospitalHistory===true});
   if(caps.legacyHospitalHistory!==true){this.setData({items:[],hasMore:false,error:'医院历史查询暂未开启。'});return}
   const result=await request.get('/orders/hospital-history',query);if(version!==this._version)return;
   const items=append?[...this.data.items,...result.items]:result.items;this._appliedQuery={...query};
   this.setData({items,hospitalName:result.hospitalName||'',total:Number(result.total),page:query.page,hasMore:items.length<Number(result.total)});
  }catch(error){if(version===this._version)this.setData({error:error.message||'历史记录加载失败，请重试。'})}
  finally{if(version===this._version)this.setData({loading:false})}
 },
 async cancelOrder(event){
  if(this._cancelBusy)return;
  const id=Number(event.currentTarget.dataset.id),row=this.data.items.find(item=>Number(item.orderId)===id);
  if(!row||row.canCancel!==true||!row.cancelToken)return;
  this._cancelBusy=true;
  try{
   const choice=await wx.showModal({title:'取消这份送检？',content:(row.paid?'该旧单已有付款标记。':'')+'取消后保留检测明细和资金记录，不会自动退款。',confirmText:'确认取消'});
   if(!choice.confirm||this._unloaded)return;
   this.setData({cancellingId:id,error:''});
   const cache=this._cancellations||(this._cancellations={}),key=id+':'+row.cancelToken;
   cache[key]=cache[key]||'hospital_cancel_'+Date.now()+'_'+Math.random().toString(36).slice(2,14);
   await request.post('/orders/hospital-history/'+id+'/cancel',{hospitalId:this.data.hospitalId,expectedToken:row.cancelToken,requestKey:cache[key]});
   if(!this._unloaded)await this.load();
  }catch(error){if(!this._unloaded)this.setData({error:error.message||'取消结果待核对，请刷新记录或重试。'})}
  finally{this._cancelBusy=false;if(!this._unloaded)this.setData({cancellingId:null})}
 },
 more(){return this.load(true)},
 retry(){return this.data.items.length?this.more():this.load()}
})
