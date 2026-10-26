const request=require('./request')
const config=require('../config/config')
// 仅当连接本机/局域网开发服务时允许走开发模拟回调；真机/生产环境绝不触发。
const isLocalDev=/^https?:\/\/(127\.0\.0\.1|localhost|\[::1\]|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)(:\d+)?(\/|$)/.test(config.baseURL)
async function payOrder(orderId){
 const payment=await request.post('/payment/create',{orderId:Number(orderId)})
 if(payment.mock){
  // 本机开发环境：调用后端开发专用模拟回调完成支付闭环，无真实资金往来。
  if(!isLocalDev)throw new Error('当前支付处于开发模式，尚不能进行真实付款')
  await request.post('/payment/mock-callback/'+encodeURIComponent(payment.paymentNo))
 }else{
 try{await wx.requestPayment({timeStamp:payment.timeStamp,nonceStr:payment.nonceStr,package:payment.package,signType:payment.signType,paySign:payment.paySign})}catch(e){throw new Error(String(e.errMsg || '').includes('cancel')?'支付未完成，可稍后继续':'微信支付未完成，请查看订单状态后重试')}
 }
 for(let i=0;i<5;i++){
  const status=await request.get('/payment/status/'+encodeURIComponent(payment.paymentNo))
  if(status.status===1)return true
  if(status.status===2)throw new Error('支付已关闭，请刷新订单')
  await new Promise(resolve=>setTimeout(resolve,1000))
 }
 return false
}
module.exports={payOrder}
