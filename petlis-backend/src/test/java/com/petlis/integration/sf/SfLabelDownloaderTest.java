package com.petlis.integration.sf;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class SfLabelDownloaderTest {
    static final String URL="https://eos-scp-core-shenzhen-futian1-oss.sf-express.com/label.pdf";
    @Test void tokenCannotBeSentToUntrustedHostsOrRedirectTargets() {
        var calls=new AtomicInteger();
        var download=new SfLabelDownloader((uri,token)->{calls.incrementAndGet();return "%PDF-test".getBytes();});
        for(String url:new String[]{"http://eos-scp-core-shenzhen-futian1-oss.sf-express.com/x","https://127.0.0.1/x","https://eos-scp-core-shenzhen-futian1-oss.sf-express.com.evil.example/x","https://user@eos-scp-core-shenzhen-futian1-oss.sf-express.com/x",URL+"#fragment"})
            assertThrows(IllegalArgumentException.class,()->download.download(url,"fixture"));
        assertEquals(0,calls.get());
    }
    @Test void acceptsPdfButRejectsHtmlAndOversizedFiles() throws Exception {
        assertArrayEquals("%PDF-test".getBytes(),new SfLabelDownloader((uri,token)->"%PDF-test".getBytes()).download(URL,"fixture"));
        assertThrows(IllegalStateException.class,()->new SfLabelDownloader((uri,token)->"<html>error".getBytes()).download(URL,"fixture"));
        assertThrows(IllegalStateException.class,()->new SfLabelDownloader((uri,token)->new byte[SfLabelDownloader.MAX_BYTES+1]).download(URL,"fixture"));
    }
}
