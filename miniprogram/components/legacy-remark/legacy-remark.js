const {fromHtml,toHtml}=require('../../utils/legacy-remark')
Component({
 properties:{html:{type:String,value:''},disabled:{type:Boolean,value:false}},
 data:{ready:false,error:''},
 observers:{html(){if(this._context)this.load()}},
 lifetimes:{attached(){this._detached=false},detached(){this._detached=true;this._version=(this._version||0)+1;this._context=null}},
 methods:{
  ready(){this.createSelectorQuery().select('#remark-editor').context(result=>{if(this._detached)return;if(result?.context){this._context=result.context;this.load()}else this.setData({error:'备注编辑器未就绪，原文已保留。'})}).exec()},
  load(){
   const version=(this._version||0)+1;this._version=version;this._loading=true;this.setData({ready:false,error:''});let delta
   try{delta=fromHtml(this.data.html);this._baseline=toHtml(delta)}catch(error){this._loading=false;this.setData({error:error.message});return}
   const fail=()=>{if(version!==this._version)return;this._loading=false;this.setData({ready:false,error:'备注格式未能完整载入，原文已保留。'})}
   this._context.setContents({delta,success:()=>{if(version!==this._version)return;this._context.getContents({success:result=>{if(version!==this._version)return;try{if(toHtml(result.delta)!==this._baseline)return fail();this._loading=false;this.setData({ready:true,error:''})}catch(_){fail()}},fail})},fail})
  },
  input(event){
   if(this._loading||!this.data.ready||this.data.disabled)return
   try{const html=toHtml(event.detail.delta),dirty=html!==this._baseline;this.setData({error:''});this.triggerEvent('change',{valid:true,dirty,html:dirty?html:this.data.html})}
   catch(error){this.setData({error:error.message});this.triggerEvent('change',{valid:false})}
  },
  format(event){if(!this.data.ready||this.data.disabled)return;const name=event.currentTarget.dataset.format;if(['bold','italic','underline'].includes(name))this._context.format(name)},
  undo(){if(this.data.ready&&!this.data.disabled)this._context.undo()},
  redo(){if(this.data.ready&&!this.data.disabled)this._context.redo()}
 }
})
