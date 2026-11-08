import {build} from 'esbuild';
import {fileURLToPath} from 'node:url';
await build({entryPoints:[fileURLToPath(new URL('./admin-editor-entry.js',import.meta.url))],outfile:fileURLToPath(new URL('../../admin/assets/legacy-editor.js',import.meta.url)),bundle:true,format:'esm',platform:'browser',target:'es2022',minify:true});
console.log('Petlis 本地编辑器打包完成');
