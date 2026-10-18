package com.pardal.kitapligim;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Http {
    public static final class Failure extends IOException { public final int status;public Failure(int status,String message){super(message);this.status=status;} }
    public static JSONObject json(String method,String url,JSONObject body,String token) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
        c.setRequestProperty("User-Agent","Kitapligim/1.0 (personal family reading tracker)");c.setRequestProperty("Accept","application/json");
        if(token!=null){c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Notion-Version","2025-09-03");}
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();
        try(InputStream in=code>=400?c.getErrorStream():c.getInputStream()){
            String raw=in==null?"":new String(readLimited(in,5_000_000),StandardCharsets.UTF_8);
            if(code<200||code>=300){String message=switch(code){case 401->"Notion bağlantı anahtarı geçersiz. Ayarlardan yeniden bağlan.";case 403,404->"Notion kitaplığına erişilemiyor. Bağlantıyı Aile Kitaplığımız sayfasına ekle.";case 429->"İstek sınırına ulaşıldı. Biraz sonra yeniden eşitle.";default->"Bağlantı tamamlanamadı ("+code+"). Yeniden dene.";};throw new Failure(code,message);}
            return new JSONObject(raw);
        }finally{c.disconnect();}
    }
    public static byte[] readLimited(InputStream in,int limit) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("Dosya beklenenden büyük.");out.write(buf,0,n);}return out.toByteArray();}
    public static String getText(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Kitapligim/1.0.1 (personal family reading tracker)");
        c.setRequestProperty("Accept","application/json,text/html");
        try{int code=c.getResponseCode();if(code<200||code>=300)throw new Failure(code,"Kitap kaynağına ulaşılamadı.");try(InputStream in=c.getInputStream()){return new String(readLimited(in,3_000_000),StandardCharsets.UTF_8);}}finally{c.disconnect();}
    }
    public static void download(String url,File output) throws Exception {
        URL u=new URL(url);if(!u.getProtocol().equals("https"))throw new IOException("Güvenli olmayan görsel adresi.");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);
        try{if(c.getResponseCode()!=200)throw new IOException("Kapak indirilemedi.");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(output)){out.write(readLimited(in,8_000_000));}}finally{c.disconnect();}
    }
    public static JSONObject uploadPhoto(String token,JSONObject upload,File file) throws Exception {
        String boundary="Kitapligim"+UUID.randomUUID();URL u=new URL("https://api.notion.com/v1/file_uploads/"+upload.getString("id")+"/send");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Notion-Version","2025-09-03");c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        try{try(OutputStream out=c.getOutputStream()){out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+file.getName()+"\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));try(InputStream in=new FileInputStream(file)){out.write(readLimited(in,8_000_000));}out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));}int status=c.getResponseCode();if(status<200||status>=300)throw new Failure(status,"Kapak fotoğrafı Notion'a yüklenemedi. Yeniden eşitle.");try(InputStream in=c.getInputStream()){return new JSONObject(new String(readLimited(in,100000),StandardCharsets.UTF_8));}}finally{c.disconnect();}
    }
}
