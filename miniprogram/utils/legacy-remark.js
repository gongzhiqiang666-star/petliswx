// Deliberately explicit native subset. Never strip unsupported legacy HTML.
const fail=()=>{throw Error('这份备注含当前未支持的格式，原文已保留，请勿覆盖。')}
const marks={b:'bold',strong:'bold',i:'italic',em:'italic',u:'underline'}
function entities(text){
 if(/&[A-Za-z#]/.test(text.replace(/&(?:amp|lt|gt|quot|apos|nbsp|#\d+|#x[\da-f]+);/gi,'')))fail()
 return text.replace(/&([^;\s]+);/g,(_,key)=>{
  const names={amp:'&',lt:'<',gt:'>',quot:'"',apos:"'",nbsp:'\u00a0'};if(Object.prototype.hasOwnProperty.call(names,key))return names[key]
  if(!/^#(?:\d+|x[\da-f]+)$/i.test(key))fail();const n=key[1].toLowerCase()==='x'?parseInt(key.slice(2),16):Number(key.slice(1));if(!Number.isInteger(n)||n<=0||n>0x10ffff||n>=0xd800&&n<=0xdfff)fail();return String.fromCodePoint(n)
 })
}
function fromHtml(html){
 if(typeof html!=='string')fail();if(!html)return {ops:[{insert:'\n'}]}
 const paragraphs=[]
 if(!html.includes('<'))paragraphs.push(html)
 else{let end=0;for(const m of html.matchAll(/<p>([\s\S]*?)<\/p>/gi)){if(html.slice(end,m.index).trim())fail();paragraphs.push(m[1]);end=m.index+m[0].length}if(!paragraphs.length||html.slice(end).trim())fail()}
 const ops=[]
 for(const paragraph of paragraphs){
  if(/^<br\s*\/?>$/i.test(paragraph)){ops.push({insert:'\n'});continue}
  const stack=[];for(const part of paragraph.split(/(<[^>]*>)/g)){
   if(!part)continue
   if(part.startsWith('<')){const m=/^<(\/?)\s*(b|strong|i|em|u)\s*>$/i.exec(part);if(!m)fail();const name=m[2].toLowerCase();if(m[1]){if(stack.pop()!==name)fail()}else stack.push(name)}
   else{if(part.includes('<'))fail();const insert=entities(part.replace(/[\t\r\n\f ]+/g,' '));if(!insert)continue;const attributes=Object.fromEntries(stack.map(name=>[marks[name],true]));ops.push(Object.keys(attributes).length?{insert,attributes}:{insert})}
  }
  if(stack.length)fail();ops.push({insert:'\n'})
 }
 return {ops}
}
function toHtml(delta){
 if(!delta||!Array.isArray(delta.ops)||!delta.ops.length)fail()
 const escape=value=>value.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
 const lines=[];let line='',ended=false
 const merged=[]
 for(const op of delta.ops){
  if(!op||typeof op.insert!=='string'||op.delete!==undefined||op.retain!==undefined)fail()
  const attrs=op.attributes||{};if(typeof attrs!=='object'||Object.entries(attrs).some(([key,value])=>!['bold','italic','underline'].includes(key)||typeof value!=='boolean'))fail()
  const cleaned=Object.fromEntries(['bold','italic','underline'].filter(k=>attrs[k]).map(k=>[k,true])),key=JSON.stringify(cleaned);const last=merged[merged.length-1];if(last&&last.key===key)last.insert+=op.insert;else merged.push({insert:op.insert,attrs:cleaned,key})
 }
 for(const op of merged){
  const attrs=op.attrs
  const parts=op.insert.split('\n');for(let i=0;i<parts.length;i++){
   if(i){lines.push('<p>'+(line||'<br>')+'</p>');line='';ended=true}
   if(parts[i]){let text=escape(parts[i]);for(const [key,tag]of [['underline','u'],['italic','em'],['bold','strong']])if(attrs[key])text='<'+tag+'>'+text+'</'+tag+'>';line+=text;ended=false}
  }
 }
 if(!ended||line)fail();return lines.length===1&&lines[0]==='<p><br></p>'?'':lines.join('')
}
module.exports={fromHtml,toHtml}
