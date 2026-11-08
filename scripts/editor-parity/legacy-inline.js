import {removeIgnoredTableWhitespace} from './ignored-table-whitespace.js';
/** Experimental hierarchy-preserving inline containers. Editing/selection is not yet approved. */
import {Node} from '@tiptap/core';
import {rawDomSpec} from './raw-dom-spec.js';
const tags=['span','strong','b','em','i','font','a','u','s','sub','sup','small','big','code'];
const allowed=new Set(['style','class','href','target','rel','title','face','color','size','align']);
const keepAttribute=attr=>allowed.has(attr.name)||/^(?:data|aria)-[a-z0-9_.:-]+$/i.test(attr.name);
export const LegacyInline=Node.create({
 name:'legacyInline',priority:1000,inline:true,group:'inline',content:'inline*',selectable:false,
 addAttributes(){return {tag:{default:'span',rendered:false},originalAttributes:{default:{},rendered:false}};},
 parseHTML(){return tags.map(tag=>({tag,getAttrs:element=>({tag,originalAttributes:Object.fromEntries([...element.attributes].filter(keepAttribute).map(attr=>[attr.name,attr.value]))})}));},
 renderHTML({node}){return rawDomSpec(tags.includes(node.attrs.tag)?node.attrs.tag:'span',node.attrs.originalAttributes);}
});
const blocks='p,div,table,ul,ol,li,h1,h2,h3,h4,h5,h6,pre,blockquote,hr,section';
export function hasLegacyBlockContent(element){return Boolean(element.querySelector(blocks));}
export const LegacyInlineBlock=Node.create({
 name:'legacyInlineBlock',priority:1100,group:'block',content:'block*',defining:true,
 addAttributes(){return {tag:{default:'span',rendered:false},originalAttributes:{default:{},rendered:false}};},
 parseHTML(){return tags.map(tag=>({tag,getAttrs:element=>hasLegacyBlockContent(element)?{tag,originalAttributes:Object.fromEntries([...element.attributes].filter(keepAttribute).map(attr=>[attr.name,attr.value]))}:false}));},
 renderHTML({node}){return rawDomSpec(tags.includes(node.attrs.tag)?node.attrs.tag:'span',node.attrs.originalAttributes);}
});
/** Fit anonymous inline runs into schema blocks without dropping their original font wrappers. */
export function prepareHierarchyContent(document,html){
 const root=document.createElement('div');root.innerHTML=html;removeIgnoredTableWhitespace(root);
 const containers=[root,...root.querySelectorAll('div,td,th,blockquote'),...root.querySelectorAll(tags.join(','))].filter(element=>element===root||['DIV','TD','TH','BLOCKQUOTE'].includes(element.tagName)||hasLegacyBlockContent(element));
 const blockTags=new Set(blocks.split(',').map(tag=>tag.toUpperCase()));
 for(const container of new Set(containers)){
  let paragraph=null;
  for(const child of [...container.childNodes]){
   const block=child.nodeType===1&&(blockTags.has(child.tagName)||(tags.includes(child.tagName.toLowerCase())&&hasLegacyBlockContent(child)));
   if(block){paragraph=null;continue;}
   if(!paragraph){paragraph=document.createElement('p');paragraph.style.margin='0';container.insertBefore(paragraph,child);}
   paragraph.append(child);
  }
 }
 return root;
}
export const LegacyPre=Node.create({
 name:'legacyPre',priority:100,group:'block',content:'inline*',whitespace:'pre',defining:true,
 addAttributes(){return {originalAttributes:{default:{},rendered:false,parseHTML:element=>Object.fromEntries([...element.attributes].filter(keepAttribute).map(attr=>[attr.name,attr.value]))}};},
 parseHTML(){return [{tag:'pre',preserveWhitespace:'full'}];},
 renderHTML({node}){return rawDomSpec('pre',node.attrs.originalAttributes);}
});
