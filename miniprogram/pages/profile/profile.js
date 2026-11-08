const request=require('../../utils/request')
const references=value=>{try{const rows=typeof value==='string'?JSON.parse(value):value;return Array.isArray(rows)?rows:[]}catch(e){return []}}
Page({
 data:{form:{nickname:'',title:'',avatar:'',hospitalId:null,qualificationFiles:[]},files:[],hospitals:[],hospitalName:'',search:'',history:[],busy:false,loaded:false,error:'',errorAction:'load',submitted:false,rejectedDraft:false},
 onLoad(){return this.load()},
 async load(){
  if(this.data.busy)return;this.setData({busy:true,error:'',errorAction:'load'})
  try{
   const [account,history,hospitals]=await Promise.all([request.get('/account'),request.get('/account/profile-changes'),request.get('/auth/hospitals',{},false)])
   let source=account,rejectedDraft=false
   if(history[0] && history[0].status==='rejected')try{const r=typeof history[0].requestedData==='string'?JSON.parse(history[0].requestedData):history[0].requestedData;if(r && typeof r==='object' && !Array.isArray(r)){source={...account,...r};rejectedDraft=true}}catch(e){}
   const files=references(source.qualificationFiles),hospital=hospitals.find(x=>Number(x.hospitalId)===Number(source.hospitalId))
   this.setData({form:{nickname:source.nickname||'',title:source.title||'',avatar:source.avatar||'',hospitalId:source.hospitalId,qualificationFiles:files},files,hospitals,hospitalName:hospital?hospital.hospitalName:Number(source.hospitalId)===Number(account.hospitalId)?account.hospitalName:'医院 #'+source.hospitalId,history,loaded:true,submitted:history.some(x=>x.status==='pending'),rejectedDraft})
  }catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}
 },
 retry(){const action=this.data.errorAction;if(action==='history')return this.loadHistory();if(action==='submit')return this.submit();if(action==='search')return this.searchHospitals();if(action==='avatar')return this.avatar();if(action==='upload')return this.upload();if(action==='preview')return this.previewQualification({currentTarget:{dataset:{index:this.previewIndex}}});return this.load()},
 async refreshHistory(){try{this.setData({history:await request.get('/account/profile-changes')})}catch(e){this.setData({error:e.message,errorAction:'history'})}},
 async loadHistory(){if(this.data.busy)return;this.setData({busy:true,error:'',errorAction:'history'});try{await this.refreshHistory()}finally{this.setData({busy:false})}},
 field(e){if(this.data.busy || this.data.submitted)return;this.setData({['form.'+e.currentTarget.dataset.key]:e.detail.value})},
 search(e){if(this.data.busy)return;this.setData({search:e.detail.value})},
 async searchHospitals(){if(this.data.busy || this.data.submitted)return;this.setData({busy:true,error:'',errorAction:'search'});try{this.setData({hospitals:await request.get('/auth/hospitals',{q:this.data.search},false)})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 hospital(e){if(this.data.busy || this.data.submitted)return;const h=this.data.hospitals[Number(e.detail.value)];if(h)this.setData({'form.hospitalId':h.hospitalId,hospitalName:h.hospitalName})},
 async avatar(){if(this.data.busy || this.data.submitted)return;this.setData({busy:true,error:'',errorAction:'avatar'});try{const {tempFiles}=await wx.chooseMedia({count:1,mediaType:['image'],sizeType:['compressed']});if(!tempFiles || !tempFiles.length)return;if(tempFiles[0].size>2*1024*1024)throw new Error('头像需小于2MB');const url=await request.uploadFile('/user/avatar',tempFiles[0].tempFilePath);this.setData({'form.avatar':url})}catch(e){if(!String(e.errMsg || '').includes('cancel'))this.setData({error:e.message || '头像上传失败'})}finally{this.setData({busy:false})}},
 async upload(){if(this.data.busy || this.data.submitted || this.data.files.length>=5)return;this.setData({busy:true,error:'',errorAction:'upload'});try{const {tempFiles}=await wx.chooseMessageFile({count:5-this.data.files.length,type:'file',extension:['pdf','png','jpg','jpeg']});for(const file of tempFiles||[]){if(file.size>2*1024*1024)throw new Error('文件需小于2MB');const r=await request.uploadFile('/auth/qualifications',file.path);this.setData({files:this.data.files.concat(r.url)})}}catch(e){if(!String(e.errMsg || '').includes('cancel'))this.setData({error:e.message || '上传失败'})}finally{this.setData({busy:false})}},
 async previewQualification(e){
  if(this.data.busy)return;const index=Number(e.currentTarget.dataset.index),url=this.data.files[index];if(!url)return;this.previewIndex=index;this.setData({busy:true,error:'',errorAction:'preview'});
  try{const path=await request.downloadFile(url);if(/\.pdf$/.test(url))await wx.openDocument({filePath:path,fileType:'pdf',showMenu:false});else await wx.previewImage({current:path,urls:[path]})}catch(error){this.setData({error:error.message || '资质预览失败，请重试'})}finally{this.setData({busy:false})}
 },
 remove(e){if(this.data.busy || this.data.submitted)return;this.setData({files:this.data.files.filter((_,i)=>i!==Number(e.currentTarget.dataset.index))})},
 async submit(){
  if(this.data.busy || this.data.submitted || !this.data.loaded)return;this.setData({busy:true,error:'',errorAction:'submit'})
  try{await request.post('/account/profile-changes',{...this.data.form,qualificationFiles:this.data.files});this.setData({submitted:true,rejectedDraft:false});await this.refreshHistory()}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}
 }
})
