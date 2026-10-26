const request=require('./request')
async function save(form){
 const payload={...form};delete payload._draftId;delete payload._draftVersion
 let id=form._draftId,version=form._draftVersion || 0
 if(id){await request.put('/drafts/'+id,{laboratoryId:form.laboratoryId || null,payload,version});version++}
 else{id=await request.post('/drafts',{laboratoryId:form.laboratoryId || null,payload});version=0}
 const saved={...form,_draftId:id,_draftVersion:version};wx.setStorageSync('order_form',saved);return saved
}
module.exports={save}
