import fs from 'node:fs';
import {roundTrip} from './baseline-probe.js';
import {legacyExtensions} from './legacy-extensions.js';
import {checkFidelity} from './fidelity-gate.js';
const compatibility=process.argv.includes('--compat');
const input=JSON.parse(fs.readFileSync(0,'utf8')),totals={documents:0,evaluated:0,passed:0,rejected:0,textChanged:0,errors:0},missing=new Map(),errors=[];
for(const html of input){
 if(process.argv.includes('--only-errors')&&![655,662].includes(totals.documents)){totals.documents++;continue;}
 totals.documents++;totals.evaluated++;let stage="roundtrip";
 try{const output=roundTrip(html,compatibility?legacyExtensions():[]);stage="gate";const check=checkFidelity(html,output);totals[check.passed?'passed':'rejected']++;if(check.textChanged)totals.textChanged++;
 for(const item of check.missing){const key=[item.kind,item.tag,item.name].join('.');missing.set(key,(missing.get(key)??0)+item.count);}
 }catch(error){totals.errors++;errors.push({documentIndex:totals.documents-1,stage,name:["RangeError","TypeError","Error"].includes(error.name)?error.name:"Other",dependencyFrames:String(error.stack).split("\n").slice(1,4).map(line=>line.replace(/.*node_modules\//,"node_modules/").trim()),category:String(error.message).includes("Invalid content")?"invalid-node-content":String(error.message).includes("Maximum call stack")?"nesting-limit":"unclassified"});}
}
console.log(JSON.stringify({scope:'local legacy templates; offline roundtrip; no content or attribute values emitted',editor:compatibility?'Tiptap 3.33.0 + experimental legacy attributes/font/div/colgroup/embedded images':'Tiptap 3.33.0 common OSS extensions; no custom compatibility extensions',adoptionApproved:false,...totals,errorSummary:errors,missing:Object.fromEntries([...missing].sort((a,b)=>b[1]-a[1]))},null,2));
