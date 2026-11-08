package com.petlis.integration.sf;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Private carrier tokens never leave the verified SF file origin and never reach client responses. */
public final class SfLabelDownloader {
    public static final int MAX_BYTES=10*1024*1024;
    private static final String FILE_HOST="eos-scp-core-shenzhen-futian1-oss.sf-express.com";
    public interface Transport { byte[] get(URI uri,String token) throws Exception; }
    private final Transport transport;
    public SfLabelDownloader(Transport transport) {this.transport=transport;}
    public byte[] download(String url,String token) throws Exception {
        URI uri=URI.create(url);
        if(!"https".equals(uri.getScheme()) || !FILE_HOST.equals(uri.getHost()) || (uri.getPort()!=-1 && uri.getPort()!=443)
                || uri.getUserInfo()!=null || uri.getFragment()!=null || token==null || token.isBlank() || token.contains("\r") || token.contains("\n"))
            throw new IllegalArgumentException("顺丰面单下载地址或令牌无效");
        byte[] data=transport.get(uri,token);
        if(data==null || data.length<5 || data.length>MAX_BYTES || data[0]!='%' || data[1]!='P' || data[2]!='D' || data[3]!='F' || data[4]!='-')
            throw new IllegalStateException("顺丰面单不是有效大小的PDF文件");
        return data;
    }
    public static Transport httpTransport() {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
        return (uri,token)->{
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).header("X-Auth-token",token).GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var body=response.body()) {
                if(response.statusCode()!=200) throw new IllegalStateException("顺丰面单下载暂未成功");
                return body.readNBytes(MAX_BYTES+1);
            }
        };
    }
}
