"""Create only isolated UI fixtures, intentionally bypass publication/broadcast; archive own rows after UI QA."""
import importlib.util,json,secrets,sys
from datetime import datetime,timedelta
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
private=Path.home()/'.local/share/petlis/qa/native-article-fixture.json'
if '--archive' in sys.argv:
    f=json.loads(private.read_text());assert f['category'].startswith('本机资讯验收')
    ids=','.join(str(int(value)) for value in f['articleIds'])
    assert staff.config.sql(f"SELECT COUNT(*) FROM lin_article WHERE id IN ({ids}) AND category='{f['category']}'")=='2'
    staff.config.sql(f"UPDATE lin_article SET status='draft' WHERE id IN ({ids}) AND category='{f['category']}'")
    f['archived']=True;staff.save_private(private,f);print('Archived exactly two own synthetic article fixtures; no broadcasts generated')
else:
    assert not private.exists(),'Inspect existing article UI fixture instead of creating duplicates'
    category='本机资讯验收'+secrets.token_hex(4);ids=[]
    for kind,title,live_at in [('article','采样注意事项','NULL'),('live','实验室直播预告',"'"+(datetime.now()+timedelta(days=1)).strftime('%Y-%m-%d %H:%M:%S')+"'")]:
        staff.config.sql(f"INSERT INTO lin_article(category,type,title,summary,content,cover_url,live_at,live_url,popup_enabled,status,published_at) VALUES('{category}','{kind}','{category}{title}','仅本机原生UI测试，不对外发布','<p>本机合成资讯内容，用于检查阅读、点赞与收藏。</p>','',{live_at},'https://petlis.example.test/live',0,'published',NOW())")
        ids.append(int(staff.config.sql(f"SELECT id FROM lin_article WHERE category='{category}' AND type='{kind}'")))
    staff.save_private(private,{'category':category,'articleIds':ids,'archived':False,'publicationApiTested':False,'broadcastEventsCreated':False});print(json.dumps({'articleIds':ids,'broadcastEventsCreated':False}))
