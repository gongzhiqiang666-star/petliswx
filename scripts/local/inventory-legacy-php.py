#!/usr/bin/env python3
"""Read-only PHP token inventory for scope review; never includes source or credentials in output."""
import csv, hashlib, json, pathlib, subprocess
ROOT=pathlib.Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn')
OUT=pathlib.Path(__file__).resolve().parents[2]/'docs/php-parity'
assert ROOT.is_dir()
files=[]
for base in ('application','public/assets/js/backend','addons'):
    folder=ROOT/base
    if not folder.exists(): continue
    for path in sorted(folder.rglob('*')):
        if not path.is_file() or path.suffix not in ('.php','.html','.js'): continue
        rel=path.relative_to(ROOT).as_posix()
        if path.is_symlink() or any(p in ('config','lang','runtime','vendor','node_modules') for p in path.relative_to(ROOT).parts):continue
        if path.name in ('database.php','config.php','extra.php'):continue
        raw=path.read_bytes()
        files.append({'path':rel,'sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw)})
php=[dict(path=f['path'],source=(ROOT/f['path']).read_text(errors='replace')) for f in files if f['path'].endswith('.php')]
parser=r'''<?php
$files=json_decode(stream_get_contents(STDIN),true);$out=[];
foreach($files as $file){$t=token_get_all($file['source']);$n=count($t);
for($i=0;$i<$n;$i++){
 if(!is_array($t[$i]))continue;
 if($t[$i][0]===T_FUNCTION){$j=$i+1;while($j<$n && (is_array($t[$j])&&in_array($t[$j][0],[T_WHITESPACE,T_COMMENT,T_DOC_COMMENT])||$t[$j]==='&'))$j++;
 if($j<$n && is_array($t[$j])&&$t[$j][0]===T_STRING){$visibility='default';for($k=$i-1;$k>=0;$k--){if(!is_array($t[$k])&&in_array($t[$k],[';','{','}']))break;if(is_array($t[$k])&&in_array($t[$k][0],[T_PUBLIC,T_PROTECTED,T_PRIVATE])){$visibility=strtolower(trim($t[$k][1]));break;}}
 $out[]=['file'=>$file['path'],'line'=>$t[$i][2],'kind'=>'named_function','name'=>$t[$j][1],'visibility'=>$visibility];}}
 if($t[$i][0]===T_DOUBLE_COLON){$j=$i+1;while($j<$n&&is_array($t[$j])&&$t[$j][0]===T_WHITESPACE)$j++;
 if($j<$n&&is_array($t[$j])&&in_array($t[$j][1],['beforeInsert','beforeUpdate','afterWrite','afterInsert','afterUpdate','beforeDelete','afterDelete']))$out[]=['file'=>$file['path'],'line'=>$t[$i][2],'kind'=>'model_hook','name'=>$t[$j][1],'visibility'=>'callback'];}
}}
echo json_encode($out,JSON_UNESCAPED_SLASHES|JSON_UNESCAPED_UNICODE);
'''
# PHP parses the original reference bytes supplied on stdin; no application bootstrap or DB calls.
# Parser code uses -r while data stays stdin, never shell-interpolated.
result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','-r',parser.removeprefix('<?php\n')],input=json.dumps(php),text=True,capture_output=True,check=True)
symbols=json.loads(result.stdout)
OUT.mkdir(exist_ok=True)
with (OUT/'source-symbols.csv').open('w',newline='') as target:
    writer=csv.DictWriter(target,fieldnames=['file','line','kind','name','visibility']);writer.writeheader();writer.writerows(symbols)
controllers=[f for f in files if '/controller/' in f['path'] and f['path'].endswith('.php')]
manifest={'referenceRoot':str(ROOT),'method':'PHP 7.4 token_get_all, no execution/bootstrap; static inventory, not route reachability proof','limitations':['Inherited actions and dynamically registered routes require separate review','Named functions and model hook registration sites only; anonymous algorithm bodies require manual review','Configuration/language/vendor/runtime files excluded; missing addons are not inferred as implemented'],'files':files,'counts':{'files':len(files),'controllerFiles':len(controllers),'namedFunctions':sum(x['kind']=='named_function' for x in symbols),'modelHookSites':sum(x['kind']=='model_hook' for x in symbols),'controllerNamedFunctions':sum('/controller/' in x['file'] and x['kind']=='named_function' for x in symbols)}}
(OUT/'source-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(manifest['counts'],ensure_ascii=False))
