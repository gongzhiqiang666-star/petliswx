import { field, renderTable } from './ui.js';
import { cell, pageItems, formatDate } from '../utils.js';
import { formData } from '../pages/shared.js';
export function configSnapshot(raw) {
  try {
    const snapshot = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return snapshot?.scopeType === 'GLOBAL' && snapshot.scopeId === 0 && Number.isInteger(snapshot.version) && snapshot.value && typeof snapshot.value === 'object' && !Array.isArray(snapshot.value) && Object.keys(snapshot.value).length ? snapshot : null;
  } catch { return null; }
}
export async function openConfigHistory(context, key, onSaved, page = 0) {
  const data = await context.api.request(`/admin/operations/configs/${encodeURIComponent(key)}/history`, { query: { page, size: 20 } });
  const rows = pageItems(data.history);
  const choices = rows.flatMap(row => ['BEFORE','AFTER'].flatMap(side => {
    const snapshot=configSnapshot(side==='BEFORE'?row.beforeSnapshot:row.afterSnapshot);
    return snapshot ? [{...row,side,snapshot,choiceId:`${row.id}:${side}`}] : [];
  }));
  const current = data.current;
  context.dialog({ title: '配置历史与恢复', wide: true, submitLabel: choices.length ? '恢复为新版本' : undefined,
    body: `<p>配置：${cell({key},'key')} · 当前版本 ${Number(current.version)}。恢复会新增版本，保留历史；不会重放过去的积分、通知或其他业务操作。</p>${renderTable({rows,emptyText:'尚无历史记录',columns:[{label:'记录ID',key:'id'},{label:'操作',key:'action'},{label:'操作人',key:'operatorId'},{label:'时间',render:row=>formatDate(row.createdAt)},{label:'原因',key:'reason'},{label:'快照版本',render:row=>configSnapshot(row.afterSnapshot)?.version ?? '旧记录无快照'}]})}<div class="toolbar"><button type="button" class="button secondary" id="config-history-prev" ${page===0?'disabled':''}>上一页</button><span>第 ${page+1} 页</span><button type="button" class="button secondary" id="config-history-next" ${(page+1)*20>=Number(data.history.total)?'disabled':''}>下一页</button></div>${choices.length ? field({name:'auditId',label:'选择要恢复的历史记录',options:choices.map(row=>({value:row.choiceId,label:`记录 ${row.id} · ${row.side==='BEFORE'?'修改前':'修改后'}版本 ${row.snapshot.version}`})),required:true})+'<h3>当前配置</h3><pre id="config-current-preview" style="white-space:pre-wrap;overflow-wrap:anywhere"></pre><h3>待恢复配置</h3><pre id="config-history-preview" style="white-space:pre-wrap;overflow-wrap:anywhere"></pre>'+field({name:'reason',label:'恢复原因（必填，最多500字）',type:'textarea',required:true,wide:true}) : '<p>本页无可恢复快照。早期未保存快照的记录不能自动恢复。</p>'}`,
    onReady(root) {
      const navigate = next => openConfigHistory(context,key,onSaved,next).catch(error=>context.toast(error.message,'error'));
      root.querySelector('#config-history-prev')?.addEventListener('click',()=>navigate(page-1));
      root.querySelector('#config-history-next')?.addEventListener('click',()=>navigate(page+1));
      const currentPreview=root.querySelector('#config-current-preview');
      if(currentPreview) { const raw=current.config_value ?? current.configValue; try { currentPreview.textContent=JSON.stringify(typeof raw==='string'?JSON.parse(raw):raw,null,2); } catch { currentPreview.textContent='当前配置格式无法预览'; } }
      const select=root.querySelector('[name="auditId"]'), preview=root.querySelector('#config-history-preview');
      if(select && preview) { const update=()=>{const row=choices.find(row=>row.choiceId===select.value);preview.textContent=JSON.stringify(row?.snapshot.value ?? {},null,2)};select.addEventListener('change',update);update(); }
    },
    ...(choices.length ? { async onSubmit(form) {
      const input=formData(form), reason=input.reason.trim();
      if(!reason || reason.length>500) throw new Error('请填写500字以内的恢复原因');
      const selected=choices.find(row=>row.choiceId===input.auditId);
      if(!selected) throw new Error('请选择本页有效快照');
      await context.api.request(`/admin/operations/configs/${encodeURIComponent(key)}/restore`,{method:'POST',data:{auditId:Number(selected.id),snapshotSide:selected.side,version:Number(current.version),reason}});
      context.toast('配置已恢复为新版本');
      onSaved();
    }} : {})
  });
}
