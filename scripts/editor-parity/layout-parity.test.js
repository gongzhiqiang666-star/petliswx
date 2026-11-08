import test from 'node:test';import assert from 'node:assert/strict';
import {compareLayoutRecords} from './layout-parity.js';
const snapshot=()=>({assetsSettled:true,externalImagesDeferred:0,dataImagesFailed:0,root:{width:800,height:100,scrollWidth:800,scrollHeight:100},nodes:[{tag:'td',x:0,y:0,width:100,height:20,styles:{borderTopColor:'rgb(0, 0, 0)'}}]});
test('layout comparison accepts identical geometry and rejects size or border color changes',()=>{
 const before=snapshot(),after=snapshot();assert.equal(compareLayoutRecords(before,after).equal,true);
 after.nodes[0].width=110;assert.equal(compareLayoutRecords(before,after).equal,false);
 after.nodes[0].width=100;after.nodes[0].styles.borderTopColor='rgb(255, 0, 0)';assert.equal(compareLayoutRecords(before,after).equal,false);
});
test('layout comparison reports count mismatch and unsettled assets without content',()=>{
 const before=snapshot(),after=snapshot();after.nodes=[];after.assetsSettled=false;
 const result=compareLayoutRecords(before,after);assert.equal(result.equal,false);assert.equal(result.nodeCountChanged,true);assert.equal(result.assetsSettled,false);assert.ok(!JSON.stringify(result).includes('rgb('));
});
