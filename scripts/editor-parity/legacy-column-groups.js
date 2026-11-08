import {selectedRect} from '@tiptap/pm/tables';
const span=value=>Math.min(1000,Math.max(1,parseInt(value,10)||1));
/** Explicit column edits only. Ordinary typing never rewrites the stored column groups. */
export function changeColumnGroups(source,operation,index,count=1){
 if(!source||(Array.isArray(source)&&!source.length))return source;
 const groups=(Array.isArray(source)?source:[source]).map(group=>{
  const attributes={...group.attributes},columns=[];
  if(group.columns.length)for(const column of group.columns){const attrs={...column};delete attrs.span;for(let i=0;i<span(column.span);i++)columns.push({...attrs});}
  else{for(let i=0;i<span(attributes.span);i++)columns.push({});delete attributes.span;}
  return {attributes,columns};
 });
 const total=groups.reduce((sum,group)=>sum+group.columns.length,0);
 if(index>total||(operation==='delete'&&index>=total))return source;
 if(operation==='insert'){
  let offset=0;for(const group of groups){if(index<=offset+group.columns.length){group.columns.splice(index-offset,0,...Array.from({length:count},()=>({})));break;}offset+=group.columns.length;}
 }else if(operation==='delete'){
  let offset=0;for(const group of groups){const length=group.columns.length,start=Math.max(0,index-offset),end=Math.min(length,index+count-offset);if(end>start)group.columns.splice(start,end-start);offset+=length;}
 }else throw new RangeError('Unknown column operation');
 return groups.filter(group=>group.columns.length);
}
export function columnCommand(factory,operation){
 if(factory.petlisLegacyColumnSync)return factory;
 const wrapped=(...args)=>props=>{
  if(!props.dispatch)return factory(...args)(props);
  let rect;try{rect=selectedRect(props.state);}catch{return factory(...args)(props);}
  const columns=rect.table.attrs.legacyColumns;if(!columns||(Array.isArray(columns)&&!columns.length))return factory(...args)(props);
  const index=operation==='after'?rect.right:rect.left,count=operation==='delete'?rect.right-rect.left:1;
  const next=changeColumnGroups(columns,operation==='delete'?'delete':'insert',index,count);
  return factory(...args)({...props,dispatch:tr=>{
   const mapped=tr.mapping.mapResult(rect.tableStart-1,1),table=mapped.deleted?null:tr.doc.nodeAt(mapped.pos);
   if(table?.type.spec.tableRole==='table'&&next!==columns)tr.setNodeMarkup(mapped.pos,null,{...table.attrs,legacyColumns:next});
   props.dispatch(tr);
  }});
 };
 Object.defineProperty(wrapped,'petlisLegacyColumnSync',{value:true});return wrapped;
}
