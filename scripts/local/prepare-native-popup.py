"""Temporarily expose only the two pre-existing synthetic article fixtures; no publication/broadcast API."""
import importlib.util,json,re,sys
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis/qa';fixture=json.loads((root/'native-article-fixture.json').read_text());category=fixture['category'];assert re.fullmatch('本机资讯验收[a-f0-9]{8}',category)
ids=[int(value) for value in fixture['articleIds']];assert len(ids)==2
baseline=root/'native-popup-baseline.json';stage=sys.argv[1];assert stage in ['first','second','restore']
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_article WHERE id IN ({','.join(map(str,ids))}) AND category='{category}'")=='2'
if stage=='first':
 assert not baseline.exists() or json.loads(baseline.read_text()).get('restored') is True,'Inspect previous popup baseline before preparing again'
 rows=staff.config.sql(f"SELECT JSON_OBJECT('id',id,'status',status,'popup',popup_enabled,'publishedAt',DATE_FORMAT(published_at,'%Y-%m-%d %H:%i:%s')) FROM lin_article WHERE id IN ({','.join(map(str,ids))}) AND category='{category}'")
 staff.save_private(baseline,{'rows':[json.loads(line) for line in rows.splitlines()],'restored':False})
 staff.config.sql(f"UPDATE lin_article SET status='published',published_at=NOW(),popup_enabled=1 WHERE id={ids[0]} AND category='{category}'")
elif stage=='second':
 assert baseline.exists();staff.config.sql(f"UPDATE lin_article SET status='published',published_at=NOW(),popup_enabled=1 WHERE id={ids[1]} AND category='{category}'")
else:
 saved=json.loads(baseline.read_text())
 for row in saved['rows']:
  assert row['id'] in ids and row['status'] in ['draft','published','archived'] and row['popup'] in [0,1]
  timestamp=row['publishedAt'];assert timestamp is None or re.fullmatch(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}',timestamp)
  literal='NULL' if timestamp is None else "'"+timestamp+"'"
  staff.config.sql(f"UPDATE lin_article SET status='{row['status']}',popup_enabled={row['popup']},published_at={literal} WHERE id={row['id']} AND category='{category}'")
 saved['restored']=True;staff.save_private(baseline,saved)
print(json.dumps({'stage':stage,'syntheticArticleIds':ids,'broadcastCalled':False}))
