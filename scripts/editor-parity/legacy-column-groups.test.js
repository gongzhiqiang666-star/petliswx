import test from 'node:test';import assert from 'node:assert/strict';
import {changeColumnGroups,columnCommand} from './legacy-column-groups.js';
const source=()=>[{attributes:{class:'left'},columns:[{width:'100'}]},{attributes:{class:'right'},columns:[{width:'300'}]}];
test('inserting a column keeps old widths in their original logical columns',()=>{
 const groups=source(),copy=structuredClone(groups);const next=changeColumnGroups(groups,'insert',1,1);
 assert.deepEqual(next[0].columns,[{width:'100'},{}]);assert.deepEqual(next[1].columns,[{width:'300'}]);assert.deepEqual(groups,copy);
 assert.deepEqual(changeColumnGroups(next,'delete',1,1),groups);
});
test('span and group-span columns expand for explicit edits without losing widths',()=>{
 const groups=[{attributes:{class:'g'},columns:[{span:'2',width:'80'}]}];
 assert.deepEqual(changeColumnGroups(groups,'delete',1,1)[0].columns,[{width:'80'}]);
 const empty=[{attributes:{span:'2',style:'width:80px'},columns:[]}];const next=changeColumnGroups(empty,'insert',1,1);assert.equal(next[0].columns.length,3);assert.equal(next[0].attributes.style,'width:80px');
});
test('implicit tail columns and absent column metadata remain implicit',()=>{
 const groups=source();assert.equal(changeColumnGroups(groups,'insert',3,1),groups);assert.deepEqual(changeColumnGroups(groups,'delete',3,1),groups);
 assert.equal(changeColumnGroups(null,'insert',0,1),null);assert.deepEqual(changeColumnGroups([],'insert',0,1),[]);
});

test('column command wrapping is idempotent and delegates can-only calls without metadata mutation',()=>{
 const calls=[];const factory=(...args)=>props=>{calls.push({args,props});return false;};const wrapped=columnCommand(factory,'after');
 assert.equal(columnCommand(wrapped,'after'),wrapped);const props={dispatch:undefined,token:'unchanged'};assert.equal(wrapped('option')(props),false);assert.deepEqual(calls[0].args,['option']);assert.equal(calls[0].props,props);
});
