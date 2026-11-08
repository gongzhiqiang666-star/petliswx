"""Create a local anonymous qualification through the actual upload contract, never by bypassing ownership."""
import base64,json,secrets,urllib.request

def upload_qualification_fixture():
    boundary='PetlisFixture'+secrets.token_hex(12)
    image=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=')
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qualification.png"\r\nContent-Type: image/png\r\n\r\n').encode()+image+(f'\r\n--{boundary}--\r\n').encode()
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    request=urllib.request.Request('http://127.0.0.1:8082/api/auth/qualifications',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
    with opener.open(request,timeout=15) as response:result=json.load(response)
    if result.get('code')!=200 or not result.get('data',{}).get('uploadToken'):raise RuntimeError('本机匿名资质夹具上传未完成')
    return result['data']
