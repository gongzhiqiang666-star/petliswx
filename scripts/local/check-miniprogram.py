"""Compile every native template/style, including imported files, and parse JavaScript.
This verifies source syntax only, not simulator rendering or device behavior.
"""
from pathlib import Path
import os, subprocess, tempfile, json, re

root = Path(__file__).resolve().parents[2] / 'miniprogram'
# Mini-program runtime does not load JSON through CommonJS require.
for source in root.rglob('*.js'):
    if re.search(r"require\(\s*['\"][^'\"]+\.json['\"]\s*\)", source.read_text()):
        raise SystemExit(f'Unsupported runtime JSON require: {source.relative_to(root)}')
routes_source=(root/'config/routes.js').read_text().split('module.exports = ',1)[1]
app_config=json.loads((root/'app.json').read_text())
assert json.loads(routes_source)==app_config['pages'], 'Route whitelist must match registered pages'
# Tab navigation must reference registered pages and existing icons, and no
# wx.navigateTo may target a tab page (the runtime rejects those calls).
tabs=[item['pagePath'] for item in app_config.get('tabBar',{}).get('list',[])]
for item in app_config.get('tabBar',{}).get('list',[]):
    assert item['pagePath'] in app_config['pages'], f"Unregistered tab page: {item['pagePath']}"
    for key in ('iconPath','selectedIconPath'):
        assert (root/item[key]).is_file(), f"Missing tab icon: {item[key]}"
for source in root.rglob('*.js'):
    for match in re.finditer(r"navigateTo\(\s*\{[^}]*?url\s*:\s*['\"]/?(pages/[a-z-]+/[a-z-]+)", source.read_text(), re.S):
        assert match.group(1) not in tabs, f'navigateTo targets tab page {match.group(1)}: {source.relative_to(root)}'
print(f'{len(tabs)} tab pages and icons verified', flush=True)
compiler = Path(os.environ.get('PETLIS_WECHAT_COMPILER_DIR',
    '/Applications/wechatwebdevtools.app/Contents/Resources/app.asar.unpacked/node_modules/wcc-exec'))
with tempfile.TemporaryDirectory(prefix='petlis-compile-') as output:
    for executable, extension in [('wcc', 'wxml'), ('wcsc', 'wxss')]:
        files = sorted(str(path.relative_to(root)) for path in root.rglob('*.' + extension))
        subprocess.run([str(compiler / executable), '-o', str(Path(output) / (extension + '.js')), *files], cwd=root, check=True)
        print(f'{len(files)} {extension.upper()} files compiled', flush=True)
    scripts = sorted(root.rglob('*.js'))
    env = dict(os.environ)
    env.pop('NODE_OPTIONS', None)
    for script in scripts:
        subprocess.run(['node', '--check', str(script)], env=env, check=True, capture_output=True)
    print(f'{len(scripts)} JavaScript files parsed', flush=True)
