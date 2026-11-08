const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
const source=fs.readFileSync(__dirname+'/overlay/public/assets/js/backend/petlis_workflow.js','utf8');
function fixture(fetcher) {
 let controller,handler,requests=0,assigned=[],timers=0;
 const button={disabled:false},notices=[];
 const form={values:{order_id:'23903',csrf:'private-csrf',summary:'保留摘要',conclusion:'保留结论'},appendChild(n){notices.push(n)},addEventListener(type,fn){handler=fn}};
 const document={querySelectorAll(s){return s.startsWith('form')?[form]:[button]},createElement(){return {style:{},children:[],setAttribute(){},appendChild(n){this.children.push(n)}}}};
 class FormData {constructor(f){this.entries=Object.entries(f.values)} [Symbol.iterator](){return this.entries[Symbol.iterator]()}}
 const window={fetch:true,FormData,URLSearchParams,AbortController,location:{assign(url){assigned.push(url)}}};
 vm.runInNewContext(source,{define(deps,fn){controller=fn()},window,document,FormData,URLSearchParams,AbortController,fetch(...args){requests++;return fetcher(...args)},setTimeout(){return ++timers},clearTimeout(){}});
 controller.index();
 return {form,button,notices,assigned,submit:()=>handler({preventDefault(){}}),requests:()=>requests};
}
(async()=>{
 let f=fixture(async(url,options)=>{assert.equal(url,'/laboklin.php/petlis_workflow/operate');assert.equal(options.headers['X-Requested-With'],'XMLHttpRequest');assert.equal(options.redirect,'error');assert.equal(options.body.get('csrf'),'private-csrf');return {ok:true,json:async()=>({code:0,message:'样本摘要不符合要求<script>'})}});
 await f.submit();assert.equal(f.form.values.summary,'保留摘要');assert.equal(f.button.disabled,false);assert.match(f.notices[0].textContent,/内容已保留/);assert.equal(f.assigned.length,0);assert.match(f.notices[0].textContent,/<script>/); // textContent, never HTML insertion.
 f=fixture(async()=>{throw Error('network')});await f.submit();assert.equal(f.button.disabled,true);assert.equal(f.form.values.conclusion,'保留结论');assert.equal(f.notices[0].children[0].target,'_blank');await f.submit();assert.equal(f.requests(),1);
 for(const result of [{code:1,url:'https://external.invalid'},{code:1,url:'/laboklin.php/petlis_workflow/index?id=999'}]){f=fixture(async()=>({ok:true,json:async()=>result}));await f.submit();assert.equal(f.assigned.length,0);assert.equal(f.button.disabled,true)}
 let release;f=fixture(()=>new Promise(resolve=>release=resolve));const pending=f.submit();await f.submit();assert.equal(f.requests(),1);release({ok:true,json:async()=>({code:1,url:'/laboklin.php/petlis_workflow/index?id=23903'})});await pending;assert.deepEqual(f.assigned,['/laboklin.php/petlis_workflow/index?id=23903']);assert.equal(f.button.disabled,true);
 console.log('PASS: retained values, CSRF POST, safe error text, uncertain outcome lock, duplicate prevention and exact success redirect');
})().catch(e=>{console.error(e);process.exitCode=1});
