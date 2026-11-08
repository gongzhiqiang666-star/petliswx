import {RawParagraph,RawHardBreak,RawImage,RawTableCell} from './raw-layout.js';
import {preserveRawRender} from './raw-dom-spec.js';
import {LegacyInlineBlock,LegacyPre} from './legacy-inline.js';
import StarterKit from '@tiptap/starter-kit';
import {TableKit} from '@tiptap/extension-table';
import {TextStyleKit} from '@tiptap/extension-text-style';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
export function editorConfiguration(extraExtensions=[]){
 const hierarchy=extraExtensions.some(extension=>extension.name==='legacyInline');
 const extras=hierarchy?extraExtensions.filter(extension=>extension.type!=='mark').map(extension=>['legacyDiv','table'].includes(extension.name)?preserveRawRender(extension):extension):extraExtensions;
 const starter=hierarchy?StarterKit.configure({bold:false,italic:false,link:false,underline:false,strike:false,code:false,codeBlock:false,trailingNode:false,paragraph:false,hardBreak:false}):extras.some(extension=>extension.name==='bold')?StarterKit.configure({bold:false}):StarterKit;
 return {injectCSS:false,extensions:[starter,extras.some(extension=>extension.name==='table')?TableKit.configure(hierarchy?{table:false,tableCell:false}:{table:false}):TableKit,...(hierarchy?[]:[TextStyleKit]),hierarchy?RawImage.configure({allowBase64:true,inline:true}):extras.length?Image.configure({allowBase64:true}):Image,TextAlign.configure({types:['heading','paragraph']}),...(hierarchy?[]:[Subscript,Superscript].filter(base=>!extras.some(extension=>extension.name===base.name))),...extras,...(hierarchy?[LegacyInlineBlock,LegacyPre,RawParagraph,RawHardBreak,RawTableCell]:[])],parseOptions:{preserveWhitespace:'full'}};
}
