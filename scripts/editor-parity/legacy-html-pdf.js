import {chromium} from 'playwright-core';
import fs from 'node:fs/promises';
import {pathToFileURL} from 'node:url';
/** Literal preprocessing from original common.php pdf(); no source document is modified. */
export function legacyPdfHtml(html){
 let body=html.replaceAll('http://vetlab.vettina.com/','http://my.laboklin.cn/')
  .replaceAll('<hr style="page-break-after: always;" class="ke-pagebreak">','<div class="pagenode" style="page-break-after:always;"> ----分页符号----</div > ')
  .replaceAll('----分页符号----','').replaceAll('_ueditor_page_break_tag_','<div class="pagenode" style="page-break-after:always;"></div>')
  .replaceAll('word-break: break-all;','').replaceAll('line-height:114%','').replaceAll('microsoft="" yahei";="" font-size:="" 16px;"=""','').replaceAll('<o:p></o:p>','');
 return `<!doctype html><html><head><meta charset="utf-8"><title>PDF file</title><style>body{margin:0px}p{margin:1px;line-height:20px}table{border-collapse:collapse;word-wrap:break-word}table td{word-break:break-all}tr{page-break-before:always;page-break-after:always;page-break-inside:avoid}</style></head><body>${body}</body></html>`;
}
export async function renderLegacyPdf(html,executablePath){
 if(typeof html!=='string'||!html.trim())throw Error('EMPTY_CONTENT');
 let browser;
 try{
  browser=await chromium.launch({executablePath,headless:true});const context=await browser.newContext({javaScriptEnabled:false,serviceWorkers:'block',acceptDownloads:false});
  let blocked=false;await context.route('**/*',route=>{if(route.request().url().startsWith('data:'))return route.continue();blocked=true;return route.abort();});
  const page=await context.newPage();await page.emulateMedia({media:'screen'});await page.setContent(legacyPdfHtml(html),{waitUntil:'networkidle',timeout:20000});
  const status=await page.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.images].map(image=>image.decode().catch(()=>{})));return {unsupported:!!document.querySelector('script,iframe,frame,object,embed,canvas,video,audio'),missing:[...document.images].some(image=>!image.complete||image.naturalWidth===0)};});
  if(blocked||status.missing)throw Error('MISSING_ASSETS');if(status.unsupported)throw Error('UNSUPPORTED_CONTENT');
  return await page.pdf({format:'A4',margin:{left:'18mm',right:'18mm',top:'10mm',bottom:'10mm'},printBackground:true,preferCSSPageSize:false,timeout:20000});
 }finally{await browser?.close();}
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
 try{const chunks=[];let size=0;for await(const chunk of process.stdin){size+=chunk.length;if(size>2*1024*1024)throw Error('CONTENT_TOO_LARGE');chunks.push(chunk);}const pdf=await renderLegacyPdf(Buffer.concat(chunks).toString('utf8'),process.argv[3]);await fs.writeFile(process.argv[2],pdf,{mode:0o600});}
 catch(error){const known=['EMPTY_CONTENT','MISSING_ASSETS','UNSUPPORTED_CONTENT','CONTENT_TOO_LARGE'];process.stderr.write(known.includes(error.message)?error.message:'PDF_RENDER_FAILED');process.exitCode=1;}
}
