"""Build source-level migration work ledger; no PHP execution, database writes or completion claims."""
import csv,json,pathlib,hashlib
repo=pathlib.Path(__file__).resolve().parents[2];folder=repo/'docs/php-parity';manifest=json.loads((folder/'source-manifest.json').read_text());root=pathlib.Path(manifest['referenceRoot'])
for f in manifest['files']:
    if hashlib.sha256((root/f['path']).read_bytes()).hexdigest()!=f['sha256']:raise RuntimeError('Reference drift: '+f['path'])
def module(path):
    p=path.lower()
    if '/example/' in p or '/demo.php' in p or '/addon.php' in p or p.startswith('addons/'):return 'M32'
    if '/admin/controller/base.php' in p:return 'M31'
    rules=[('/auth/adminlog.php','M28'),('/auth/adm','M01'),('/auth/lbdictmailserver.php','M20'),('/auth/','M02'),('/user/','M03'),('/general/attachment.php','M27'),('/general/profile.php','M01'),('/general/config.php','M28'),('/finance/hospmonthpeport.php','M22'),('/finance/lbpaylog.php','M23'),('/finance/project.php','M21'),('/basedata/infohospitalmoney.php','M06'),('/basedata/lbinfohospitalcontact.php','M05'),('/basedata/lbinfohospital.php','M04'),('/project/lbfilediagnosisproject.php','M14'),('/project/lbdiagnosisdetail.php','M12'),('/project/lblistdiagnosisproject.php','M10'),('/tpl/lbinfotesttypeprice.php','M08'),('/tpl/lbinfotesttype.php','M08'),('/tpl/lbinfotesttypemodel.php','M15'),('/data/lbdictbindkey.php','M16'),('/data/','M07'),('/basedata/lbinfocheckreceived.php','M05'),('/basedata/','M07'),('/index/controller/project/pay.php','M24'),('/index/controller/pay.php','M24'),('/index/controller/project/view.php','M25'),('/index/controller/cron.php','M30'),('/index/controller/user.php','M11'),('/api/controller/sms.php','M26'),('/api/controller/ems.php','M26'),('/api/controller/token.php','M03'),('/api/controller/validate.php','M03'),('/api/controller/user.php','M03'),('/tpl/','M19')]
    for suffix,value in rules:
        if suffix in p:return value
    if '/admin/controller/a.php' in p:return 'M32'
    if 'ajax.php' in p or 'common.php' in p:return 'M27'
    return 'M28'
existing_path=folder/'migration-ledger.csv'
existing={row['id']:row for row in csv.DictReader(existing_path.open())} if existing_path.exists() else {}
rows=[]
for source in csv.DictReader((folder/'controller-action-candidates.csv').open()):
    path=source['file'];s=source['symbol'];origin=source['origin'];text=(root/path).read_text(errors='replace')
    rows.append(dict(id=source['id'],module=module(path),source=path,line=source['line'],symbol=s,origin=origin,status='pending_source_contract',permission='pending',displayContract='pending',calculationAndSideEffects='pending',newImplementation='pending_parity_check',verification='not_run',note='Static ownership only; not endpoint reachability or completed migration'))
for row in rows:
    old=existing.get(row['id'])
    if old:
        if any(old.get(key)!=row[key] for key in ('source','line','symbol','origin')):
            raise RuntimeError('Candidate identity changed; preserve ledger and review source diff: '+row['id'])
        row.update({key:value for key,value in old.items() if key in row})
if set(existing)-{row['id'] for row in rows}:
    raise RuntimeError('Candidates would be lost; refusing to overwrite ledger')
with (folder/'migration-ledger.csv').open('w',newline='') as target:
    writer=csv.DictWriter(target,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
counts={}
for r in rows:counts[r['module']]=counts.get(r['module'],0)+1
(folder/'ledger-summary.json').write_text(json.dumps({'candidateRows':len(rows),'moduleCandidateCounts':counts,'status':'source_ownership_only_not_function_count_or_completion','unmodifiedReferenceFilesVerified':len(manifest['files'])},ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'candidateRows':len(rows),'referenceHashesVerified':len(manifest['files']),'completionClaimed':False}))
