/** CSS table indentation is not a report cell. Preserve cell/pre text and non-ASCII spacing. */
const displays={TABLE:['table','inline-table'],THEAD:['table-header-group'],TBODY:['table-row-group'],TFOOT:['table-footer-group'],TR:['table-row'],COLGROUP:['table-column-group'],COL:['table-column'],TD:['table-cell'],TH:['table-cell'],CAPTION:['table-caption']};
const children={TABLE:new Set(['CAPTION','COL','COLGROUP','THEAD','TBODY','TFOOT','TR']),THEAD:new Set(['TR']),TBODY:new Set(['TR']),TFOOT:new Set(['TR']),TR:new Set(['TD','TH']),COLGROUP:new Set(['COL'])};
const asciiWhitespace=node=>node?.nodeType===3&&/^[\t\n\f\r ]+$/.test(node.data);
function nativeDisplay(element){const value=element.style?.display;return !value||displays[element.tagName]?.includes(value);}
export function isIgnoredTableWhitespace(node){
 if(!asciiWhitespace(node))return false;const parent=node.parentElement,allowed=children[parent?.tagName];if(!allowed||!nativeDisplay(parent))return false;
 for(const direction of ['previousSibling','nextSibling']){let sibling=node[direction];while(sibling&&(sibling.nodeType===8||asciiWhitespace(sibling)))sibling=sibling[direction];if(sibling&&(sibling.nodeType!==1||!allowed.has(sibling.tagName)||!nativeDisplay(sibling)))return false;}
 return true;
}
export function removeIgnoredTableWhitespace(root){for(const parent of root.querySelectorAll('table,thead,tbody,tfoot,tr,colgroup'))for(const node of [...parent.childNodes])if(isIgnoredTableWhitespace(node))node.remove();return root;}
export function tableContentText(document,html){const root=document.createElement('div');root.innerHTML=html;return removeIgnoredTableWhitespace(root).textContent;}
