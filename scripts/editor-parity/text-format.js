import {isIgnoredTableWhitespace} from './ignored-table-whitespace.js';
/** Supplemental diagnostic: not a replacement for layout, table, image and PDF checks. */
const properties=['font-family','font-size','font-weight','font-style','color','letter-spacing','line-height','text-decoration-line','vertical-align','white-space'];
function runs(document,html){
 const root=document.createElement('div');root.innerHTML=html;
 root.style.cssText='position:absolute;left:-100000px;top:0;visibility:hidden;width:800px';document.body.append(root);
 try{
  const walker=document.createTreeWalker(root,document.defaultView.NodeFilter.SHOW_TEXT),items=[];
  while(walker.nextNode()){
   const text=walker.currentNode;if(!text.data||isIgnoredTableWhitespace(text))continue;
   const css=document.defaultView.getComputedStyle(text.parentElement);
   function inlineAlignment(element){const values=[];while(element&&element!==root){const style=document.defaultView.getComputedStyle(element),display=style.display||(['SPAN','STRONG','B','EM','I','A','FONT','CODE','SUB','SUP','U','S','SMALL','BIG'].includes(element.tagName)?'inline':'block');if(!display.startsWith('inline'))break;const alignment=style.verticalAlign;if(alignment&&!['baseline','0px'].includes(alignment))values.push(alignment);element=element.parentElement;}return JSON.stringify(values);}
   function decoration(element){const lines=new Set();while(element&&element!==root){const style=document.defaultView.getComputedStyle(element),value=style.textDecorationLine||style.textDecoration||'';for(const line of value.split(/\s+/))if(['underline','overline','line-through'].includes(line))lines.add(line);if(['inline-block','inline-flex','inline-grid'].includes(style.display))break;element=element.parentElement;}return [...lines].sort().join(' ');}
   const format=JSON.stringify(properties.map(name=>name==='vertical-align'?inlineAlignment(text.parentElement):name==='text-decoration-line'?decoration(text.parentElement):css.getPropertyValue(name)));
   const item={text:text.data,format};const context=[];let parent=text.parentElement;while(parent&&parent!==root&&context.length<6){const style=document.defaultView.getComputedStyle(parent);const bucket=value=>['Arial','Times','Tahoma','Microsoft','YaHei','微软雅黑','monospace'].filter(name=>value.toLowerCase().includes(name.toLowerCase())).join('|')||(value===''?'empty':'other');context.push({tag:parent.tagName.toLowerCase(),computedFont:bucket(style.fontFamily),ownFont:bucket(parent.style.fontFamily),display:style.display,children:[...parent.childNodes].slice(0,12).map(child=>child.nodeType===3?'#text':child.nodeName.toLowerCase())});parent=parent.parentElement;}Object.defineProperty(item,'context',{value:context});items.push(item);
  }
  return items;
 }finally{root.remove();}
}
export function compareTextFormats(document,before,after){
 const a=runs(document,before),b=runs(document,after);
 function projection(items,index){const result=[];for(const item of items){const value=JSON.parse(item.format)[index],prior=result.at(-1);if(prior?.value===value)prior.text+=item.text;else result.push({text:item.text,value});}return JSON.stringify(result);}
 const changedProperties=properties.filter((name,index)=>projection(a,index)!==projection(b,index));
 const transitions=new Map();let fontSample=null;
 if(a.map(item=>item.text).join('')===b.map(item=>item.text).join('')){
  let i=0,j=0,ai=0,bj=0;
  while(i<a.length&&j<b.length){const count=Math.min(a[i].text.length-ai,b[j].text.length-bj),av=JSON.parse(a[i].format),bv=JSON.parse(b[j].format);
   for(const property of ['color','font-weight','font-size','line-height','font-family']){const index=properties.indexOf(property);if(av[index]!==bv[index]){
    const safe=value=>property==='font-family'?(['initial','inherit','unset','revert','Microsoft','YaHei','Times','Tahoma','Arial','Times New Roman','Courier New','SimSun','宋体','Calibri','Microsoft YaHei','微软雅黑','Verdana','Helvetica','monospace','sans-serif','serif'].filter(name=>value.toLowerCase().includes(name.toLowerCase())).join('|')||(value===''?'empty':'other')):/^(?:rgba?\([\d\s.,%]+\)|[\d.]+(?:px)?|normal|bold|bolder)$/.test(value)?value:'other';
    const key=JSON.stringify([property,safe(av[index]),safe(bv[index])]);if(property==='font-family'&&!fontSample&&/\S/.test(a[i].text.slice(ai,ai+count)))fontSample={before:a[i].context,after:b[j].context};const prior=transitions.get(key)??{characters:0,nonWhitespaceCharacters:0};prior.characters+=count;prior.nonWhitespaceCharacters+=a[i].text.slice(ai,ai+count).replace(/\s/g,'').length;transitions.set(key,prior);
   }}ai+=count;bj+=count;if(ai===a[i].text.length){i++;ai=0;}if(bj===b[j].text.length){j++;bj=0;}
  }
 }
 return {fontSample,transitions:[...transitions].map(([key,counts])=>{const [property,before,after]=JSON.parse(key);return {property,before,after,...counts};}),equivalent:changedProperties.length===0,beforeRuns:a.length,afterRuns:b.length,changedProperties};
}
