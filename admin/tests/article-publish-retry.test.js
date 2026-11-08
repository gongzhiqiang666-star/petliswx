import test from 'node:test';
import assert from 'node:assert/strict';
import {createArticlePublisher} from '../src/pages/operations.js';
test('同一资讯弹窗失败重试沿用请求号，新弹窗使用独立请求号',async()=>{
 const calls=[];let fail=true;const api={request:async(path,options)=>{calls.push({path,...options});if(fail)throw Error('响应丢失');return 7;}};
 const publish=createArticlePublisher(api),data={title:'标题',type:'article',popupEnabled:'false',liveAt:'',liveUrl:''};
 await assert.rejects(publish(data),/响应丢失/);fail=false;assert.equal(await publish(data),7);
 assert.equal(calls[0].headers['Idempotency-Key'],calls[1].headers['Idempotency-Key']);assert.ok(calls[0].headers['Idempotency-Key'].length>=16);
 assert.equal(calls[1].data.popupEnabled,false);assert.equal(calls[1].data.liveAt,null);assert.equal(calls[1].data.liveUrl,null);assert.equal(data.popupEnabled,'false');
 await createArticlePublisher(api)(data);assert.notEqual(calls[2].headers['Idempotency-Key'],calls[0].headers['Idempotency-Key']);
});
