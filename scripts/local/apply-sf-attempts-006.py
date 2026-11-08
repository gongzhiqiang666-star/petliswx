"""Apply only user-approved 006 to the isolated Petlis copy, preserving snapshots and full backup.
Run with --execute-approved-local-006 only after specific user approval; never auto-run at startup.
"""
import argparse,copy,datetime,hashlib,importlib.util,json,os,re,subprocess
from pathlib import Path
from types import SimpleNamespace
REPO=Path(__file__).resolve().parents[2];PRIVATE=Path.home()/'.local/share/petlis'
spec=importlib.util.spec_from_file_location('config',REPO/'scripts/local/configure-php-lab.py');config=importlib.util.module_from_spec(spec);spec.loader.exec_module(config)
spec=importlib.util.spec_from_file_location('guard',REPO/'integration/schema_guard.py');guard=importlib.util.module_from_spec(spec);spec.loader.exec_module(guard)
TARGETS={'lin_shipment','lin_sf_request'}
APPROVED=["""ALTER TABLE lin_shipment ADD COLUMN attempt_no INT UNSIGNED NOT NULL DEFAULT 1, ADD COLUMN is_current TINYINT NULL DEFAULT 1, DROP INDEX uk_shipment_order, ADD UNIQUE KEY uk_shipment_order_attempt (order_id,attempt_no), ADD UNIQUE KEY uk_shipment_order_current (order_id,is_current), ADD CONSTRAINT chk_shipment_current_flag CHECK (is_current IS NULL OR is_current=1)""","""ALTER TABLE lin_sf_request ADD COLUMN attempt_no INT UNSIGNED NOT NULL DEFAULT 1, DROP INDEX uk_sf_order_operation, ADD UNIQUE KEY uk_sf_order_attempt_operation (order_id,attempt_no,operation)"""]
def normalized(s):return re.sub(r'\s+','',s).lower()
def save(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n');path.chmod(0o600)
def snapshot():
 guard.mysql_rows=lambda args,query:[json.loads(line) for line in config.sql(query).splitlines() if line.strip()]
 result=guard.snapshot(SimpleNamespace(host='127.0.0.1',port=13306,isolated_copy=True,database='laboklin'),allow_platform=True)
 result['views']=[json.loads(line) for line in config.sql("SELECT JSON_OBJECT('table',TABLE_NAME,'sql',VIEW_DEFINITION,'check',CHECK_OPTION,'updatable',IS_UPDATABLE,'definer',DEFINER,'security',SECURITY_TYPE,'charset',CHARACTER_SET_CLIENT,'collation',COLLATION_CONNECTION) FROM information_schema.VIEWS WHERE TABLE_SCHEMA='laboklin' ORDER BY TABLE_NAME").splitlines()]
 result['routines']=[json.loads(line) for line in config.sql("SELECT JSON_OBJECT('name',ROUTINE_NAME,'type',ROUTINE_TYPE,'sql',ROUTINE_DEFINITION,'deterministic',IS_DETERMINISTIC,'security',SECURITY_TYPE,'definer',DEFINER,'mode',SQL_MODE) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='laboklin' ORDER BY ROUTINE_NAME").splitlines()]
 return result

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--execute-approved-local-006',action='store_true');args=parser.parse_args();os.umask(0o077)
 source=REPO/'docs/database/pending/006-sf-shipment-attempts.sql';content=source.read_text();statements=[s.strip() for s in re.sub(r'^\s*--.*$','',content,flags=re.M).split(';') if s.strip()]
 assert len(statements)==2 and list(map(normalized,statements))==list(map(normalized,APPROVED)),'SQL differs from the approved two statements'
 assert config.sql("SELECT @@port=13306 AND DATABASE()='laboklin'")=='1'
 existing=int(config.sql("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='laboklin' AND TABLE_NAME IN ('lin_shipment','lin_sf_request') AND COLUMN_NAME IN ('attempt_no','is_current')"));assert existing==0,'006 already or partly applied; refuse replay'
 for table,index in [('lin_shipment','uk_shipment_order'),('lin_sf_request','uk_sf_order_operation')]:
  assert int(config.sql(f"SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='laboklin' AND TABLE_NAME='{table}' AND INDEX_NAME='{index}' AND NON_UNIQUE=0"))>0,'Original unique index missing'
 assert config.sql('SELECT @@event_scheduler')=='OFF','MySQL events must remain disabled'
 if not args.execute_approved_local_006:print(json.dumps({'target':'127.0.0.1:13306/laboklin','preflightPassed':True,'ddlExecuted':False}));return
 listening=subprocess.run(['lsof','-t','-iTCP:8082','-sTCP:LISTEN'],capture_output=True,text=True);assert not listening.stdout.strip(),'Stop verified Petlis backend before executing DDL'
 assert config.sql("SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB='laboklin' AND COMMAND<>'Sleep' AND ID<>CONNECTION_ID()")=='0','Another Petlis DB operation is active'
 folder=PRIVATE/'backups'/('sf-attempts-006-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'));folder.mkdir(parents=True,exist_ok=False);folder.chmod(0o700)
 before=snapshot();save(folder/'schema-before.json',before)
 columns={t:[r['name'] for r in before['columns'] if r['table']==t] for t in TARGETS}
 def data_digest(table):
  names=','.join('`'+name+'`' for name in columns[table]);data=config.sql(f'SELECT {names} FROM `{table}` ORDER BY id');return {'count':int(config.sql(f'SELECT COUNT(*) FROM `{table}`')),'sha256':hashlib.sha256(data.encode()).hexdigest()}
 data_before={t:data_digest(t) for t in sorted(TARGETS)};save(folder/'original-rows-before.json',data_before)
 manifest={'target':'127.0.0.1:13306/laboklin','sqlSha256':hashlib.sha256(content.encode()).hexdigest(),'approvedTables':sorted(TARGETS),'completedStatements':0,'status':'BACKING_UP'};save(folder/'execution.json',manifest)
 binary=PRIVATE/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysqldump';dump=folder/'laboklin-full.sql'
 with dump.open('wb') as output:
  result=subprocess.run([str(binary),f'--defaults-extra-file={PRIVATE}/mysql/root.cnf','--single-transaction','--routines','--triggers','--events','--hex-blob','--no-tablespaces','--set-gtid-purged=OFF','laboklin'],stdout=output,stderr=subprocess.PIPE)
 assert result.returncode==0 and dump.stat().st_size>1000,'Full backup failed; no DDL executed';dump.chmod(0o600)
 digest=hashlib.sha256()
 with dump.open('rb') as stream:
  for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
 manifest.update({'backupBytes':dump.stat().st_size,'backupSha256':digest.hexdigest(),'status':'APPLYING'});save(folder/'execution.json',manifest)
 try:
  for index,statement in enumerate(statements):
   config.sql(statement);manifest['completedStatements']=index+1;save(folder/'execution.json',manifest)
  after=snapshot();save(folder/'schema-after.json',after)
  baseline=copy.deepcopy(before);baseline['tables']=[t for t in before['tables'] if t not in guard.PLATFORM_TABLES];baseline['legacy_tables']=[t for t in before['legacy_tables'] if t not in TARGETS]
  differences=guard.compare_snapshots(baseline,after)
  for section in ['table_options','columns','indexes','foreign_keys','check_constraints','triggers']:
   assert [r for r in before[section] if r['table'] not in TARGETS]==[r for r in after[section] if r['table'] not in TARGETS],section+' changed outside approval'
  assert not differences,differences
  assert before['tables']==after['tables'] and before['views']==after['views'] and before['routines']==after['routines'],'Objects changed outside approval'
  data_after={t:data_digest(t) for t in sorted(TARGETS)};save(folder/'original-rows-after.json',data_after);assert data_before==data_after,'Original rows changed'
  assert config.sql('SELECT COUNT(*) FROM lin_shipment WHERE attempt_no<>1 OR is_current<>1 OR is_current IS NULL')=='0'
  assert config.sql('SELECT COUNT(*) FROM lin_sf_request WHERE attempt_no<>1')=='0'
  assert int(config.sql("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='laboklin' AND TABLE_NAME IN ('lin_shipment','lin_sf_request') AND COLUMN_NAME IN ('attempt_no','is_current')"))==3
  manifest.update({'status':'VERIFIED','schemaGuardDifferences':differences,'otherTablesUnchanged':True,'viewsUnchanged':True,'routinesUnchanged':True,'originalRowsUnchanged':True,'tableCount':len(after['tables']),'viewCount':len(after['views']),'triggerCount':len(after['triggers']),'mysqlEvents':'OFF','reshipEnabled':False});save(folder/'execution.json',manifest)
  print(json.dumps({'status':'VERIFIED','backupDirectory':str(folder),'completedStatements':2,'originalRows':data_after,'otherTablesViewsTriggersUnchanged':True,'reshipEnabled':False}))
 except Exception:
  manifest['status']='STOPPED_REQUIRES_INSPECTION';save(folder/'execution.json',manifest);raise
if __name__=='__main__':main()
