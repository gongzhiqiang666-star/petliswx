#!/bin/bash
set -euo pipefail
# Operate only the named Petlis preview; never restart Docker/Colima or other containers.
python3 - "${1:-status}" <<'PY'
from pathlib import Path
import json,subprocess,sys
name='petlis-legacy-php-preview'
root=Path.home()/'.local/share/petlis'
mode=sys.argv[1]
assert mode in ('start','status'), 'Supported commands: start, status'
result=subprocess.run(['docker','inspect',name],capture_output=True,text=True)
if result.returncode==0:
    item=json.loads(result.stdout)[0]
    assert any(m['Source']==str(root/'php-preview/app') and m['Destination']=='/srv/petlis' for m in item['Mounts']), 'Container does not match Petlis preview'
    assert any(m['Source']==str(root/'reports') and m['Destination']=='/srv/petlis-private-reports' and not m['RW'] for m in item['Mounts']), 'Private PDF mount must be read-only'
    assert item['HostConfig']['PortBindings']=={'8091/tcp':[{'HostIp':'127.0.0.1','HostPort':'8091'}]}, 'Unexpected port bindings'
    if mode=='start' and not item['State']['Running']: subprocess.run(['docker','start',name],check=True,stdout=subprocess.DEVNULL)
    print('Petlis PHP preview: '+('running' if mode=='start' or item['State']['Running'] else 'stopped'))
elif mode=='status':
    print('Petlis PHP preview container not found')
else:
    assert (root/'php-preview/app/.env').is_file() and (root/'reports').is_dir(), 'Private local preview/configuration must be prepared first'
    subprocess.run(['docker','image','inspect','petlis-legacy-php-local:7.4'],check=True,stdout=subprocess.DEVNULL)
    subprocess.run(['docker','run','-d','--platform','linux/amd64','--name',name,'-p','127.0.0.1:8091:8091','--mount',f'type=bind,source={root}/php-preview/app,target=/srv/petlis','--mount',f'type=bind,source={root}/reports,target=/srv/petlis-private-reports,readonly','-w','/srv/petlis','petlis-legacy-php-local:7.4','php7.4','-S','0.0.0.0:8091','-t','public'],check=True,stdout=subprocess.DEVNULL)
    print('Started only Petlis PHP preview')
print('http://127.0.0.1:8091/laboklin.php/index/login')
PY
