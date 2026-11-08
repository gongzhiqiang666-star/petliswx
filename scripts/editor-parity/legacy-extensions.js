import {columnCommand} from './legacy-column-groups.js';
import {protectLegacyTableEditing} from './legacy-table-editing.js';
/** Compatibility extensions under validation only; not enabled in product saving. */
import {Extension,Mark,Node} from '@tiptap/core';
import {Table} from '@tiptap/extension-table';
import Bold from '@tiptap/extension-bold';
import Superscript from '@tiptap/extension-superscript';
import Subscript from '@tiptap/extension-subscript';
const preserved=['style','class','align','width','height','valign','border','cellpadding','cellspacing','size','title','_src','x:num','x:str'];
function attributes(names){return Object.fromEntries(names.map(name=>[name,{default:null,parseHTML:element=>element.getAttribute(name),renderHTML:attrs=>attrs[name]===null?{}:{[name]:attrs[name]}}]));}
const Layout=Extension.create({name:'legacyLayout',addGlobalAttributes(){return [{types:['paragraph','heading','table','tableRow','tableCell','tableHeader','image','horizontalRule','hardBreak','bold','italic','link','textStyle'],attributes:attributes(preserved)}];}});
const Font=Mark.create({name:'legacyFont',addAttributes(){return attributes(['face','color','size','style','class']);},parseHTML(){return [{tag:'font'}];},renderHTML({HTMLAttributes}){return ['font',HTMLAttributes,0];}});
const Block=Node.create({name:'legacyDiv',group:'block',content:'block*',defining:true,addAttributes(){return attributes(['style','class','title']);},parseHTML(){return [{tag:'div'}];},renderHTML({HTMLAttributes}){return ['div',HTMLAttributes,0];}});
const LegacyTable=Table.extend({
 content:'tableRow*',
 addCommands(){const parent=this.parent();return {...parent,addColumnBefore:columnCommand(parent.addColumnBefore,'before'),addColumnAfter:columnCommand(parent.addColumnAfter,'after'),deleteColumn:columnCommand(parent.deleteColumn,'delete')};},
 addProseMirrorPlugins(){return this.parent().map(protectLegacyTableEditing);},
 addNodeView(){const standard=this.parent?.();return props=>props.node.attrs.legacyColumns!==null?null:standard?standard(props):null;},
 addAttributes(){return {...this.parent?.(),legacyColumns:{default:null,rendered:false,parseHTML:element=>{
  const groups=[...element.children].filter(child=>child.tagName==='COLGROUP');if(!groups.length)return [];
  const keep=element=>Object.fromEntries([...element.attributes].filter(a=>['style','width','span','class'].includes(a.name)).map(a=>[a.name,a.value]));
  return groups.map(group=>({attributes:keep(group),columns:[...group.children].filter(child=>child.tagName==='COL').map(keep)}));
 }}};},
 renderHTML(props){const columns=props.node.attrs.legacyColumns;if(!columns)return this.parent?.(props);
  const groups=Array.isArray(columns)?columns:[columns];return ['table',props.HTMLAttributes,...groups.map(group=>['colgroup',group.attributes,...group.columns.map(attrs=>['col',attrs])]),['tbody',0]];
 }
});
const LegacyBold=Bold.extend({
 addAttributes(){return {...this.parent?.(),legacyAncestors:{default:[],rendered:false,parseHTML:element=>{
  const ancestors=[];let parent=element.parentElement;
  while(parent){if(['STRONG','B'].includes(parent.tagName))ancestors.push(Object.fromEntries([...parent.attributes].filter(a=>preserved.includes(a.name)).map(a=>[a.name,a.value])));parent=parent.parentElement;}return ancestors;
 }}};},
 parseHTML(){return this.parent().filter(rule=>!rule.style||rule.clearMark);},
 renderHTML({mark,HTMLAttributes}){let spec=['strong',HTMLAttributes,0];for(const attrs of mark.attrs.legacyAncestors??[])spec=['strong',attrs,spec];return spec;}
});
const LegacySuperscript=Superscript.extend({parseHTML(){return this.parent().filter(rule=>!rule.style);}});
const LegacySubscript=Subscript.extend({parseHTML(){return this.parent().filter(rule=>!rule.style);}});
export function legacyExtensions(){return [Layout,Font,Block,LegacyTable,LegacyBold,LegacySuperscript,LegacySubscript];}
