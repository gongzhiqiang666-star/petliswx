const request=require('../../utils/request')
const newKey=()=> 'recharge_'+Date.now()+'_'+Math.random().toString(36).slice(2)
Page({
 data:{amount:'100',requestKey:newKey(),items:[],page:0,more:false,busy:false,error:'',balance:null},
 onShow(){this.load(true)},
 amount(e){if(e.detail.value!==this.data.amount)this.setData({amount:e.detail.value,requestKey:newKey()})},
 async load(reset){if(this.data.busy)return;const page=reset?0:this.data.page+1;this.setData({busy:true,error:''});try{const [r,account]=await Promise.all([request.get('/wallet/recharges',{page,size:20}),request.get('/account')]);this.setData({items:reset?r.items:this.data.items.concat(r.items),page,more:(page+1)*20<r.total,balance:account.balance})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 retry(){this.load(true)},more(){this.load(false)},
 async submit(){if(this.data.busy)return;const value=Number(this.data.amount);if(!/^\d+(\.\d{1,2})?$/.test(this.data.amount) || value<1 || value>50000){this.setData({error:'请输入1至50,000元，最多两位小数'});return}const c=await wx.showModal({title:'确认充值 ¥'+value.toFixed(2)+'？',content:'充值到你本人的 Petlis 账户。付款完成后，以服务端确认到账为准。'});if(!c.confirm)return;this.setData({busy:true,error:''});try{const payment=await request.post('/wallet/recharges',{amount:value,requestKey:this.data.requestKey});await this.pay(payment)}catch(e){this.setData({error:e.message || '充值未完成，请先查看充值记录再重试'})}finally{this.setData({busy:false})}this.loadAfterPayment()},
 async resume(e){if(this.data.busy)return;this.setData({busy:true,error:''});try{await this.pay(await request.post('/wallet/recharges/'+e.currentTarget.dataset.no+'/resume',{}))}catch(e){this.setData({error:e.message || '充值未完成'})}finally{this.setData({busy:false})}this.loadAfterPayment()},
 async pay(payment){if(payment.status===1){wx.showToast({title:'充值已到账'});return}if(payment.mock){await wx.showModal({title:'当前为开发支付',content:'尚不能真实付款，余额不会增加。此记录可以关闭，待正式支付配置完成后再充值。',showCancel:false});return}try{await wx.requestPayment({timeStamp:payment.timeStamp,nonceStr:payment.nonceStr,package:payment.package,signType:payment.signType,paySign:payment.paySign})}catch(e){throw new Error(String(e.errMsg || '').includes('cancel')?'充值未付款，可从记录继续支付':'付款未完成，请查询充值记录后重试')}
 for(let i=0;i<5;i++){const r=await request.get('/wallet/recharges/'+payment.paymentNo);if(r.status==='paid'){wx.showToast({title:'充值已到账'});return}if(r.status==='closed')throw new Error('充值已关闭，请核对记录');await new Promise(resolve=>setTimeout(resolve,1000))}wx.showToast({title:'到账确认中，请稍后刷新',icon:'none'})},
 async loadAfterPayment(){// Keep a payment failure message visible while refreshing the authoritative balance.
 const message=this.data.error;await this.load(true);if(message)this.setData({error:message})},
 async close(e){if(this.data.busy)return;const c=await wx.showModal({title:'关闭这笔未付款充值？',content:'已到账充值无法关闭。关闭后可重新发起。'});if(!c.confirm)return;this.setData({busy:true,error:''});try{await request.post('/wallet/recharges/'+e.currentTarget.dataset.no+'/close',{});this.setData({requestKey:newKey()})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}this.loadAfterPayment()}
})
