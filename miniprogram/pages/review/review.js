const request=require('../../utils/request')
const config=require('../../config/config')
Page({
 data:{tab:'registrations',items:[],page:0,hasMore:false,busy:false,error:'',reason:'',invoiceUrl:'',activeId:null,tabs:[{id:'registrations',name:'医生注册'},{id:'profile-changes',name:'资料变更'},{id:'orders',name:'送检申请'},{id:'invoices',name:'发票申请'}]},
 onLoad(){const u=wx.getStorageSync(config.storageKeys.USER_INFO);if(!u || !u.reviewer){wx.reLaunch({url:'/pages/login/login'});return}this.load(true)},
 async load(reset){if(this.data.busy)return;const page=reset?0:this.data.page+1;this.setData({busy:true,error:''});try{const r=await request.get('/review/'+this.data.tab,{page,size:20});const items=r.items.map(x=>({...x,key:x.userId && this.data.tab==='registrations'?x.userId:(x.orderId || x.id),requestedText:x.requestedData || '',files:this.files(x.qualificationFiles)}));this.setData({items:reset?items:this.data.items.concat(items),page,hasMore:(page+1)*20<r.total})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 files(value){try{return typeof value==='string'?JSON.parse(value):value || []}catch(e){return []}},
 tab(e){this.setData({tab:e.currentTarget.dataset.id,items:[],activeId:null});this.load(true)},
 more(){this.load(false)},retry(){this.load(true)},
 edit(e){this.setData({activeId:e.currentTarget.dataset.id,reason:'',invoiceUrl:''})},
 invoice(e){this.setData({invoiceUrl:e.detail.value})},
 reason(e){this.setData({reason:e.detail.value})},
 async decide(e){if(this.data.busy)return;const {id,approved}=e.currentTarget.dataset;const ok=approved==='yes';if(!ok && !this.data.reason.trim()){wx.showToast({title:'请填写驳回原因',icon:'none'});return}
 const confirm=await wx.showModal({title:ok?'确认通过审核？':'确认驳回申请？',content:ok?'请确认申请资料已核实。':'申请人将收到填写的驳回原因。'});if(!confirm.confirm)return
 this.setData({busy:true,error:''});try{await request.post('/review/'+this.data.tab+'/'+id,{approved:ok,reason:this.data.reason.trim()});this.setData({activeId:null});wx.showToast({title:'已处理'})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}this.load(true)},
 async issue(e){if(this.data.busy)return;const approved=e.currentTarget.dataset.approved==='yes';if(approved && !/^https:\/\/[^\s]+$/.test(this.data.invoiceUrl)){this.setData({error:'请填写可访问的 HTTPS 电子发票地址'});return}if(!approved && !this.data.reason.trim()){this.setData({error:'请填写驳回原因'});return}const c=await wx.showModal({title:approved?'确认发票已开具？':'确认驳回发票申请？',content:approved?'请核实发票抬头、金额与地址。':'申请人可以修改后重新提交。'});if(!c.confirm)return;this.setData({busy:true,error:''});try{await request.post('/invoices/'+e.currentTarget.dataset.id+'/'+(approved?'issue':'reject'),{invoiceUrl:this.data.invoiceUrl.trim(),reason:this.data.reason.trim()});this.setData({activeId:null});wx.showToast({title:'已处理'})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}this.load(true)},
 async file(e){this.setData({busy:true,error:''});try{const r=await new Promise((resolve,reject)=>wx.downloadFile({url:config.baseURL+e.currentTarget.dataset.url,header:request.getHeaders(),success:resolve,fail:reject}));if(r.statusCode!==200)throw new Error('文件无法访问');if(/\.pdf$/i.test(e.currentTarget.dataset.url))await wx.openDocument({filePath:r.tempFilePath});else await wx.previewImage({urls:[r.tempFilePath]})}catch(e){this.setData({error:e.message || '无法打开资质'})}finally{this.setData({busy:false})}},
 logout(){getApp().logout()}
})
