import {tableContentText} from './ignored-table-whitespace.js';
import {compareLayouts,layoutSnapshot} from './layout-parity.js';
import {hierarchyContent} from './hierarchy-content.js';
import {textDiagnostic,compareBreakAwareText} from './text-diagnostic.js';
import {LegacyInline} from './legacy-inline.js';
import {Editor} from '@tiptap/core';
import {editorConfiguration} from './editor-configuration.js';
import {legacyExtensions} from './legacy-extensions.js';
import {compareTextFormats} from './text-format.js';
import {createFidelityGate} from './browser-fidelity-gate.js';
const check=createFidelityGate(document);
window.petlisProbe=async (html,hierarchy=false,layout=false)=>{
 let editor;
 try{
  const configuration=editorConfiguration([...legacyExtensions(),...(hierarchy?[LegacyInline]:[])]);
  editor=new Editor({...configuration,element:document.createElement('div'),content:hierarchy?hierarchyContent(document,html,configuration):html});
  const result=editor.getHTML(),structure=check(html,result);let textChange=null;
  if(structure.textChanged){const a=document.createElement('div'),b=document.createElement('div');a.innerHTML=html;b.innerHTML=result;textChange=textDiagnostic(a.textContent,b.textContent);textChange.tableStructuralWhitespaceEquivalent=tableContentText(document,html)===tableContentText(document,result);textChange.breakAwareTextEquivalent=compareBreakAwareText(document,html,result);
   function context(root,offset){const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);let count=0;while(walker.nextNode()){const node=walker.currentNode;const length=Array.from(node.data).length;if(count+length>offset){const path=[];let parent=node.parentElement;while(parent&&parent!==root){path.push(parent.tagName.toLowerCase());parent=parent.parentElement;}return {path,previousSibling:node.previousSibling?.nodeName??null,offsetWithinNode:offset-count,nodeLength:length,siblings:[...node.parentElement.childNodes].slice(0,15).map(child=>({kind:child.nodeType===3?'text':child.nodeName.toLowerCase(),textLength:Array.from(child.textContent??'').length,firstTextChildLength:child.firstChild?.nodeType===3?Array.from(child.firstChild.data).length:null}))};}count+=length;}return [];}
   const parsed=new DOMParser().parseFromString('<body>'+html+'</body>','text/html').body;
   textChange.documentParserTextEqualsFragment=parsed.textContent===a.textContent;
   textChange.documentParserTextLength=Array.from(parsed.textContent).length;
   const preSummary=root=>[...root.querySelectorAll('pre')].map(pre=>({textLength:Array.from(pre.textContent).length,whitespaceOnly:/^\s*$/.test(pre.textContent),children:[...pre.children].map(el=>el.tagName.toLowerCase())}));textChange.sourcePre=preSummary(a);textChange.outputPre=preSummary(b);
   textChange.sourceContext=context(a,textChange.commonPrefixLength);textChange.outputContext=context(b,textChange.commonPrefixLength);}
  return {success:true,...structure,contentTextChanged:tableContentText(document,html)!==tableContentText(document,result),textChange,textFormats:compareTextFormats(document,html,result),layout:layout?await compareLayouts(document,html,result):null};
 }catch(error){return {success:false,errorName:['TypeError','RangeError','Error'].includes(error.name)?error.name:'Other'};}
 finally{editor?.destroy();}
};
let liveMount=null;
window.petlisUnmountEditor=()=>{window.petlisLiveEditor?.destroy();window.petlisLiveEditor=null;liveMount?.remove();liveMount=null;};
window.petlisMountEditor=html=>{
 window.petlisUnmountEditor();liveMount=document.createElement('div');liveMount.id='petlis-editor-smoke';document.body.append(liveMount);
 const configuration=editorConfiguration([...legacyExtensions(),LegacyInline]);
 window.petlisLiveEditor=new Editor({...configuration,element:liveMount,content:hierarchyContent(document,html,configuration)});
};

window.petlisCompareLayouts=(before,after)=>compareLayouts(document,before,after);
window.petlisSnapshotLayout=html=>layoutSnapshot(document,html);
