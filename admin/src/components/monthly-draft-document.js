/** Keep the original HTML unless the user actually edits the document. */
export function monthlyDraftDocument(data,editors){
 const result={title:String(data.title??''),pdfTitle:String(data.pdfTitle??''),reportHtml:String(data.reportHtml??''),mailHtml:String(data.mailHtml??'')};
 for(const key of ['reportHtml','mailHtml'])if(editors[key]?.isDirty())result[key]=editors[key].getHTML();
 return result;
}
export function monthlyPdfFilename(title){
 if(typeof title!=='string'||!title.trim()||/[\\/\x00-\x1f\x7f]/.test(title)||title==='.'||title==='..')throw Error('请填写不含路径或控制字符的PDF文件名');
 if(new TextEncoder().encode(title+'.pdf').length>255)throw Error('PDF文件名过长');
 return title+'.pdf';
}
export async function downloadMonthlyDraftPdf(api,document){
 const filename=monthlyPdfFilename(document.pdfTitle);
 const file=await api.download('/admin/legacy/html-pdf/preview',{method:'POST',data:{html:document.reportHtml}});
 if(!file?.blob||file.blob.type!=='application/pdf'||await file.blob.slice(0,5).text()!=='%PDF-')throw Error('未取得有效PDF，请重试');
 return {...file,filename};
}
