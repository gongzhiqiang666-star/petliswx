const test=require('node:test'),assert=require('node:assert/strict');const {loadPage}=require('./helpers');
test('账户分别加载个人与授权医院余额，医院加载失败不影响个人余额',async()=>{
 let fail=false;const {instance:p}=loadPage('account/account',{get:async url=>{if(url==='/account')return {balance:'37.21',points:2};if(fail)throw Error('医院暂不可用');return [{hospitalId:2,hospitalName:'医院A',balance:'12.50'}]}});
 await p.load();assert.equal(p.data.account.balance,'37.21');assert.equal(p.data.hospitalWallets[0].balance,'12.50');fail=true;await p.load();assert.equal(p.data.account.balance,'37.21');assert.deepEqual(p.data.hospitalWallets,[]);assert.equal(p.data.hospitalFundsError,'医院暂不可用');
});
test('医院资金入口只能打开当前返回的授权医院',async()=>{
 const {instance:p,wx}=loadPage('account/account');const calls=[];wx.navigateTo=o=>calls.push(o.url);p.data.hospitalWallets=[{hospitalId:2}];p.openHospitalFund({currentTarget:{dataset:{id:3}}});assert.equal(calls.length,0);p.openHospitalFund({currentTarget:{dataset:{id:2}}});assert.match(calls[0],/kind=hospital&hospitalId=2/);
});
test('医院流水请求医院独立接口，不误导出个人余额',async()=>{
 const calls=[];const {instance:p}=loadPage('transactions/transactions',{get:async(url,params)=>{calls.push({url,params});return {items:[{id:1,amount:'-2.00',memo:'扣款',createdAt:100}],total:1}}});
 await p.onLoad({kind:'hospital',hospitalId:'2'});assert.equal(calls[0].url,'/account/hospital-wallets/2/transactions');assert.equal(p.data.items[0].balanceAfter,undefined);await p.exportFile();assert.equal(calls.length,1);
});
test('非法医院编号不发起查询，加载时不切换流水分类',async()=>{
 const calls=[];const {instance:p}=loadPage('transactions/transactions',{get:async url=>{calls.push(url);return {items:[],total:0}}});await p.onLoad({kind:'hospital',hospitalId:'bad'});assert.equal(calls.length,0);assert.ok(p.data.error);p.data.busy=true;p.data.category='all';p.filter({currentTarget:{dataset:{id:'expense'}}});assert.equal(p.data.category,'all');
});
