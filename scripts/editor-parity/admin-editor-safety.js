/** Refuse unsafe content rather than silently sanitizing away report information. */
export function assertEditableHtml(document,html){
 const root=document.createElement('template');root.innerHTML=html;
 for(const element of root.content.querySelectorAll('*')){
  if(['SCRIPT','IFRAME','OBJECT','EMBED','FORM','INPUT','BUTTON','LINK','STYLE','META','BASE','SVG','MATH','TEMPLATE'].includes(element.tagName))throw Error('该正文含暂不能安全编辑的内容，原文保留，请先核对差异');
  for(const attr of element.attributes){
   const value=attr.value.replace(/[\u0000-\u0020]/g,'').toLowerCase();
   if(/^on/i.test(attr.name)||['srcdoc','srcset','formaction'].includes(attr.name)||(['href','src','xlink:href'].includes(attr.name)&&/^(?:javascript|vbscript|data:text\/html):/.test(value))||(attr.name==='style'&&/(?:url\s*\(|expression\s*\(|@import|behavior\s*:)/i.test(attr.value)))throw Error('该正文含暂不能安全编辑的内容，原文保留，请先核对差异');
  }
 }
 return html;
}
