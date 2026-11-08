import {getSchema} from '@tiptap/core';
import {DOMParser} from '@tiptap/pm/model';
import {prepareHierarchyContent} from './legacy-inline.js';
/** Parse the prepared native fragment once; avoid Tiptap string helper's whitespace deletion. */
export function hierarchyContent(document,html,configuration){
 const root=prepareHierarchyContent(document,html);
 return DOMParser.fromSchema(getSchema(configuration.extensions)).parse(root,configuration.parseOptions).toJSON();
}
