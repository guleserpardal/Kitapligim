package com.pardal.kitapligim;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BookStore {
    public static final String DATA_SOURCE="a77420e6-f540-49a2-84c0-45c3bffa711a";
    private final AtomicFile file;
    private JSONObject data;
    public BookStore(Context context) throws Exception {
        file=new AtomicFile(new File(context.getFilesDir(),"books-v1.json"));
        if(file.getBaseFile().exists()) {try(InputStream in=file.openRead()){data=new JSONObject(new String(Http.readLimited(in,50_000_000),StandardCharsets.UTF_8));}}
        else data=new JSONObject().put("version",1).put("books",new JSONArray()).put("profile","Güleser").put("goals",new JSONObject()).put("lastSync", "").put("syncError", "");
    }
    public synchronized JSONObject state() throws JSONException { return Book.copy(data); }
    public synchronized JSONObject book(String id) throws JSONException {JSONArray a=data.getJSONArray("books");for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).optString("id")))return Book.copy(a.getJSONObject(i));return null;}
    public synchronized void put(JSONObject b) throws Exception {
        JSONArray a=data.getJSONArray("books"); boolean found=false;
        for(int i=0;i<a.length();i++)if(b.getString("id").equals(a.getJSONObject(i).optString("id"))){a.put(i,Book.copy(b));found=true;break;}
        if(!found)a.put(Book.copy(b));persist();
    }
    public synchronized JSONObject save(JSONObject input) throws Exception {
        JSONObject b=Book.normalize(input);String id=b.optString("id"); JSONObject old=id.isEmpty()?null:book(id);
        if(old!=null && "conflict".equals(old.optString("syncStatus"))) throw new IllegalArgumentException("Önce bu kitabın iki sürümünü karşılaştırıp birini seç.");
        if(id.isEmpty())id=UUID.randomUUID().toString();
        b.put("id",id).put("updatedAt",Book.now()).put("syncStatus","pending");
        if(old!=null)for(String k:List.of("remotePageId","remoteEditedAt","uploadedPhotoKey"))if(old.has(k))b.put(k,old.get(k));
        b.remove("conflictRemote");put(b);return b;
    }
    public synchronized void delete(String id) throws Exception {JSONObject b=book(id);if(b==null)return;if("conflict".equals(b.optString("syncStatus")))throw new IllegalArgumentException("Önce sürüm çakışmasını çöz.");b.put("deleted",true).put("syncStatus","pending").put("updatedAt",Book.now());put(b);}
    public synchronized void settings(JSONObject s) throws Exception {if(s.has("profile")){if(!Book.PROFILES.contains(s.getString("profile")))throw new IllegalArgumentException("Profil bulunamadı.");data.put("profile",s.getString("profile"));}if(s.has("goal")){int n=s.getInt("goal");if(n<1||n>1000)throw new IllegalArgumentException("Hedef 1–1000 kitap olabilir.");data.getJSONObject("goals").put(data.getString("profile"),n);}persist();}
    public synchronized void syncResult(String error) throws Exception {data.put("syncError",error);if(error.isEmpty())data.put("lastSync",Book.now());persist();}
    private void persist() throws Exception {FileOutputStream out=null;try{out=file.startWrite();out.write(data.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
}
