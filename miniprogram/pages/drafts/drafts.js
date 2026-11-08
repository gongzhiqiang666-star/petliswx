const request=require('../../utils/request')
const legacyBase='/orders/hospital-history'
const choiceFields={variety:'AnimalTypeId',sex:'DictSexId',sampleType:'InfoSampleTypeId'}
function noteText(html){return String(html||'').replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi,'').replace(/<img\b[^>]*>/gi,'[图片]').replace(/<\/?(?:p|div|br|li|tr)\b[^>]*>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&#39;/g,"'").replace(/&amp;/g,'&').trim()}
Page({
 data:{legacyCreating:false,legacyCreateEnabled:false,legacyPreview:null,legacyPreviewBusy:false,legacyPreviewError:'',legacyPreviewRemarkText:'',legacySubmitting:false,legacySubmitted:null,legacyPreviewFields:[{key:'MasterName',label:'畜主姓名'},{key:'AnimalName',label:'动物姓名'},{key:'Age',label:'动物年龄'},{key:'DoctorName',label:'送检医师'},{key:'CollectDt',label:'采样日期'},{key:'SampleArea',label:'采样部位'}],legacyItemsExpanded:false,legacySelectedItems:[],legacyItemCatalog:[],legacyItemWritable:false,legacyItemBusy:false,legacyItemLoading:false,legacyItemError:'',legacyItemQuery:'',legacyItemCategoryIndex:0,legacyItemCategories:[{label:'全部'},{label:'细胞学与组织病理'},{label:'过敏原'},{label:'微生物'},{label:'综合（其他项目）'}],legacyCatalogPage:0,legacyCatalogMore:false,items:[],busy:false,error:'',legacyTextFields:[{key:'MasterName',label:'畜主姓名',max:250},{key:'AnimalName',label:'动物姓名',max:250},{key:'Age',label:'动物年龄',max:50},{key:'DoctorName',label:'送检医师',max:100},{key:'SampleArea',label:'采样部位',max:50}],legacyChoiceKinds:[{kind:'variety',label:'动物品种'},{kind:'sex',label:'动物性别'},{kind:'sampleType',label:'采样类型'}],legacyEnabled:false,legacyItems:[],legacyPage:0,legacyMore:false,legacyBusy:false,legacyError:'',legacyOrderId:null,legacyFields:{},legacyWritable:false,legacyRemarkText:'',legacySaving:false,legacyChoices:{variety:[],sex:[],sampleType:[]},legacyChoiceIndex:{variety:0,sex:0,sampleType:0},legacyChoiceQuery:{variety:'',sex:'',sampleType:''},legacyChoicePage:{variety:0,sex:0,sampleType:0},legacyChoiceMore:{variety:false,sex:false,sampleType:false}},
 onShow(){this._legacyUnloaded=false;this.load();this.loadLegacy()},
 onUnload(){this._legacyUnloaded=true;this._legacyVersion=(this._legacyVersion||0)+1;this._legacyListVersion=(this._legacyListVersion||0)+1},
 async load(){this.setData({busy:true,error:''});try{this.setData({items:await request.get('/drafts')})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 async open(e){if(this.data.busy)return;this.setData({busy:true,error:''});try{const d=await request.get('/drafts/'+e.currentTarget.dataset.id);const payload=typeof d.payload==='string'?JSON.parse(d.payload):d.payload;const form={...payload,_draftId:d.id,_draftVersion:d.version};wx.setStorageSync('order_form',form);wx.navigateTo({url:'/pages/order-add-pet/order-add-pet?draft=1&petId='+form.petId})}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}},
 async remove(e){const c=await wx.showModal({title:'删除这份草稿？',content:'仅删除未提交资料，不影响送检订单。'});if(!c.confirm)return;this.setData({busy:true,error:''});try{await request.delete('/drafts/'+e.currentTarget.dataset.id)}catch(e){this.setData({error:e.message})}finally{this.setData({busy:false})}this.load()},
 async loadLegacy(append=false){
  if(typeof append!=='boolean')append=false;if(append&&(this.data.legacyBusy||!this.data.legacyMore))return;
  const version=(this._legacyListVersion||0)+1;this._legacyListVersion=version;this.setData({legacyBusy:true,legacyError:''});
  try{const caps=await request.get(legacyBase+'/capabilities');if(version!==this._legacyListVersion)return;
   this.setData({legacyCreateEnabled:caps.legacyHospitalDraftCreates===true,legacyEnabled:caps.legacyHospitalDrafts===true});if(caps.legacyHospitalDrafts!==true){this.setData({legacyItems:[],legacyMore:false});return}
   const hospitalId=getApp().getHospitalId();if(!hospitalId)throw Error('账号未关联医院');const page=append?this.data.legacyPage+1:0;
   const result=await request.get(legacyBase+'/drafts',{hospitalId,page,size:10});if(version!==this._legacyListVersion)return;
   const items=append?[...this.data.legacyItems,...result.items]:result.items;this.setData({legacyItems:items,legacyPage:page,legacyMore:items.length<Number(result.total)});
  }catch(error){if(version===this._legacyListVersion)this.setData({legacyError:error.message})}finally{if(version===this._legacyListVersion)this.setData({legacyBusy:false})}
 },
 async createLegacy(){
  if(!this.data.legacyCreateEnabled||this.data.legacySaving||this.data.legacyItemBusy||this.data.legacySubmitting)return;
  this._legacyVersion=(this._legacyVersion||0)+1;const fields=this._legacyCreatePending?{...this._legacyCreatePending.fields}:{MasterName:'',AnimalName:'',Age:' - 岁 - 月',DoctorName:'',AnimalTypeId:'0',DictSexId:'0',InfoSampleTypeId:'0',CollectDt:''};
  const choices={},indexes={};for(const[kind,field]of Object.entries(choiceFields)){choices[kind]=[{id:String(fields[field]||0),name:fields[field]&&fields[field]!=='0'?'待重新核对原选择':'未选择'}];indexes[kind]=0}
  this.setData({legacyCreating:true,legacyOrderId:'new',legacyFields:fields,legacyWritable:true,legacyError:'',legacyRemarkText:'',legacyChoices:choices,legacyChoiceIndex:indexes,legacyChoiceQuery:{variety:'',sex:'',sampleType:''},legacyPreview:null,legacySubmitted:null,legacyItemsExpanded:false});
  await Promise.all(Object.keys(choiceFields).map(kind=>this.loadLegacyChoices(kind)));
 },
 moreLegacy(){return this.loadLegacy(true)},
 async openLegacy(event){
  if(this.data.legacyItemBusy||this.data.legacySubmitting)return false;
  const id=Number(event.currentTarget.dataset.id),version=(this._legacyVersion||0)+1;this._legacyVersion=version;this.setData({legacyBusy:true,legacyError:'',legacyOrderId:null,legacyWritable:false,legacyPreview:null,legacyPreviewBusy:false,legacySubmitted:null});
  try{const detail=await request.get(legacyBase+'/drafts/'+id,{hospitalId:getApp().getHospitalId()});if(version!==this._legacyVersion)return;
   this._legacyOriginal={...detail.fields};this._legacyToken=detail.editToken;this._legacyPending=null;
   const choices={},indexes={};for(const[kind,field]of Object.entries(choiceFields)){choices[kind]=[{id:String(detail.fields[field]||0),name:detail.labels[field]||'原选择'}];indexes[kind]=0}
   this.setData({legacyItemsExpanded:false,legacySelectedItems:[],legacyItemWritable:false,legacyItemError:'',legacyCreating:false,legacyOrderId:id,legacyFields:{...detail.fields},legacyWritable:detail.writable===true,legacyRemarkText:noteText(detail.fields.Remark),legacyChoices:choices,legacyChoiceIndex:indexes,legacyChoiceQuery:{variety:'',sex:'',sampleType:''}});
   if(detail.writable)await Promise.all(Object.keys(choiceFields).map(kind=>this.loadLegacyChoices(kind)));return true;
  }catch(error){if(version===this._legacyVersion)this.setData({legacyError:error.message});return false}finally{if(version===this._legacyVersion)this.setData({legacyBusy:false})}
 },
 closeLegacy(){if(this.data.legacySaving||this.data.legacyItemBusy||this.data.legacySubmitting)return;this._legacyVersion=(this._legacyVersion||0)+1;this.setData({legacyCreating:false,legacyOrderId:null,legacyBusy:false,legacyError:'',legacyPreview:null,legacyPreviewBusy:false})},
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
  if(!this.data.legacyWritable||this.data.legacySaving||this.data.legacyItemBusy||!this.data.legacyOrderId)return;
  if(this.data.legacyCreating)return this.saveNewLegacy();
  const fields={};for(const key of ['MasterName','AnimalName','Age','DoctorName','SampleArea','CollectDt','AnimalTypeId','DictSexId','InfoSampleTypeId'])if(String(this.data.legacyFields[key]??'')!==String(this._legacyOriginal[key]??''))fields[key]=String(this.data.legacyFields[key]??'');
  if(!Object.keys(fields).length){wx.showToast({title:'资料未修改',icon:'none'});return}
  const id=this.data.legacyOrderId,body={hospitalId:getApp().getHospitalId(),fields,expectedToken:this._legacyToken},signature=JSON.stringify({id,...body});
  if(this._legacyPending?.signature!==signature)this._legacyPending={signature,key:'legacy_draft_'+Date.now()+'_'+Math.random().toString(36).slice(2,14)};
  this.setData({legacySaving:true,legacyError:''});try{await request.put(legacyBase+'/drafts/'+id,{...body,requestKey:this._legacyPending.key});if(this._legacyUnloaded)return;const reopened=await this.openLegacy({currentTarget:{dataset:{id}}});if(reopened)await this.loadLegacy();else this.setData({legacyError:'资料已保存，但重新读取失败，请重新打开草稿核对。'})}catch(error){if(!this._legacyUnloaded)this.setData({legacyError:error.message})}finally{if(!this._legacyUnloaded)this.setData({legacySaving:false})}
 },
 async saveNewLegacy(){
  if(!this.data.legacyCreateEnabled||!this.data.legacyCreating||this.data.legacySaving)return;
  const fields={};for(const key of ['MasterName','AnimalName','Age','DoctorName','CollectDt','AnimalTypeId','DictSexId','InfoSampleTypeId'])if(this.data.legacyFields[key]!==''&&this.data.legacyFields[key]!=null)fields[key]=String(this.data.legacyFields[key]);
  const body={hospitalId:getApp().getHospitalId(),fields},signature=JSON.stringify(body);
  if(this._legacyCreatePending&&this._legacyCreatePending.signature!==signature){this.setData({legacyError:'上次创建结果待核对，请恢复上次资料后重试，避免生成重复草稿。'});return}
  if(!this._legacyCreatePending)this._legacyCreatePending={signature,fields:{...fields},key:'legacy_create_'+Date.now()+'_'+Math.random().toString(36).slice(2,14)};
  this.setData({legacySaving:true,legacyError:''});
  try{const id=await request.post(legacyBase+'/drafts',{...body,requestKey:this._legacyCreatePending.key});if(this._legacyUnloaded)return;if(!Number.isSafeInteger(Number(id))||Number(id)<1)throw Error('创建结果待核对，请重试。');this._legacyCreatePending=null;
   const reopened=await this.openLegacy({currentTarget:{dataset:{id:Number(id)}}});if(reopened)await this.loadLegacy();else this.setData({legacyCreating:false,legacyError:'草稿已创建，编号'+id+'，请刷新列表重新打开核对。'});
  }catch(error){if(!this._legacyUnloaded){if([400,401,403,404,409,422].includes(Number(error.code)))this._legacyCreatePending=null;this.setData({legacyError:error.message})}}
  finally{if(!this._legacyUnloaded)this.setData({legacySaving:false})}
 },
 async openLegacyItems(){if(this.data.legacyCreating||!this.data.legacyOrderId||this.data.legacySaving)return;this.setData({legacyItemsExpanded:true,legacyItemError:''});await this.reloadLegacyItems();await this.loadLegacyCatalog()},
 async reloadLegacyItems(){
  const id=this.data.legacyOrderId,version=this._legacyVersion;if(!id)return false;this._selectedSequence=(this._selectedSequence||0)+1;const sequence=this._selectedSequence;
  try{const result=await request.get(legacyBase+'/drafts/'+id+'/items',{hospitalId:getApp().getHospitalId()});if(version!==this._legacyVersion||id!==this.data.legacyOrderId||sequence!==this._selectedSequence)return false;if(!Array.isArray(result.items))throw Error('已选项目读取失败');this._legacyItemsToken=result.editToken;this.setData({legacySelectedItems:result.items,legacyItemWritable:result.writable===true});return true}
  catch(error){if(version===this._legacyVersion&&sequence===this._selectedSequence)this.setData({legacyItemError:error.message,legacyItemWritable:false});return false}
 },
 legacyItemInput(event){this.setData({legacyItemQuery:event.detail.value})},
 legacyItemCategory(event){this.setData({legacyItemCategoryIndex:Number(event.detail.value)});return this.searchLegacyCatalog()},
 searchLegacyCatalog(){return this.loadLegacyCatalog()},
 moreLegacyCatalog(){if(this.data.legacyCatalogMore&&!this.data.legacyItemLoading)return this.loadLegacyCatalog(true)},
 async loadLegacyCatalog(append=false,keepApplied=false){
  const id=this.data.legacyOrderId,version=this._legacyVersion;if(!id)return;
  this._catalogSequence=(this._catalogSequence||0)+1;const sequence=this._catalogSequence;
  const query=(append||keepApplied)&&this._legacyCatalogQuery?{...this._legacyCatalogQuery}:{hospitalId:getApp().getHospitalId(),q:this.data.legacyItemQuery.trim(),...(this.data.legacyItemCategoryIndex?{category:this.data.legacyItemCategoryIndex}:{}),size:20};query.page=append?this.data.legacyCatalogPage+1:0;
  this.setData({legacyItemLoading:true});
  try{const result=await request.get(legacyBase+'/drafts/'+id+'/test-catalog',query);if(version!==this._legacyVersion||sequence!==this._catalogSequence)return;if(!Array.isArray(result.items))throw Error('检测项目目录读取失败');this._legacyCatalogQuery={...query};this.setData({legacyItemCatalog:append?[...this.data.legacyItemCatalog,...result.items]:result.items,legacyCatalogPage:query.page,legacyCatalogMore:(query.page+1)*20<Number(result.total)})}
  catch(error){if(version===this._legacyVersion&&sequence===this._catalogSequence)this.setData({legacyItemError:error.message})}
  finally{if(version===this._legacyVersion&&sequence===this._catalogSequence)this.setData({legacyItemLoading:false})}
 },
 addLegacyItem(event){return this.mutateLegacyItem(Number(event.currentTarget.dataset.id),'add')},
 removeLegacyItem(event){return this.mutateLegacyItem(Number(event.currentTarget.dataset.id),'remove')},
 async mutateLegacyItem(testId,operation){
  if(!this.data.legacyItemWritable||this.data.legacyItemBusy||this.data.legacySaving||!this._legacyItemsToken||!this.data.legacyOrderId)return;
  if(operation==='add'&&this.data.legacySelectedItems.some(row=>Number(row.testId)===testId))return;
  const id=this.data.legacyOrderId,version=this._legacyVersion;this.setData({legacyItemBusy:true,legacyItemError:''});
  try{
   if(operation==='remove'){const answer=await wx.showModal({title:'移除这个检测项目？',content:'仅从当前草稿移除该项目，其他项目和资金记录保持不变。'});if(!answer.confirm)return}
   const body={hospitalId:getApp().getHospitalId(),testId,expectedToken:this._legacyItemsToken},signature=JSON.stringify({id,operation,...body});this._legacyItemRequests=this._legacyItemRequests||{};this._legacyItemRequests[signature]=this._legacyItemRequests[signature]||'legacy_item_'+Date.now()+'_'+Math.random().toString(36).slice(2,14);
   await request.post(legacyBase+'/drafts/'+id+'/items/'+operation,{...body,requestKey:this._legacyItemRequests[signature]});if(version!==this._legacyVersion||this._legacyUnloaded)return;
   if(await this.reloadLegacyItems())await this.loadLegacyCatalog(false,true);else this.setData({legacyItemError:'项目已处理，请刷新已选项目核对结果。'});
  }catch(error){if(version===this._legacyVersion&&!this._legacyUnloaded)this.setData({legacyItemError:error.message})}
  finally{if(version===this._legacyVersion&&!this._legacyUnloaded)this.setData({legacyItemBusy:false})}
 }
,
 async previewLegacy(){
  if(this.data.legacyCreating||!this.data.legacyOrderId||this.data.legacySaving||this.data.legacyItemBusy||this.data.legacySubmitting)return;
  const changed=['MasterName','AnimalName','Age','DoctorName','SampleArea','CollectDt','AnimalTypeId','DictSexId','InfoSampleTypeId'].some(key=>String(this.data.legacyFields[key]??'')!==String(this._legacyOriginal?.[key]??''));
  if(changed){this.setData({legacyPreviewError:'请先保存资料，再预览提交。'});return}
  const id=this.data.legacyOrderId,version=this._legacyVersion;this._previewSequence=(this._previewSequence||0)+1;const sequence=this._previewSequence;
  this.setData({legacyPreviewBusy:true,legacyPreviewError:''});
  try{const result=await request.get(legacyBase+'/drafts/'+id+'/preview',{hospitalId:getApp().getHospitalId()});if(version!==this._legacyVersion||sequence!==this._previewSequence)return;this.setData({legacyPreview:result,legacyPreviewRemarkText:noteText(result.fields.Remark)})}
  catch(error){if(version===this._legacyVersion&&sequence===this._previewSequence)this.setData({legacyPreviewError:error.message})}
  finally{if(version===this._legacyVersion&&sequence===this._previewSequence)this.setData({legacyPreviewBusy:false})}
 },
 backLegacyPreview(){if(!this.data.legacySubmitting)this.setData({legacyPreview:null,legacyPreviewError:''})},
 async submitLegacy(){
  const preview=this.data.legacyPreview;if(!preview||preview.writable!==true||this.data.legacySubmitting||this.data.legacySaving||this.data.legacyItemBusy)return;
  const version=this._legacyVersion;this.setData({legacySubmitting:true,legacyPreviewError:''});
  try{
   const choice=await wx.showModal({title:'确认提交送检？',content:'将按本次预览提交审核。提交后可在医院送检历史中查看状态。',confirmText:'确认提交'});if(!choice.confirm||version!==this._legacyVersion||this._legacyUnloaded)return;
   const body={hospitalId:preview.hospitalId,expectedToken:preview.editToken},signature=JSON.stringify({id:preview.orderId,...body});this._submitRequests=this._submitRequests||{};this._submitRequests[signature]=this._submitRequests[signature]||'legacy_submit_'+Date.now()+'_'+Math.random().toString(36).slice(2,14);
   const result=await request.post(legacyBase+'/drafts/'+preview.orderId+'/submit',{...body,requestKey:this._submitRequests[signature]});if(version!==this._legacyVersion||this._legacyUnloaded)return;
   if(Number(result?.orderId)!==Number(preview.orderId)||Number(result?.sampleStatus)!==0)throw Error('提交结果待核对，请刷新记录或重试。');
   this._legacyVersion=(this._legacyVersion||0)+1;this._legacyOriginal=null;this._legacyToken=null;this._legacyItemsToken=null;
   this.setData({legacySubmitted:result,legacyOrderId:null,legacyPreview:null,legacyItemsExpanded:false,legacyFields:{},legacySubmitting:false});await this.loadLegacy();
  }catch(error){if(version===this._legacyVersion&&!this._legacyUnloaded)this.setData({legacyPreviewError:error.message})}
  finally{if(!this._legacyUnloaded)this.setData({legacySubmitting:false})}
 },
 viewSubmittedLegacy(){wx.navigateTo({url:'/pages/hospital-history/hospital-history'})}

})
