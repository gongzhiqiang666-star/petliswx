"""Run five-tab acceptance against the already-open Petlis automation window.
Does not launch, compile, upload or publish the mini-program.
"""
from pathlib import Path
import os, subprocess, sys

scripts=Path(__file__).resolve().parent
state=Path.home()/'.local/share/petlis/simulator-fixture.json'
def python(name):
    subprocess.run([sys.executable,str(scripts/name)],check=True)
if state.exists():
    python('cleanup-simulator.py')
try:
    python('prepare-simulator.py')
    env=dict(os.environ)
    for key in ('NODE_OPTIONS','HTTP_PROXY','HTTPS_PROXY','ALL_PROXY','http_proxy','https_proxy','all_proxy'):
        env.pop(key,None)
    result=subprocess.run(['node',str(scripts/'test-tabs.js')],env=env,timeout=300)
    raise SystemExit(result.returncode)
finally:
    if state.exists():python('cleanup-simulator.py')
