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
        if(b.has("readingDays")){JSONArray rows=ReadingProgress.rebase(book(b.getString("id")),b,ReadingProgress.day(),ReadingProgress.stamp());if(!ReadingProgress.key(rows).equals(ReadingProgress.key(b.getJSONArray("readingDays"))))b=Book.copy(b).put("readingDays",rows).put("syncStatus","pending");}
        JSONArray a=data.getJSONArray("books"); boolean found=false;
        for(int i=0;i<a.length();i++)if(b.getString("id").equals(a.getJSONObject(i).optString("id"))){a.put(i,Book.copy(b));found=true;break;}
        if(!found)a.put(Book.copy(b));persist();
    }
    public synchronized JSONObject save(JSONObject input) throws Exception {
        JSONObject clean=Book.copy(input);boolean track=clean.optBoolean("_recordReading");clean.remove("_recordReading");
        JSONObject b=Book.normalize(clean);String id=b.optString("id"); JSONObject old=id.isEmpty()?null:book(id);
        if(old!=null && "conflict".equals(old.optString("syncStatus"))) throw new IllegalArgumentException("Önce bu kitabın iki sürümünü karşılaştırıp birini seç.");
        if(id.isEmpty())id=UUID.randomUUID().toString();
        b.put("id",id).put("updatedAt",Book.now()).put("syncStatus","pending");
        if(old!=null)for(String k:List.of("remotePageId","remoteEditedAt","uploadedPhotoKey"))if(old.has(k))b.put(k,old.get(k));
        JSONArray history=old==null?new JSONArray():ReadingProgress.normalize(old.optJSONArray("readingDays"));
        if(track)history=ReadingProgress.record(old,b,ReadingProgress.day(),data.optJSONObject("dailyGoals")!=null&&data.getJSONObject("dailyGoals").has(b.getString("profile"))?data.getJSONObject("dailyGoals").getInt(b.getString("profile")):ReadingProgress.latestGoal(data.getJSONArray("books"),b.getString("profile")),ReadingProgress.stamp());
        if(old!=null&&old.has("readingDays")||history.length()>0)b.put("readingDays",history);else b.remove("readingDays");
        b.remove("conflictRemote");put(b);return b;
    }
    public synchronized void delete(String id) throws Exception {JSONObject b=book(id);if(b==null)return;if("conflict".equals(b.optString("syncStatus")))throw new IllegalArgumentException("Önce sürüm çakışmasını çöz.");b.put("deleted",true).put("syncStatus","pending").put("updatedAt",Book.now());put(b);}
    public synchronized void settings(JSONObject s) throws Exception {if(s.has("profile")){if(!Book.PROFILES.contains(s.getString("profile")))throw new IllegalArgumentException("Profil bulunamadı.");data.put("profile",s.getString("profile"));}if(s.has("goal")){int n=s.getInt("goal");if(n<1||n>1000)throw new IllegalArgumentException("Hedef 1–1000 kitap olabilir.");data.getJSONObject("goals").put(data.getString("profile"),n);}if(s.has("dailyGoal")){
        double n=s.getDouble("dailyGoal");if(!Double.isFinite(n)||n!=Math.floor(n)||n<1||n>1000)throw new IllegalArgumentException("Günlük hedef 1–1000 sayfa olabilir.");
        if(data.optJSONObject("dailyGoals")==null)data.put("dailyGoals",new JSONObject());String profile=data.getString("profile"),date=ReadingProgress.day(),stamp=ReadingProgress.stamp();data.getJSONObject("dailyGoals").put(profile,(int)n);
        JSONArray books=data.getJSONArray("books");for(int i=0;i<books.length();i++){JSONObject b=books.getJSONObject(i);if(!b.has("readingDays")||"conflict".equals(b.optString("syncStatus")))continue;JSONArray rows=ReadingProgress.goalForToday(b.getJSONArray("readingDays"),profile,date,(int)n,stamp);if(!ReadingProgress.key(rows).equals(ReadingProgress.key(b.getJSONArray("readingDays"))))b.put("readingDays",rows).put("syncStatus","pending");}
    }persist();}
    public synchronized void readingSyncWarning(String warning) throws Exception {data.put("readingSyncWarning",warning);persist();}
    public synchronized void syncResult(String error) throws Exception {data.put("syncError",error);if(error.isEmpty())data.put("lastSync",Book.now());persist();}
    private void persist() throws Exception {FileOutputStream out=null;try{out=file.startWrite();out.write(data.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
}
