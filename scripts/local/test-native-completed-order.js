// Read-only simulator verification of the exact order previously submitted through native pages.
const fs=require('fs'),assert=require('assert/strict');const root=process.env.HOME+'/.local/share/petlis';const automator=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');const chain=JSON.parse(fs.readFileSync(root+'/qa/native-full-chain.json'));const project=JSON.parse(fs.readFileSync('miniprogram/project.config.json'));
(async()=>{const m=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'});const wait=async fn=>{for(let i=0;i<50;i++){const v=await fn();if(v)return v;await new Promise(r=>setTimeout(r,200))}throw Error('Native completed order view did not load')};try{
 const appid=await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId);assert.equal(appid,project.appid);
 let p=await m.reLaunch('/pages/order-detail/order-detail?id='+chain.orderId);
 await wait(async()=>!(await p.data('loading')) && await p.data('shipment') && await p.data('clinical'));
 assert.equal(Number(await p.data('orderData.orderId')),chain.orderId);assert.equal(await p.data('orderData.sampleStatus'),3);
 assert.equal(await p.data('clinical.clinicalSymptoms'),'本机原生联调：采样后状态稳定');
 assert.equal(await p.data('shipment.status'),'RECEIVED');const incidents=await p.data('shipment.exceptions');assert.equal(incidents.length,1);assert.equal(incidents[0].stateLabel,'已处理');assert.equal(await p.data('shipment.exceptionNote'),'');
 assert.ok(await p.$('.btn-preview'));const timeline=await p.data('shipmentTimeline');const raw=timeline.filter(x=>/^(PICKUP|TRANSIT|DELIVER|RECEIVE|EXCEPTION_RESOLVED)$/.test(x.title)).map(x=>x.title);
 assert.equal(raw.length,0,'Native logistics timeline must not expose internal operation codes');
 const evidence={orderId:chain.orderId,reportId:chain.reportIds[0],nativeCompletedStatus:true,clinicalMatches:true,exceptionHistoryResolved:true,reportButtonVisible:true,untranslatedTimelineNodes:raw};
 await new Promise(r=>setTimeout(r,650));await m.screenshot({path:root+'/qa/native-completed-order.png'});
 const logistics=await p.$('[aria-label="物流配送"]');if(logistics){const offset=await logistics.offset();await m.pageScrollTo(Math.max(0,offset.top-60));await new Promise(r=>setTimeout(r,500));await m.screenshot({path:root+'/qa/native-completed-logistics.png'})}
 p=await m.navigateTo('/pages/report-view/report-view?id='+chain.reportIds[0]);await wait(async()=>!(await p.data('loading')));
 assert.equal(await p.data('error'),false);assert.equal(Number(await p.data('report.reportId')),chain.reportIds[0]);assert.equal(await p.data('report.paymentRestricted'),false);assert.match(await p.data('report.reportSummary'),/本机合成验证结论/);
 evidence.nativeReportViewMatches=true;await new Promise(r=>setTimeout(r,650));await m.screenshot({path:root+'/qa/native-report-detail.png'});
 fs.writeFileSync(root+'/qa/native-completed-view.json',JSON.stringify(evidence,null,2));console.log(JSON.stringify(evidence));
}finally{await m.disconnect()}})().catch(e=>{console.error(e.message);process.exitCode=1});
