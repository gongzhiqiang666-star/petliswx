/** Source finance HTML embeds a stylesheet. Keep it out of the app DOM, but
 * retain it in the resulting document so edited PDFs keep the source CSS. */
export function mountMonthlyEditor(target,original,mountEditor){
 const template=target.ownerDocument.createElement('template');template.innerHTML=original;
 const styles=[...template.content.querySelectorAll('style')];
 const stylesheet=styles.map(node=>node.outerHTML).join('');
 for(const node of styles)node.remove();
 const editor=mountEditor(target,template.innerHTML);
 return {isDirty:()=>editor.isDirty(),getHTML:()=>editor.isDirty()?stylesheet+editor.getHTML():original,destroy:()=>editor.destroy()};
}
