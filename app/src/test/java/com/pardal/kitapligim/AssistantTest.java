package com.pardal.kitapligim;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class AssistantTest {
    static final class Mem implements SecretStore {String v="";public String get(){return v;}public void save(String s){v=s;}public void clear(){v="";}}

    @Test public void normalizesAddresses() throws Exception {
        assertEquals("http://192.168.1.12:8790",Assistant.normalizeUrl(" 192.168.1.12:8790/ "));
        assertEquals("https://pc.example.net",Assistant.normalizeUrl("HTTPS://pc.example.net"));
        assertEquals("",Assistant.normalizeUrl(""));
        for(String bad:new String[]{"http://a:b@host:1","http://host/api","http://host?x=1","http://"})try{Assistant.normalizeUrl(bad);fail(bad);}catch(Exception expected){}
    }

    @Test public void summaryNeverExposesKeyAndEmptyTokenKeepsOld() throws Exception {
        Mem m=new Mem();Assistant a=new Assistant(m);
        assertFalse(a.summary().getBoolean("configured"));
        a.save("192.168.1.12:8790","","anahtar");
        a.save("192.168.1.12:8790","100.68.128.112:8790","");
        JSONObject s=a.summary();
        assertTrue(s.getBoolean("configured"));assertFalse(s.toString().contains("anahtar"));
        assertEquals("anahtar",new JSONObject(m.v).getString("token"));
        try{new Assistant(new Mem()).save("host","","");fail();}catch(Exception expected){}
    }

    @Test public void chatSendsKeyAndFallsBackToSecondAddress() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/chat",ex->{byte[] body;int code;if("sirri".equals(ex.getRequestHeaders().getFirst("X-Token"))){code=200;body="{\"reply\":\"merhaba\",\"books\":[]}".getBytes(StandardCharsets.UTF_8);}else{code=401;body="{}".getBytes();}ex.sendResponseHeaders(code,body.length);ex.getResponseBody().write(body);ex.close();});
        server.start();
        try{
            Mem m=new Mem();Assistant a=new Assistant(m);
            a.save("http://127.0.0.1:1","http://127.0.0.1:"+server.getAddress().getPort(),"sirri");
            assertEquals("merhaba",a.chat(new JSONObject().put("messages",new org.json.JSONArray())).getString("reply"));
            a.save("http://127.0.0.1:"+server.getAddress().getPort(),"","yanlis");
            try{a.chat(new JSONObject());fail();}catch(Exception e){assertTrue(e.getMessage().contains("anahtar"));}
            a.save("http://127.0.0.1:1","","sirri");
            try{a.chat(new JSONObject());fail();}catch(Exception e){assertTrue(e.getMessage().contains("ulaşılamıyor"));}
        }finally{server.stop(0);}
    }
}
