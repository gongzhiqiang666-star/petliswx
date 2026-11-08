import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import {execFileSync} from 'node:child_process';
import {legacyPdfHtml,renderLegacyPdf} from './legacy-html-pdf.js';
test('PHP PDF preprocessing retains content while translating both legacy page markers',()=>{const result=legacyPdfHtml('<p style="line-height:114%;word-break: break-all;">中文</p>_ueditor_page_break_tag_<o:p></o:p>----分页符号----');assert.match(result,/中文/);assert.match(result,/page-break-after:always/);assert.ok(!result.includes('_ueditor_page_break_tag_'));assert.ok(!result.includes('----分页符号----'));assert.ok(!result.includes('line-height:114%'));});
const enabled=process.env.PETLIS_HTML_PDF_TEST==='true',chrome=process.env.PETLIS_PDF_BROWSER||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
test('actual HTML PDF content retained; table pagination awaits user decision',{skip:!enabled},async()=>{
 const image='data:image/svg+xml;base64,'+Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="60" height="30"><rect width="60" height="30" fill="#cc6600"/></svg>').toString('base64');
 const html=`<h1>Petlis 合成检测报告</h1><p>标本号 PETLIS-001 · 动物：测试猫</p><table border="1" style="width:100%"><tr><td>检测项目</td><td>结果</td></tr><tr><td>合成项目 Synthetic</td><td>12.50 μg/L</td></tr></table><p><img width="60" height="30" src="${image}"></p>_ueditor_page_break_tag_<h2>第二页 Second page</h2><p>报告末尾 END-OF-REPORT</p>`;
 const pdf=await renderLegacyPdf(html,chrome),dir=path.join(os.homedir(),'.local/share/petlis/reports/qa-html-pdf');await fs.mkdir(dir,{recursive:true});const file=path.join(dir,'acceptance.pdf');await fs.writeFile(file,pdf,{mode:0o600});
 const text=execFileSync('pdftotext',[file,'-'],{encoding:'utf8'});for(const value of ['合成检测报告','PETLIS-001','Synthetic','12.50','第二页','END-OF-REPORT'])assert.ok(text.includes(value),`Missing ${value}`);const info=execFileSync('pdfinfo',[file],{encoding:'utf8'});assert.match(info,/595.*841.*pts|595.*842.*pts/);assert.ok(Number(info.match(/Pages:\s+(\d+)/)[1])>=2);
});
test('missing image stops PDF without making even a local network request',{skip:!enabled},async()=>{let requests=0;const server=http.createServer((req,res)=>{requests++;res.end('unexpected');});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));try{await assert.rejects(renderLegacyPdf(`<img src="http://127.0.0.1:${server.address().port}/image.png">`,chrome),/MISSING_ASSETS/);assert.equal(requests,0);await assert.rejects(renderLegacyPdf('<canvas>lost output</canvas>',chrome),/UNSUPPORTED_CONTENT/);await assert.rejects(renderLegacyPdf('<script>document.body.textContent="lost"</script><p>report</p>',chrome),/UNSUPPORTED_CONTENT/);}finally{await new Promise(resolve=>server.close(resolve));}});
