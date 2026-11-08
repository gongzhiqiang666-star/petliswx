/** Controlled-canvas diagnostics. Not a replacement for print/PDF or external-asset verification. */
const dimensions=['x','y','width','height'];
const styleProperties=['display','position','float','verticalAlign','borderTopWidth','borderRightWidth','borderBottomWidth','borderLeftWidth','borderTopStyle','borderRightStyle','borderBottomStyle','borderLeftStyle','borderTopColor','borderRightColor','borderBottomColor','borderLeftColor','borderCollapse','borderSpacing','paddingTop','paddingRight','paddingBottom','paddingLeft','marginTop','marginRight','marginBottom','marginLeft','breakBefore','breakAfter','breakInside'];
export async function layoutSnapshot(document,html,width=800){
 const root=document.createElement('div');root.innerHTML=html;root.style.cssText=`position:absolute;left:0;top:0;width:${width}px;visibility:hidden;display:flow-root`;
 document.body.append(root);let timer;
 try{
  const images=[...root.querySelectorAll('img')];let assetsSettled=true;
  const loading=Promise.all(images.map(image=>image.complete?Promise.resolve():new Promise(resolve=>{image.addEventListener('load',resolve,{once:true});image.addEventListener('error',resolve,{once:true});})));
  await Promise.race([loading,new Promise(resolve=>{timer=setTimeout(()=>{assetsSettled=false;resolve();},2000);})]);clearTimeout(timer);
  await document.fonts.ready;
  const origin=root.getBoundingClientRect();
  const nodes=[...root.querySelectorAll('table,tr,td,th,img,pre,hr,[style*="page-break"],[style*="break-after"],[style*="break-before"]')].map(element=>{
   const rect=element.getBoundingClientRect(),style=document.defaultView.getComputedStyle(element);
   return {tag:element.tagName.toLowerCase(),x:rect.x-origin.x,y:rect.y-origin.y,width:rect.width,height:rect.height,styles:Object.fromEntries(styleProperties.map(name=>[name,style[name]]))};
  });
  return {assetsSettled,externalImagesDeferred:images.filter(image=>!image.getAttribute('src')?.startsWith('data:')).length,dataImagesFailed:images.filter(image=>image.getAttribute('src')?.startsWith('data:')&&image.naturalWidth===0).length,root:{width:origin.width,height:origin.height,scrollWidth:root.scrollWidth,scrollHeight:root.scrollHeight},nodes};
 }finally{clearTimeout(timer);root.remove();}
}
export function compareLayoutRecords(before,after,tolerance=.25){
 const rootChanged=Object.keys(before.root).some(name=>Math.abs(before.root[name]-after.root[name])>tolerance),nodeCountChanged=before.nodes.length!==after.nodes.length;
 const properties={},samples=[];let changedNodes=0;
 for(let index=0;index<Math.min(before.nodes.length,after.nodes.length);index++){
  const a=before.nodes[index],b=after.nodes[index],changed=[];
  if(a.tag!==b.tag)changed.push('tag');
  for(const name of dimensions)if(Math.abs(a[name]-b[name])>tolerance)changed.push(name);
  for(const name of new Set([...Object.keys(a.styles),...Object.keys(b.styles)]))if(a.styles[name]!==b.styles[name])changed.push(name);
  if(changed.length){changedNodes++;for(const name of changed)properties[name]=(properties[name]??0)+1;if(samples.length<5)samples.push({index,tag:a.tag,properties:changed,delta:Object.fromEntries(dimensions.filter(name=>changed.includes(name)).map(name=>[name,b[name]-a[name]]))});}
 }
 const assetsSettled=before.assetsSettled&&after.assetsSettled;
 const counts=nodes=>nodes.reduce((result,node)=>{result[node.tag]=(result[node.tag]??0)+1;return result;},{});
 return {beforeNodeCounts:counts(before.nodes),afterNodeCounts:counts(after.nodes),equal:assetsSettled&&!rootChanged&&!nodeCountChanged&&!changedNodes,assetsSettled,nodeCountChanged,rootChanged,changedNodes,properties,samples,externalImagesDeferred:before.externalImagesDeferred,dataImagesFailed:before.dataImagesFailed+after.dataImagesFailed};
}
export async function compareLayouts(document,before,after,width=800){return compareLayoutRecords(await layoutSnapshot(document,before,width),await layoutSnapshot(document,after,width));}
