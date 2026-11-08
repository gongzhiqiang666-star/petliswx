export function createFidelityGate(document){
/** Conservative adoption gate, not a claim of pixel/PDF equivalence. Values stay internal. */
function snapshot(html){
 const root=document.createElement('div');root.innerHTML=html;const counts=new Map();
 function add(kind,tag,name,value){const key=JSON.stringify([kind,tag,name,value]);counts.set(key,(counts.get(key)??0)+1);}
 for(const node of root.querySelectorAll('*')){
  const tag=node.tagName.toLowerCase();add('tag',tag,tag,'');
  for(const attr of node.attributes){
   if(attr.name==='style'){
    // Parse declaration text, retaining legacy/Office properties unsupported by CSSOM too.
    for(const item of attr.value.split(';')){const colon=item.indexOf(':');if(colon<1)continue;const name=item.slice(0,colon).trim().toLowerCase(),value=item.slice(colon+1).trim();if(name&&value){const probe=document.createElement('span');probe.style.setProperty(name,value);const normalized=probe.style.getPropertyValue(name)||value;add('style',tag,name,normalized);}}
   }else if(!(['td','th'].includes(tag)&&['rowspan','colspan'].includes(attr.name)&&Number(attr.value)===1))add('attribute',tag,attr.name,attr.value);
  }
 }
 return {text:root.textContent,counts};
}
function checkFidelity(before,after){
 const a=snapshot(before),b=snapshot(after),missing=[];
 for(const [key,count] of a.counts){const lost=count-(b.counts.get(key)??0);if(lost>0){const [kind,tag,name]=JSON.parse(key);missing.push({kind,tag,name,count:lost});}}
 const textChanged=a.text!==b.text;return {passed:!textChanged&&missing.length===0,textChanged,missing};
}

return checkFidelity;
}
