"""Verify Java against private original-PHP results without logging or retaining report bodies."""
import json,os,secrets,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def verify_java_display(raw,expected,source_hash):
 if not raw or len(raw)!=len(expected):raise ValueError('Display corpus size mismatch')
 private=Path.home()/'.local/share/petlis/qa';private.mkdir(parents=True,exist_ok=True)
 nonce=secrets.token_hex(16)
 with tempfile.NamedTemporaryFile(mode='w',dir=private,prefix='display-corpus-',suffix='.json',delete=False) as handle:
  fixture=Path(handle.name);json.dump({'nonce':nonce,'count':len(raw),'sourceHash':source_hash,'rows':[{'raw':a,'expected':b} for a,b in zip(raw,expected)]},handle)
 result=Path(str(fixture)+'.proof.json')
 try:
  env=dict(os.environ);env.update({'JAVA_HOME':str(Path.home()/'.local/share/petlis/tools/Home'),'PETLIS_DISPLAY_CORPUS':str(fixture),'APP_SCHEDULING_ENABLED':'false','DB_MIGRATIONS_ENABLED':'false'})
  env['PATH']=env['JAVA_HOME']+'/bin:'+str(Path.home()/'.local/share/petlis/tools/libexec/bin')+':'+env['PATH']
  run=subprocess.run(['mvn','-o','-f','petlis-backend/pom.xml','-Dtest=LegacyDisplayCorpusTest','-DskipTests=false','-Dmaven.test.skip=false','test'],cwd=ROOT,env=env,capture_output=True,text=True,timeout=180)
  with tempfile.NamedTemporaryFile(mode='w',dir='/tmp',prefix='petlis-java-display-',suffix='.log',delete=False) as log:log.write(run.stdout+run.stderr);log_path=log.name
  if run.returncode or not result.exists():raise RuntimeError('Java display verification failed; body withheld; log '+log_path)
  proof=json.loads(result.read_text())
  if proof!={'nonce':nonce,'count':len(raw),'matched':True,'sourceHash':source_hash}:raise RuntimeError('Java display proof does not match this corpus')
  return {'cases':len(raw),'matched':True,'sourceHash':source_hash,'log':log_path,'privateCorpusRetained':False}
 finally:
  fixture.unlink(missing_ok=True);result.unlink(missing_ok=True)
