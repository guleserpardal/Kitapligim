package com.pardal.kitapligim;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Talks to the family's own PC server (server/), which runs Claude Code. Address and key are stored encrypted on this phone. */
public final class Assistant {
    private final SecretStore store;
    public Assistant(SecretStore store){this.store=store;}

    static String normalizeUrl(String raw) throws Exception {
        String u=raw==null?"":raw.trim();if(u.isEmpty())return "";
        if(!u.matches("(?i)^https?://.*"))u="http://"+u;
        URI uri;try{uri=new URI(u);}catch(URISyntaxException e){throw new Exception("Sunucu adresi geçersiz. Örnek: http://192.168.1.12:8790");}
        if(uri.getHost()==null||(uri.getPath()!=null&&!uri.getPath().isEmpty()&&!uri.getPath().equals("/"))||uri.getQuery()!=null||uri.getUserInfo()!=null)throw new Exception("Sunucu adresi geçersiz. Örnek: http://192.168.1.12:8790");
        return uri.getScheme().toLowerCase()+"://"+uri.getHost()+(uri.getPort()>0?":"+uri.getPort():"");
    }

    private JSONObject config() throws Exception {String s=store.get();return s.isEmpty()?new JSONObject():new JSONObject(s);}
    /** Public view for the UI: never includes the key. */
    public JSONObject summary() throws Exception {JSONObject c=config();return new JSONObject().put("configured",!c.optString("url").isEmpty()&&!c.optString("token").isEmpty()).put("url",c.optString("url")).put("altUrl",c.optString("altUrl"));}
    public void save(String url,String altUrl,String token) throws Exception {
        JSONObject old=config();String u=normalizeUrl(url),alt=normalizeUrl(altUrl),t=token==null?"":token.trim();
        if(u.isEmpty())throw new Exception("Sunucu adresini gir.");
        if(t.isEmpty())t=old.optString("token");
        if(t.isEmpty())throw new Exception("Sunucu anahtarını gir.");
        store.save(new JSONObject().put("url",u).put("altUrl",alt).put("token",t).toString());
    }
    public void clear(){store.clear();}

    public JSONObject test() throws Exception {JSONObject c=requireConfig();JSONObject r=send("GET","/api/ping",null,c,15000);return new JSONObject().put("ok",r.optBoolean("ok"));}
    public JSONObject chat(JSONObject body) throws Exception {return send("POST","/api/chat",body,requireConfig(),140000);}

    private JSONObject requireConfig() throws Exception {JSONObject c=config();if(c.optString("url").isEmpty()||c.optString("token").isEmpty())throw new Exception("Kitap Danışmanı henüz kurulmadı. Ayarlar'dan PC sunucusunun adresini ve anahtarını gir.");return c;}

    /** Tries the home address first, then the Tailscale one. Only unreachable servers fall through; real answers (even errors) stop the search. */
    private JSONObject send(String method,String path,JSONObject body,JSONObject c,int readTimeout) throws Exception {
        String[] urls=c.optString("altUrl").isEmpty()?new String[]{c.optString("url")}:new String[]{c.optString("url"),c.optString("altUrl")};
        for(String base:urls){
            HttpURLConnection h=null;
            try{
                h=(HttpURLConnection)new URL(base+path).openConnection();h.setConnectTimeout(4000);h.setReadTimeout(readTimeout);h.setInstanceFollowRedirects(false);h.setRequestMethod(method);
                h.setRequestProperty("X-Token",c.getString("token"));h.setRequestProperty("Accept","application/json");
                if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream out=h.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
                int code=h.getResponseCode();
                String raw;try(InputStream in=code>=400?h.getErrorStream():h.getInputStream()){raw=in==null?"":new String(Http.readLimited(in,2_000_000),StandardCharsets.UTF_8);}
                if(code>=200&&code<300)return new JSONObject(raw);
                if(code==401)throw new Exception("Sunucu anahtarı yanlış. Ayarlar'da Kitap Danışmanı anahtarını kontrol et.");
                String msg=null;try{msg=new JSONObject(raw).optString("error",null);}catch(Exception ignored){}
                throw new Exception(msg==null||msg.isEmpty()?"Danışman şu an yanıt veremiyor ("+code+"). Biraz sonra yeniden dene.":msg);
            }catch(ConnectException|NoRouteToHostException|UnknownHostException e){
                // unreachable: try the next address
            }catch(SocketTimeoutException e){
                if(method.equals("GET")||e.getMessage()!=null&&e.getMessage().toLowerCase().contains("connect"))continue;
                throw new Exception("Danışman çok uzun düşündü. Biraz sonra yeniden dene.");
            }catch(IOException e){
                String m=e.getMessage()==null?"":e.getMessage();
                if(m.contains("Cleartext"))throw new Exception("Bu adrese şifresiz bağlantı izni yok. Adres 192.168.1.12 veya Tailscale adresi (100.68.128.112) olmalı; başka bir adres için network_security_config.xml'e ekleyip yeni APK derlemek gerekir.");
            }finally{if(h!=null)h.disconnect();}
        }
        throw new Exception("PC'deki sunucuya ulaşılamıyor. PC açık mı, aynı ağda mısın? Evin dışındaysan Tailscale'in açık olduğundan emin ol.");
    }
}
