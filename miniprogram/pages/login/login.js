const request = require('../../utils/request')
const config = require('../../config/config')
Page({
 data:{account:'',password:'',remember:true,busy:false,error:''},
 onLoad(){this.setData({account:wx.getStorageSync('rememberedAccount') || ''})
 const info=wx.getStorageSync(config.storageKeys.USER_INFO)
 if(wx.getStorageSync(config.storageKeys.TOKEN) && info) getApp().acceptSession({...info,token:wx.getStorageSync(config.storageKeys.TOKEN)})},
 field(e){this.setData({[e.currentTarget.dataset.field]:e.detail.value,error:''})},
 remember(){this.setData({remember:!this.data.remember})},
 async submit(){if(this.data.busy)return
 if(!this.data.account.trim() || !this.data.password){this.setData({error:'请填写账号和密码'});return}
 this.setData({busy:true,error:''})
 try{const result=await request.post('/auth/password/login',{account:this.data.account.trim(),password:this.data.password},false)
 if(this.data.remember)wx.setStorageSync('rememberedAccount',this.data.account.trim());else wx.removeStorageSync('rememberedAccount')
 this.setData({password:''});getApp().acceptSession(result,this.data.remember)
 }catch(e){this.setData({error:e.message || '登录失败，请重试'})}finally{this.setData({busy:false})}},
 forgot(){wx.navigateTo({url:'/pages/security/security?reset=1'})},
 register(){wx.navigateTo({url:'/pages/register/register'})},
 async wechat(){if(this.data.busy)return;this.setData({busy:true,error:''});try{const {code}=await wx.login();getApp().acceptSession(await request.post('/auth/wechat/login',{code},false),this.data.remember)}catch(e){this.setData({error:e.message || '微信登录失败'})}finally{this.setData({busy:false})}}
})
