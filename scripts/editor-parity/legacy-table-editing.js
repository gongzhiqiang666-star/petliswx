import {Plugin,NodeSelection} from '@tiptap/pm/state';
import {TableMap,tableEditingKey} from '@tiptap/pm/tables';
/** Imported irregular layouts are intentional legacy content, not an invitation to add cells. */
export function changedLegacyTableNeedsProtection(before,after){
 if(before===after)return false;const original=new Set();before.descendants(node=>{if(node.type.spec.tableRole==='table')original.add(node);});
 let protectedTable=false;after.descendants(node=>{
  if(node.type.spec.tableRole!=='table'||node.attrs.legacyColumns===null||original.has(node))return;
  try{if(TableMap.get(node).problems?.length)protectedTable=true;}catch{protectedTable=true;}
 });return protectedTable;
}
export function protectLegacyTableEditing(plugin){
 if(plugin.spec.petlisLegacyPreserved||plugin.spec.key!==tableEditingKey||!plugin.spec.appendTransaction)return plugin;
 return new Plugin({...plugin.spec,petlisLegacyPreserved:true,appendTransaction(transactions,before,after){
  if(after.selection instanceof NodeSelection){const node=after.selection.node,role=node.type.spec.tableRole;if(role==='row'&&!node.childCount)return null;if(role==='table'){try{const map=TableMap.get(node);if(!map.width||!map.height)return null;}catch{return null;}}}
  // In pinned prosemirror-tables, an equal doc baseline skips fixTables, while the
  // original normalizeSelection and every event/selection handler still run.
  const baseline=changedLegacyTableNeedsProtection(before.doc,after.doc)?Object.create(before,{doc:{value:after.doc}}):before;
  return plugin.spec.appendTransaction.call(this,transactions,baseline,after);
 }});
}
