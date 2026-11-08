/** Preserve stored declaration text; ProseMirror's array style attribute uses CSSOM and drops it. */
export function rawDomSpec(tag,attributes={},ownerDocument=document){
 const dom=ownerDocument.createElement(tag);
 for(const [name,value] of Object.entries(attributes))if(value!==null&&value!==undefined&&!/^on/i.test(name)&&name!=='srcdoc')dom.setAttribute(name,String(value));
 return {dom,contentDOM:dom};
}
/** Materialize a regular output spec without the built-in style.cssText assignment. */
export function rawOutputSpec(spec,ownerDocument=document){
 if(typeof spec==='string')return {dom:ownerDocument.createTextNode(spec)};
 if(spec?.nodeType)return {dom:spec};
 if(spec?.dom)return spec;
 if(!Array.isArray(spec))throw new TypeError('Unsupported output spec');
 let offset=1;const attrs=spec[1];
 const hasAttrs=attrs&&typeof attrs==='object'&&!Array.isArray(attrs)&&!attrs.nodeType&&!attrs.dom;
 const {dom}=rawDomSpec(spec[0],hasAttrs?attrs:{},ownerDocument);if(hasAttrs)offset=2;
 let contentDOM;
 for(const child of spec.slice(offset)){
  if(child===0){if(contentDOM)throw new RangeError('Duplicate content hole');contentDOM=dom;continue;}
  const rendered=rawOutputSpec(child,ownerDocument);dom.append(rendered.dom);if(rendered.contentDOM){if(contentDOM)throw new RangeError('Duplicate content hole');contentDOM=rendered.contentDOM;}
 }
 return contentDOM?{dom,contentDOM}:{dom};
}
export function preserveRawRender(extension){return extension.extend({renderHTML(props){return rawOutputSpec(this.parent(props));}});}
