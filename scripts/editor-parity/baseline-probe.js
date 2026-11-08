/** Offline schema probe. No assets loaded, no original report text leaves this process. */
import { JSDOM } from 'jsdom';
const dom=new JSDOM('<!doctype html><html><body></body></html>');
for(const key of ['window','document','DOMParser','Node','HTMLElement','MutationObserver','getComputedStyle'])globalThis[key]=dom.window[key];
Object.defineProperty(globalThis,'navigator',{value:dom.window.navigator,configurable:true});
const {Editor}=await import('@tiptap/core');
const {hierarchyContent}=await import('./hierarchy-content.js');
const {editorConfiguration}=await import('./editor-configuration.js');
export function roundTrip(html,extraExtensions=[]){
 const configuration=editorConfiguration(extraExtensions);
 const editor=new Editor({...configuration,element:document.createElement('div'),content:extraExtensions.some(extension=>extension.name==='legacyInline')?hierarchyContent(document,html,configuration):html});
 const result=editor.getHTML();editor.destroy();return result;
}
export function structure(html){
 const root=document.createElement('div');root.innerHTML=html;
 return [...root.querySelectorAll('*')].map(el=>({tag:el.tagName.toLowerCase(),attrs:Object.fromEntries([...el.attributes].map(a=>[a.name,a.value]))}));
}
if(process.argv[1]?.endsWith('baseline-probe.js')){
 const source='<div style="page-break-after:always"><table width="640" cellpadding="3" cellspacing="0" style="border-collapse:collapse"><colgroup><col width="120"><col width="520"></colgroup><tbody><tr height="40"><td rowspan="2" width="120" valign="top" style="height:40px;line-height:24px;border-bottom:1px solid #000">{宠物姓名}</td><td colspan="2"><p style="margin:0;text-indent:2em;letter-spacing:1px"><font face="宋体" color="#123456">示例报告</font></p></td></tr><tr><td><span style="font-size:14px;color:#ff0000">保留内容</span></td></tr></tbody></table></div><p>----分页符号----</p>';
 const output=roundTrip(source);console.log(JSON.stringify({scope:'synthetic structural fixture; not production HTML',input:structure(source),output:structure(output),textRetained:output.includes('示例报告')&&output.includes('保留内容')&&output.includes('{宠物姓名}'),pageMarkerRetained:output.includes('----分页符号----')},null,2));
}
