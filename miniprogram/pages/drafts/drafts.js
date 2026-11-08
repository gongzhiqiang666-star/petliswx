const request=require('../../utils/request')
const legacyBase='/orders/hospital-history'
const choiceFields={variety:'AnimalTypeId',sex:'DictSexId',sampleType:'InfoSampleTypeId'}
function noteText(html){return String(html||'').replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi,'').replace(/<img\b[^>]*>/gi,'[图片]').replace(/<\/?(?:p|div|br|li|tr)\b[^>]*>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&#39;/g,"'").replace(/&amp;/g,'&').trim()}
Page({
 data:{items:[],busy:false,error:'',legacyTextFields:[{key:'MasterName',label:'畜主姓名',max:250},{key:'AnimalName',label:'动物姓名',max:250},{key:'Age',label:'动物年龄',max:50},{key:'DoctorName',label:'送检医师',max:100},{key:'SampleArea',label:'采样部位',max:50}],legacyChoiceKinds:[{kind:'variety',label:'动物品种'},{kind:'sex',label:'动物性别'},{kind:'sampleType',label:'采样类型'}],legacyEnabled:false,legacyItems:[],legacyPage:0,legacyMore:false,legacyBusy:false,legacyError:'',legacyOrderId:null,legacyFields:{},legacyWritable:false,legacyRemarkText:'',legacySaving:false,legacyChoices:{variety:[],sex:[],sampleType:[]},legacyChoiceIndex:{variety:0,sex:0,sampleType:0},legacyChoiceQuery:{variety:'',sex:'',sampleType:''},legacyChoicePage:{variety:0,sex:0,sampleType:0},legacyChoiceMore:{variety:false,sex:false,sampleType:false}},
 onShow(){this._legacyUnloaded=false;this.load();this.loadLegacy()},
 onUnload(){this._legacyUnloaded=true;this._legacyVersion=(this._legacyVersion||0)+1;this._legacyListVersion=(this._legacyListVersion||0)+1},
 async load(){this.setData({busy:true,error:''});try{this.setData({items:await request.get('/drafts')})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 async open(e){if(this.data.busy)return;this.setData({busy:true,error:''});try{const d=await request.get('/drafts/'+e.currentTarget.dataset.id);const payload=typeof d.payload==='string'?JSON.parse(d.payload):d.payload;const form={...payload,_draftId:d.id,_draftVersion:d.version};wx.setStorageSync('order_form',form);wx.navigateTo({url:'/pages/order-add-pet/order-add-pet?draft=1&petId='+form.petId})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 async remove(e){const c=await wx.showModal({title:'删除这份草稿？',content:'仅删除未提交资料，不影响送检订单。'});if(!c.confirm)return;this.setData({busy:true,error:''});try{await request.delete('/drafts/'+e.currentTarget.dataset.id)}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}this.load()},
 async loadLegacy(append=false){
  if(typeof append!=='boolean')append=false;if(append&&(this.data.legacyBusy||!this.data.legacyMore))return;
  const version=(this._legacyListVersion||0)+1;this._legacyListVersion=version;this.setData({legacyBusy:true,legacyError:''});
  try{const caps=await request.get(legacyBase+'/capabilities');if(version!==this._legacyListVersion)return;
   this.setData({legacyEnabled:caps.legacyHospitalDrafts===true});if(caps.legacyHospitalDrafts!==true){this.setData({legacyItems:[],legacyMore:false});return}
   const hospitalId=getApp().getHospitalId();if(!hospitalId)throw Error('账号未关联医院');const page=append?this.data.legacyPage+1:0;
   const result=await request.get(legacyBase+'/drafts',{hospitalId,page,size:10});if(version!==this._legacyListVersion)return;
   const items=append?[...this.data.legacyItems,...result.items]:result.items;this.setData({legacyItems:items,legacyPage:page,legacyMore:items.length<Number(result.total)});
  }catch(error){if(version===this._legacyListVersion)this.setData({legacyError:error.message})}finally{if(version===this._legacyListVersion)this.setData({legacyBusy:false})}
 },
 moreLegacy(){return this.loadLegacy(true)},
 async openLegacy(event){
  const id=Number(event.currentTarget.dataset.id),version=(this._legacyVersion||0)+1;this._legacyVersion=version;this.setData({legacyBusy:true,legacyError:'',legacyOrderId:null,legacyWritable:false});
  try{const detail=await request.get(legacyBase+'/drafts/'+id,{hospitalId:getApp().getHospitalId()});if(version!==this._legacyVersion)return;
   this._legacyOriginal={...detail.fields};this._legacyToken=detail.editToken;this._legacyPending=null;
   const choices={},indexes={};for(const[kind,field]of Object.entries(choiceFields)){choices[kind]=[{id:String(detail.fields[field]||0),name:detail.labels[field]||'原选择'}];indexes[kind]=0}
   this.setData({legacyOrderId:id,legacyFields:{...detail.fields},legacyWritable:detail.writable===true,legacyRemarkText:noteText(detail.fields.Remark),legacyChoices:choices,legacyChoiceIndex:indexes,legacyChoiceQuery:{variety:'',sex:'',sampleType:''}});
   if(detail.writable)await Promise.all(Object.keys(choiceFields).map(kind=>this.loadLegacyChoices(kind)));return true;
  }catch(error){if(version===this._legacyVersion)this.setData({legacyError:error.message});return false}finally{if(version===this._legacyVersion)this.setData({legacyBusy:false})}
 },
 closeLegacy(){if(this.data.legacySaving)return;this._legacyVersion=(this._legacyVersion||0)+1;this.setData({legacyOrderId:null,legacyBusy:false,legacyError:''})},
 legacyInput(event){const key=event.currentTarget.dataset.key;if(['MasterName','AnimalName','Age','DoctorName','SampleArea','CollectDt'].includes(key))this.setData({['legacyFields.'+key]:event.detail.value})},
 legacyChoiceQuery(event){const kind=event.currentTarget.dataset.kind;if(choiceFields[kind])this.setData({['legacyChoiceQuery.'+kind]:event.detail.value})},
 searchLegacyChoices(event){return this.loadLegacyChoices(event.currentTarget.dataset.kind)},
 moreLegacyChoices(event){return this.loadLegacyChoices(event.currentTarget.dataset.kind,true)},
 async loadLegacyChoices(kind,append=false){
  if(!choiceFields[kind]||!this.data.legacyOrderId)return;const version=this._legacyVersion;
  this._choiceVersions=this._choiceVersions||{};const sequence=(this._choiceVersions[kind]||0)+1;this._choiceVersions[kind]=sequence;
  const page=append?this.data.legacyChoicePage[kind]+1:0;
  try{const result=await request.get(legacyBase+'/draft-choices/'+kind,{hospitalId:getApp().getHospitalId(),q:this.data.legacyChoiceQuery[kind],page,size:50});if(version!==this._legacyVersion||sequence!==this._choiceVersions[kind])return;
   const selected=String(this.data.legacyFields[choiceFields[kind]]||0),previous=this.data.legacyChoices[kind].find(row=>String(row.id)===selected)||{id:selected,name:'原选择'};
   const choices=append?[...this.data.legacyChoices[kind]]:[previous];for(const row of result.items){const found=choices.findIndex(item=>String(item.id)===String(row.id));if(found>=0)choices[found]=row;else choices.push(row)}
   this.setData({['legacyChoices.'+kind]:choices,['legacyChoiceIndex.'+kind]:Math.max(0,choices.findIndex(row=>String(row.id)===selected)),['legacyChoicePage.'+kind]:page,['legacyChoiceMore.'+kind]:(page+1)*50<Number(result.total)});
  }catch(error){if(version===this._legacyVersion)this.setData({legacyError:error.message})}
 },
 legacyChoice(event){if(!this.data.legacyWritable)return;const kind=event.currentTarget.dataset.kind,index=Number(event.detail.value),row=this.data.legacyChoices[kind]?.[index];if(row)this.setData({['legacyFields.'+choiceFields[kind]]:row.id,['legacyChoiceIndex.'+kind]:index})},
 async saveLegacy(){
  if(!this.data.legacyWritable||this.data.legacySaving||!this.data.legacyOrderId)return;
  const fields={};for(const key of ['MasterName','AnimalName','Age','DoctorName','SampleArea','CollectDt','AnimalTypeId','DictSexId','InfoSampleTypeId'])if(String(this.data.legacyFields[key]??'')!==String(this._legacyOriginal[key]??''))fields[key]=String(this.data.legacyFields[key]??'');
  if(!Object.keys(fields).length){wx.showToast({title:'资料未修改',icon:'none'});return}
  const id=this.data.legacyOrderId,body={hospitalId:getApp().getHospitalId(),fields,expectedToken:this._legacyToken},signature=JSON.stringify({id,...body});
  if(this._legacyPending?.signature!==signature)this._legacyPending={signature,key:'legacy_draft_'+Date.now()+'_'+Math.random().toString(36).slice(2,14)};
  this.setData({legacySaving:true,legacyError:''});try{await request.put(legacyBase+'/drafts/'+id,{...body,requestKey:this._legacyPending.key});if(this._legacyUnloaded)return;const reopened=await this.openLegacy({currentTarget:{dataset:{id}}});if(reopened)await this.loadLegacy();else this.setData({legacyError:'资料已保存，但重新读取失败，请重新打开草稿核对。'})}catch(error){if(!this._legacyUnloaded)this.setData({legacyError:error.message})}finally{if(!this._legacyUnloaded)this.setData({legacySaving:false})}
 }
})
