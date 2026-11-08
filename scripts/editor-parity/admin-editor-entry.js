import {Editor,Mark} from '@tiptap/core';
import {editorConfiguration} from './editor-configuration.js';
import {legacyExtensions} from './legacy-extensions.js';
import {LegacyInline} from './legacy-inline.js';
import {hierarchyContent} from './hierarchy-content.js';
import {assertEditableHtml} from './admin-editor-safety.js';

// New formatting marks coexist with the hierarchy used to preserve imported documents.
const format=(name,tag,style)=>Mark.create({name,priority:20,parseHTML(){return [{tag}];},renderHTML(){return [tag,{style},0];}});
export function mountEditor(target,initialHtml){
 const document=target.ownerDocument;
 assertEditableHtml(document,initialHtml);
 const wrapper=document.createElement('section');wrapper.className='legacy-rich-editor';
 const toolbar=document.createElement('div');toolbar.className='toolbar';toolbar.setAttribute('role','toolbar');toolbar.setAttribute('aria-label','正文编辑工具');
 const surface=document.createElement('div');surface.className='legacy-editor-surface';
 const status=document.createElement('p');status.className='muted';status.setAttribute('role','status');
 wrapper.append(toolbar,surface,status);target.replaceChildren(wrapper);
 const configuration=editorConfiguration([...legacyExtensions(),LegacyInline]);
 configuration.extensions.push(format('userBold','strong','font-weight:bold'),format('userItalic','em','font-style:italic'),format('userUnderline','u','text-decoration:underline'));
 let original=initialHtml,dirty=false;
 const editor=new Editor({...configuration,element:surface,content:hierarchyContent(document,initialHtml,configuration),
  editorProps:{attributes:{'aria-label':'正文内容',role:'textbox','aria-multiline':'true'}},
  onUpdate(){dirty=true;status.textContent='正文已修改';}
 });
 const actions=[['加粗',()=>editor.chain().focus().toggleMark('userBold').run()],['斜体',()=>editor.chain().focus().toggleMark('userItalic').run()],['下划线',()=>editor.chain().focus().toggleMark('userUnderline').run()],['撤销',()=>editor.chain().focus().undo().run()],['重做',()=>editor.chain().focus().redo().run()],['增加行',()=>editor.chain().focus().addRowAfter().run()],['增加列',()=>editor.chain().focus().addColumnAfter().run()]];
 for(const [label,action] of actions){const button=document.createElement('button');button.type='button';button.className='button secondary';button.textContent=label;button.addEventListener('mousedown',event=>event.preventDefault());button.addEventListener('click',()=>{if(!action())status.textContent='请先将光标放在可执行此操作的位置';});toolbar.append(button);}
 const onClick=event=>{if(event.target.closest('a'))event.preventDefault();};
 const onPaste=event=>{const html=event.clipboardData?.getData('text/html');if(html)try{assertEditableHtml(document,html);}catch(error){event.preventDefault();event.stopImmediatePropagation();status.textContent=error.message;}};
 surface.addEventListener('click',onClick);surface.addEventListener('paste',onPaste,true);
 return {
  getHTML(){const html=dirty?editor.getHTML():original;assertEditableHtml(document,html);return html;},
  isDirty(){return dirty;},
  setHTML(html){assertEditableHtml(document,html);editor.commands.setContent(hierarchyContent(document,html,configuration),{emitUpdate:false});original=html;dirty=false;status.textContent='模板已载入';},
  destroy(){surface.removeEventListener('click',onClick);surface.removeEventListener('paste',onPaste,true);editor.destroy();wrapper.remove();}
 };
}
