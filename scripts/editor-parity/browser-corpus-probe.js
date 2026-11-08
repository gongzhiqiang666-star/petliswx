/** Dedicated headless Chrome, private temporary profile; never controls existing browsers. */
import fs from 'node:fs';
import {layoutSmoke} from './layout-smoke.js';
import {editingSmoke} from './editing-smoke.js';
import http from 'node:http';
import {fileURLToPath} from 'node:url';
import {build} from 'esbuild';
import {chromium} from 'playwright-core';
const editMode=process.argv.includes('--editing-smoke');
const layoutSmokeMode=process.argv.includes('--layout-smoke'),layoutMode=layoutSmokeMode||process.argv.includes('--layout');
const input=(editMode||layoutSmokeMode)?[]:JSON.parse(fs.readFileSync(0,'utf8'));
const caseArg=process.argv.find(arg=>arg.startsWith('--case-index='));const caseIndex=caseArg?Number(caseArg.split('=')[1]):null;
if(caseArg&&(!Number.isInteger(caseIndex)||caseIndex<0||caseIndex>=input.length))throw new RangeError('Invalid corpus case index');
const compiled=await build({entryPoints:[fileURLToPath(new URL('./browser-entry.js',import.meta.url))],bundle:true,write:false,format:'iife',platform:'browser',logLevel:'silent'});
const bundle=compiled.outputFiles[0].text;
const server=http.createServer((req,res)=>{
 res.setHeader('Content-Security-Policy',`default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; img-src ${layoutMode?'data:':"'none'"}; connect-src 'none'; font-src 'none'; frame-src 'none'; object-src 'none'`);
 if(req.url==='/probe.js'){res.setHeader('Content-Type','text/javascript');res.end(bundle);}
 else if(req.url==='/'){res.setHeader('Content-Type','text/html');res.end('<!doctype html><html><body><script src="/probe.js"></script></body></html>');}
 else{res.statusCode=404;res.end();}
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
let browser;
const totals={documents:input.length,evaluated:0,passed:0,rejected:0,errors:0,textChanged:0,contentTextChanged:0,tableWhitespaceOnlyChanges:0,textFormatsEquivalent:0,textFormatsChanged:0,structurePassedButTextFormatsChanged:0,layoutChecked:0,layoutEquivalent:0,layoutChanged:0,layoutPending:0,externalImagesDeferred:0,dataImagesFailed:0},missing=new Map(),formatChanges=new Map(),formatTransitions=new Map(),formatTransitionNonWhitespace=new Map(),textChanges=[],fontSamples=[],layoutSamples=[],layoutProperties=new Map(),errorSummary=[];let blockedRequests=0;
try{
 browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const context=await browser.newContext({serviceWorkers:'block'});const origin='http://127.0.0.1:'+server.address().port;
 await context.route('**/*',route=>{if([origin+'/',origin+'/probe.js'].includes(route.request().url()))return route.continue();blockedRequests++;return route.abort();});
 const page=await context.newPage();await page.goto(origin+'/');await page.waitForFunction(()=>typeof window.petlisProbe==='function');
 let editingProof=null;if(editMode)editingProof=await editingSmoke(page);if(layoutSmokeMode)editingProof=await layoutSmoke(page);
 for(let index=0;index<input.length;index++){
  if(caseIndex!==null&&index!==caseIndex)continue;
  if(process.argv.includes('--only-errors')&&![655,662].includes(index))continue;
  if(process.argv.includes('--only-text')&&index!==284)continue;
  totals.evaluated++;const result=await page.evaluate(({html,hierarchy,layout})=>window.petlisProbe(html,hierarchy,layout),{html:input[index],hierarchy:process.argv.includes('--hierarchy'),layout:layoutMode});
  if(!result.success){totals.errors++;errorSummary.push({documentIndex:index,errorName:result.errorName});continue;}
  if(result.layout){totals.layoutChecked++;totals[result.layout.equal?'layoutEquivalent':'layoutChanged']++;if(!result.layout.assetsSettled)totals.layoutPending++;totals.externalImagesDeferred+=result.layout.externalImagesDeferred;totals.dataImagesFailed+=result.layout.dataImagesFailed;
   for(const [name,count] of Object.entries(result.layout.properties))layoutProperties.set(name,(layoutProperties.get(name)??0)+count);
   if(!result.layout.equal&&layoutSamples.length<5)layoutSamples.push({documentIndex:index,...result.layout});
  }
  if(result.contentTextChanged)totals.contentTextChanged++;if(result.textChanged&&!result.contentTextChanged)totals.tableWhitespaceOnlyChanges++;if(result.textFormats.fontSample&&fontSamples.length<3)fontSamples.push({documentIndex:index,...result.textFormats.fontSample});for(const item of result.textFormats.transitions){const key=[item.property,item.before,item.after].join(' → ');formatTransitions.set(key,(formatTransitions.get(key)??0)+item.characters);formatTransitionNonWhitespace.set(key,(formatTransitionNonWhitespace.get(key)??0)+item.nonWhitespaceCharacters);}totals[result.passed?'passed':'rejected']++;totals[result.textFormats.equivalent?'textFormatsEquivalent':'textFormatsChanged']++;for(const name of result.textFormats.changedProperties)formatChanges.set(name,(formatChanges.get(name)??0)+1);if(result.passed&&!result.textFormats.equivalent)totals.structurePassedButTextFormatsChanged++;if(result.textChanged){totals.textChanged++;textChanges.push({documentIndex:index,...result.textChange});}
  for(const item of result.missing){const key=[item.kind,item.tag,item.name].join('.');missing.set(key,(missing.get(key)??0)+item.count);}
 }
 if(editMode||layoutSmokeMode)console.log(JSON.stringify({...editingProof,browserVersion:browser.version(),blockedRequests},null,2));else console.log(JSON.stringify({scope:'read-only local templates; isolated real Chrome; network denied except local harness',editor:process.argv.includes('--hierarchy')?'Tiptap 3.33.0 experimental inline hierarchy':'Tiptap 3.33.0 experimental legacy compatibility',adoptionApproved:false,browserVersion:browser.version(),blockedRequests,layoutMode,canvasWidth:layoutMode?800:null,externalAssetsPermitted:false,...totals,errorSummary,layoutSamples,layoutProperties:Object.fromEntries(layoutProperties),textChanges,fontSamples,formatTransitions:Object.fromEntries(formatTransitions),formatTransitionNonWhitespace:Object.fromEntries(formatTransitionNonWhitespace),formatChanges:Object.fromEntries(formatChanges),missing:Object.fromEntries([...missing].sort((a,b)=>b[1]-a[1]))},null,2));
}finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
