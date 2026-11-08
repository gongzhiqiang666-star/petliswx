/** Counts only; original text and actual changed characters never leave this helper. */
export function textDiagnostic(source,output){
 const a=Array.from(source),b=Array.from(output);let start=0,ae=a.length,be=b.length;
 while(start<ae&&start<be&&a[start]===b[start])start++;
 while(ae>start&&be>start&&a[ae-1]===b[be-1]){ae--;be--;}
 function kinds(chars){const result={};for(const char of chars){const kind=char==='\n'?'newline':char==='\r'?'carriage-return':char==='\t'?'tab':char===' '?'space':char==='\u00a0'?'non-breaking-space':/\p{L}/u.test(char)?'letter':/\p{N}/u.test(char)?'number':/\p{P}/u.test(char)?'punctuation':'other';result[kind]=(result[kind]??0)+1;}return result;}
 return {sourceLength:a.length,outputLength:b.length,commonPrefixLength:start,commonSuffixLength:a.length-ae,sourceChangeKinds:kinds(a.slice(start,ae)),outputChangeKinds:kinds(b.slice(start,be))};
}
/** BR-aware text comparison is diagnostic only; it does not prove line height/layout equivalence. */
export function compareBreakAwareText(document,before,after){
 function text(html){const root=document.createElement('div');root.innerHTML=html;for(const br of root.querySelectorAll('br'))br.replaceWith(document.createTextNode('\n'));return root.textContent;}
 return text(before)===text(after);
}
