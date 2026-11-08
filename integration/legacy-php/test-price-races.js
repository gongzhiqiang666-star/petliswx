// Executes the installed AMD controller with deterministic asynchronous responses; no HTTP or writes.
const fs=require('fs'),vm=require('vm'),assert=require('assert/strict'),os=require('os'),path=require('path')
const filename=process.argv[2] || path.join(os.homedir(),'.local/share/petlis/php-preview/app/public/assets/js/backend/project/lb_diagnosisdetail.js')
const fields=new Map(), handlers=new Map(), requests=[]
let controller
function $(selector){
 if(selector && selector.__chain)return selector
 return {__chain:true,selector,val(value){if(arguments.length){fields.set(selector,value);return this}return fields.get(selector)||''},
 data(){return this},click(){return this},change(){return this},selectPageRefresh(){return this},
 on(event,target,callback){handlers.set(target,callback);return this}}
}
$.post=(url,body,callback)=>requests.push({url,body,callback})
vm.runInNewContext(fs.readFileSync(filename,'utf8'),{define(deps,factory){controller=factory($,null,{}, {},{api:{bindevent(){}}},{})},Fast:{api:{query(){return 0}}},console})
controller.api.table=()=>{}
controller.api.bindevent()
const select=$('#c-InfoTestTypeId'),trigger=id=>{select.val(id);handlers.get('#c-InfoTestTypeId').call(select)}
trigger('850');const first=requests.at(-1)
trigger('851');const middle=requests.at(-1)
trigger('850');const latest=requests.at(-1)
latest.callback({list:[{price:'88.00'}]})
assert.equal(fields.get('#c-InfoTestTypePriceId'),'88.00')
first.callback({list:[{price:'668.00'}]})
middle.callback({list:[{price:'999.00'}]})
assert.equal(fields.get('#c-InfoTestTypePriceId'),'88.00','stale A/B responses must not replace latest A')
controller.api.price();const oldCalculation=requests.at(-1)
$('#c-InfoTestTypePriceId').val('99.00');controller.api.price();const newCalculation=requests.at(-1)
newCalculation.callback({code:1,msg:'99.00'})
oldCalculation.callback({code:1,msg:'88.00'})
assert.equal(fields.get('#c-txtResultPrice'),'99.00','older discount calculation must not overwrite latest price')
controller.api.price();const pendingBeforeSelection=requests.at(-1)
trigger('852');const emptySelection=requests.at(-1)
pendingBeforeSelection.callback({code:1,msg:'99.00'})
assert.equal(fields.get('#c-txtResultPrice'),'','changing selection must invalidate pending calculations immediately')
emptySelection.callback({list:[]})
assert.equal(fields.get('#c-InfoTestTypePriceId'),'','missing prices must not reuse previous project price')
assert.equal(fields.get('#c-txtResultPrice'),'')
console.log('PASS: production legacy controller rejects A-B-A stale responses, old calculations and empty-price reuse')
