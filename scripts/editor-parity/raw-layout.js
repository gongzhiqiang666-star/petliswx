import Paragraph from '@tiptap/extension-paragraph';
import HardBreak from '@tiptap/extension-hard-break';
import Image from '@tiptap/extension-image';
import {TableCell} from '@tiptap/extension-table';
import {preserveRawRender} from './raw-dom-spec.js';
export const RawParagraph=preserveRawRender(Paragraph);
export const RawHardBreak=preserveRawRender(HardBreak);
export const RawImage=preserveRawRender(Image);
export const RawTableCell=preserveRawRender(TableCell);
