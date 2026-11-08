package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecimenBarcodeTest {
    // Independent Code39 reference encodings: ZXing Code39Reader, Apache-2.0.
    static final String ALPHABET="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%";
    static final int[] ENCODINGS={0x034,0x121,0x061,0x160,0x031,0x130,0x070,0x025,0x124,0x064,0x109,0x049,0x148,0x019,0x118,0x058,0x00D,0x10C,0x04C,0x01C,0x103,0x043,0x142,0x013,0x112,0x052,0x007,0x106,0x046,0x016,0x181,0x0C1,0x1C0,0x091,0x190,0x0D0,0x085,0x184,0x0C4,0x0A8,0x0A2,0x08A,0x02A};
    PrintService service(String code,AuditService audits){
        var store=mock(FeatureStore.class);var access=mock(AccessService.class);
        when(store.one(anyString(),eq(1))).thenReturn(new HashMap<>() {{ put("orderNo",code);put("hospitalId",1);put("laboratoryId",1);put("hospitalName","医院");put("animalName","合成宠物"); }});
        when(access.current()).thenReturn(new CurrentActor(7,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
        return new PrintService(store,access,audits);
    }
    String decode(String html){
        var svg=Pattern.compile("<svg.*?</svg>").matcher(html);assertTrue(svg.find());String content=svg.group();
        var bars=Pattern.compile("<rect x='(\\d+)' y='5' width='(\\d+)' height='60' fill='black'/>").matcher(content);
        List<Integer> xs=new ArrayList<>(),ws=new ArrayList<>();while(bars.find()){xs.add(Integer.parseInt(bars.group(1)));ws.add(Integer.parseInt(bars.group(2)));}
        assertEquals(0,xs.size()%5);StringBuilder decoded=new StringBuilder();
        for(int b=0;b<xs.size();b+=5){int mask=0;for(int j=0;j<9;j++){int index=b+j/2;int width=j%2==0?ws.get(index):xs.get(index+1)-xs.get(index)-ws.get(index);assertTrue(width==1||width==3);mask=(mask<<1)|(width==3?1:0);}
            if(mask==0x094)decoded.append('*');else{int index=-1;for(int i=0;i<ENCODINGS.length;i++)if(ENCODINGS[i]==mask)index=i;assertTrue(index>=0,"Unrecognized standard Code39 glyph");decoded.append(ALPHABET.charAt(index));}}
        assertEquals('*',decoded.charAt(0));assertEquals('*',decoded.charAt(decoded.length()-1));
        var bounds=Pattern.compile("viewBox='0 0 (\\d+) 80'").matcher(content);assertTrue(bounds.find());int width=Integer.parseInt(bounds.group(1));
        assertTrue(xs.get(0)>=10);assertTrue(width-xs.get(xs.size()-1)-ws.get(ws.size()-1)>=10,"Missing trailing quiet zone / clipped barcode");
        return decoded.substring(1,decoded.length()-1);
    }
    @Test void hyphenIsNotLetterBAndAllStandardGlyphsDecode(){String code=ALPHABET;assertEquals(code,decode(service(code,mock(AuditService.class)).specimenLabel(1)));}
    @Test void longOriginalIdentifierIsNeverClipped(){String code="PETLIS-"+"1234567890".repeat(10);assertEquals(code,decode(service(code,mock(AuditService.class)).specimenLabel(1)));}
    @Test void unsupportedOrTerminatorIdentifiersNeverProduceMisleadingPrintAudit(){for(String code:Arrays.asList(null,"","a-b","A*B","标本123","A".repeat(251))){var audits=mock(AuditService.class);assertThrows(BusinessException.class,()->service(code,audits).specimenLabel(1));verifyNoInteractions(audits);}}
}
