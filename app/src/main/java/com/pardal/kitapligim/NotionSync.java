package com.pardal.kitapligim;

import org.json.*;
import java.io.*;
import java.util.*;

/** All calls run on the same executor as local writes, so a late response cannot erase an edit. */
public final class NotionSync {
    private final BookStore store;private final String token;private final File covers;private long lastRequest;
    public NotionSync(BookStore store,String token,File covers){this.store=store;this.token=token;this.covers=covers;}
    private JSONObject call(String method,String path,JSONObject body) throws Exception {long delay=370-(System.currentTimeMillis()-lastRequest);if(delay>0)Thread.sleep(delay);lastRequest=System.currentTimeMillis();return Http.json(method,"https://api.notion.com/v1"+path,body,token);}
    public void check() throws Exception {
        JSONObject source=call("GET","/data_sources/"+BookStore.DATA_SOURCE,null);JSONObject p=source.getJSONObject("properties");
        for(String name:List.of("Kitap","Profil","Durum","Yazar","ISBN","Sayfa Sayısı","Kaldığım Sayfa","Puan","Notlar","Başlama","Bitiş","Kapak","Uygulama ID","Silindi","Kapak Fotoğrafı"))if(!p.has(name))throw new Exception("Notion kitaplığında '"+name+"' alanı eksik. Alan adını geri yükle.");
    }
    public void run() throws Exception {
        if(token.isEmpty())return;
        // Purchase columns were added later; families whose table lacks them keep syncing everything else.
        JSONObject columns=call("GET","/data_sources/"+BookStore.DATA_SOURCE,null).getJSONObject("properties");
        Map<String,JSONObject> remote=new LinkedHashMap<>();String cursor="";int count=0;
        do {JSONObject q=new JSONObject().put("page_size",100);if(!cursor.isEmpty())q.put("start_cursor",cursor);JSONObject result=call("POST","/data_sources/"+BookStore.DATA_SOURCE+"/query",q);JSONArray pages=result.getJSONArray("results");for(int i=0;i<pages.length();i++){JSONObject page=pages.getJSONObject(i);JSONObject b=NotionCodec.fromPage(page);String id=b.getString("id");if(remote.containsKey(id))throw new Exception("Notion'da aynı Uygulama ID ile iki kayıt var. Yinelenen satırı kontrol et.");remote.put(id,b);}cursor=result.optBoolean("has_more")?result.optString("next_cursor",""):"";if(++count>100)throw new Exception("Kitaplık çok büyük; eşitleme tamamlanmadı.");}while(!cursor.isEmpty());
        JSONArray local=store.state().getJSONArray("books");Set<String> seenPages=new HashSet<>();for(JSONObject b:remote.values())seenPages.add(b.optString("remotePageId"));
        // A Notion row moved to trash disappears from query; retrieve known missing pages explicitly.
        for(int i=0;i<local.length();i++){JSONObject b=local.getJSONObject(i);String pid=b.optString("remotePageId");if(!pid.isEmpty()&&!seenPages.contains(pid)){JSONObject page=call("GET","/pages/"+pid,null);JSONObject rb=NotionCodec.fromPage(page);remote.put(b.getString("id"),rb);}}
        for(Map.Entry<String,JSONObject> entry:remote.entrySet()){
            String id=entry.getKey();JSONObject rb=entry.getValue();JSONObject lb=store.book(id);
            if(lb==null){ // Recover a saved page even if Notion's ID field was cleared.
                JSONArray a=store.state().getJSONArray("books");for(int i=0;i<a.length();i++)if(rb.optString("remotePageId").equals(a.getJSONObject(i).optString("remotePageId"))){lb=a.getJSONObject(i);id=lb.getString("id");break;}
            }
            JSONObject merged=SyncMerge.merge(lb,rb);merged.put("id",id);
            if("synced".equals(merged.optString("syncStatus")))cacheRemotePhoto(merged,rb,lb);
            store.put(merged);
        }
        local=store.state().getJSONArray("books");
        for(int i=0;i<local.length();i++){
            JSONObject b=local.getJSONObject(i);if(!"pending".equals(b.optString("syncStatus")))continue;
            if(b.optBoolean("deleted") && b.optString("remotePageId").isEmpty()){b.put("syncStatus","synced");store.put(b);continue;}
            String pid=b.optString("remotePageId");
            if(!pid.isEmpty()) {JSONObject latest=NotionCodec.fromPage(call("GET","/pages/"+pid,null));JSONObject merged=SyncMerge.merge(b,latest);if("conflict".equals(merged.optString("syncStatus"))){store.put(merged);continue;}}
            JSONObject props=NotionCodec.toProperties(b);for(String name:NotionCodec.PURCHASE_COLUMNS)if(!columns.has(name))props.remove(name);JSONObject photo=null;
            if(!b.optString("photoKey").isEmpty()&&!b.getString("photoKey").equals(b.optString("uploadedPhotoKey"))){File f=new File(covers,b.getString("photoKey"));if(!f.getCanonicalPath().startsWith(covers.getCanonicalPath()+File.separator)||!f.isFile())throw new Exception("Kitabın kapak fotoğrafı bulunamadı.");JSONObject up=call("POST","/file_uploads",new JSONObject().put("mode","single_part").put("filename",f.getName()).put("content_type","image/jpeg"));Http.uploadPhoto(token,up,f);photo=new JSONObject().put("type","file_upload").put("file_upload",new JSONObject().put("id",up.getString("id")));props.put("Kapak Fotoğrafı",new JSONObject().put("files",new JSONArray().put(Book.copy(photo).put("name",b.getString("photoKey")))));}
            JSONObject body=new JSONObject().put("properties",props);if(!pid.isEmpty())body.put("in_trash",false);if(photo!=null)body.put("cover",photo);else if(!b.optString("cover").isEmpty())body.put("cover",new JSONObject().put("type","external").put("external",new JSONObject().put("url",b.getString("cover"))));
            JSONObject response;if(pid.isEmpty()){body.put("parent",new JSONObject().put("type","data_source_id").put("data_source_id",BookStore.DATA_SOURCE));response=call("POST","/pages",body);}else response=call("PATCH","/pages/"+pid,body);
            JSONObject updated=NotionCodec.fromPage(response);JSONObject saved=SyncMerge.synced(b,updated);saved.put("uploadedPhotoKey",b.optString("photoKey"));store.put(saved);
        }
        store.syncResult("");
    }
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(int i=0;i<12;i++)s.append(String.format(java.util.Locale.ROOT,"%02x",bytes[i]&255));return s.toString();}
    private void cacheRemotePhoto(JSONObject target,JSONObject remote,JSONObject previous) throws Exception {
        String name=remote.optString("photoKey");if(name.isEmpty()){target.remove("localCover");return;}
        String hash=hex(java.security.MessageDigest.getInstance("SHA-256").digest((remote.getString("remotePageId")+"/"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String localName="notion-"+hash+".jpg";File f=new File(covers,localName);
        boolean changed=previous!=null&&!previous.optString("remoteEditedAt").equals(remote.optString("remoteEditedAt"));
        if((!f.exists()||changed)&&!remote.optString("remotePhotoUrl").isEmpty()){File temp=new File(covers,localName+".part");Http.download(remote.getString("remotePhotoUrl"),temp);if(!temp.renameTo(f))throw new IOException("Kapak kaydedilemedi.");}
        if(f.exists())target.put("localCover",localName).put("uploadedPhotoKey",name);
        target.remove("remotePhotoUrl");
    }
}
