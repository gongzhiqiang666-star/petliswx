// Native list/detail/actions against isolated fixtures; inject HTTP503 only for failed-action recovery.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const automator=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator'),f=JSON.parse(fs.readFileSync(root+'/qa/native-article-fixture.json')),old=JSON.parse(fs.readFileSync(root+'/simulator-fixture.json'));
(async()=>{assert.equal(f.archived,false);const m=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'});let mocked=false;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Native article readiness timeout')};
 try{
 assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),old.account);
 let p=await m.navigateTo('/pages/articles/articles');await wait(async()=>!await p.data('busy')&&(await p.data('categories')).includes(f.category));
 const index=(await p.data('categories')).indexOf(f.category);await(await p.$('.category-picker')).trigger('change',{value:index});await wait(async()=>!await p.data('busy')&&(await p.data('items')).length===2);
 await(await p.$('.card[data-id="'+f.articleIds[0]+'"]')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/article-detail/article-detail'&&!await p.data('busy')&&!!await p.data('article')});
 assert.ok(!await p.data('article.liked')&&!await p.data('article.bookmarked'));
 for(const key of ['liked','bookmarked']){await(await p.$('button[data-key="'+key+'"]')).tap();await wait(async()=>!await p.data('busy')&&Boolean(await p.data('article.'+key)));}
 await m.mockWxMethod('request',{statusCode:503,data:{code:503,message:'本机失败恢复验收'}});mocked=true;
 await(await p.$('button[data-key="liked"]')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('error'));assert.equal(Boolean(await p.data('article.liked')),true);
 await m.restoreWxMethod('request');mocked=false;await(await p.$('button[data-key="liked"]')).tap();await wait(async()=>!await p.data('busy')&&!await p.data('article.liked'));
 p=await m.navigateBack();await wait(async()=>!await p.data('busy')&&(await p.data('items')).some(x=>Number(x.id)===f.articleIds[0]&&Boolean(x.bookmarked)));
 await(await p.$('.page > .row > button')).tap();await wait(async()=>!await p.data('busy')&&await p.data('bookmarked')===true&&(await p.data('items')).length===1);
 await(await p.$('.card[data-id="'+f.articleIds[0]+'"]')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/article-detail/article-detail'&&!await p.data('busy')&&!!await p.data('article')});
 await(await p.$('button[data-key="bookmarked"]')).tap();await wait(async()=>!await p.data('busy')&&!await p.data('article.bookmarked'));
 p=await m.navigateBack();await wait(async()=>!await p.data('busy')&&(await p.data('items')).length===0);assert.equal(await p.data('bookmarked'),true);
 await m.screenshot({path:root+'/qa/native-article-bookmarks-empty.png'});
 await(await p.$('.page > .row > button')).tap();await wait(async()=>!await p.data('busy')&&(await p.data('items')).length===2);
 await(await p.$('.card[data-id="'+f.articleIds[1]+'"]')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/article-detail/article-detail'&&!await p.data('busy')&&!!await p.data('countdown')});
 const first=await p.data('countdown');assert.match(first,/天 .*时 .*分 .*秒/);await new Promise(r=>setTimeout(r,1200));assert.notEqual(await p.data('countdown'),first);
 await m.screenshot({path:root+'/qa/native-article-live-countdown.png'});
 const result={articleIds:f.articleIds,nativeCategoryFiltered:true,likeAndBookmarkPersisted:true,failedLikeInjectedHTTP503:true,failedLikeRetainsState:true,realRetrySucceeded:true,returnListRefreshesBookmark:true,removeBookmarkReturnsEmpty:true,liveCountdownTicks:true,publicationApiTested:false,broadcastOrExternalLiveCalled:false};
 fs.writeFileSync(root+'/qa/native-article-ui.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));await m.switchTab('/pages/home/home');
 }finally{if(mocked)await m.restoreWxMethod('request');await m.disconnect()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
