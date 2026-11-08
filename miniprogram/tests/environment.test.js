const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path')
function config(version,baseURL,{unavailable=false}={}){const context={module:{exports:{}},wx:{getAccountInfoSync:()=>{if(unavailable)throw Error('unknown');return {miniProgram:{envVersion:version}}},getExtConfigSync:()=>({baseURL})}};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../config/config.js'),'utf8'),context);return context.module.exports}
test('开发版保留本机联调，体验版正式版不回退本机且禁止mock',()=>{
 assert.equal(config('develop').baseURL,'http://127.0.0.1:8082/api');assert.equal(config('develop','http://127.0.0.1:8082/').allowMockPayment,true);
 for(const env of ['trial','release']){
  assert.throws(()=>config(env));assert.throws(()=>config(env,'http://127.0.0.1:8082/api'));
  const c=config(env,'https://api.example.test/api/');assert.equal(c.baseURL,'https://api.example.test/api');assert.equal(c.allowMockPayment,false);assert.equal(c.environment,env);
 }
});
test('未知版本不降级开发，非法或携带凭据及查询的地址拒绝',()=>{
 for(const env of [undefined,'','production'])assert.throws(()=>config(env));assert.throws(()=>config('develop',null,{unavailable:true}));
 for(const url of ['https://user:password@example.test','https://example.test?token=secret','https://example.test/#fragment',' https://example.test','javascript:alert(1)',{}])assert.throws(()=>config('release',url));
});
