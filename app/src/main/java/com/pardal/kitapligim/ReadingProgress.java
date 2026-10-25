package com.pardal.kitapligim;

import org.json.*;
import java.time.*;
import java.util.*;

/** One adjustable record per book, reader and local day. Existing positions are baselines, not new reading. */
public final class ReadingProgress {
    public static final String COLUMN="Okuma Günlüğü";
    public static String day(){return LocalDate.now().toString();}
    public static String stamp(){return Instant.ofEpochMilli(System.currentTimeMillis()).toString();}
    private static int bounded(JSONObject x,String key,int max) {
        Object raw=x.opt(key);if(!(raw instanceof Number))throw new IllegalArgumentException("Okuma günlüğündeki sayı geçersiz.");
        double d=((Number)raw).doubleValue();if(!Double.isFinite(d)||d!=Math.floor(d)||d<0||d>max)throw new IllegalArgumentException("Okuma günlüğündeki sayı geçersiz.");return (int)d;
    }
    public static JSONArray normalize(JSONArray input) throws JSONException {
        TreeMap<String,JSONObject> rows=new TreeMap<>();if(input==null)return new JSONArray();
        if(input.length()>2500)throw new IllegalArgumentException("Bu kitabın okuma günlüğü çok büyük.");
        for(int i=0;i<input.length();i++){
            JSONObject x=input.getJSONObject(i);String date=x.getString("date"),profile=x.getString("profile");
            if(!LocalDate.parse(date).toString().equals(date)||!Book.PROFILES.contains(profile))throw new IllegalArgumentException("Okuma günü veya profil geçersiz.");
            int start=bounded(x,"start",100000),end=bounded(x,"end",100000),goal=bounded(x,"goal",1000);
            String updated=Instant.ofEpochMilli(Instant.parse(x.getString("updatedAt")).toEpochMilli()).toString();
            JSONObject row=new JSONObject().put("date",date).put("profile",profile).put("start",start).put("end",end).put("goal",goal).put("updatedAt",updated);
            String key=date+"/"+profile;JSONObject old=rows.get(key);
            if(old!=null){JSONObject newest=Instant.parse(updated).compareTo(Instant.parse(old.getString("updatedAt")))>=0?row:old;row=Book.copy(newest);if(row.getInt("goal")==0)row.put("goal",old.getInt("goal"));}
            rows.put(key,row);
        }
        JSONArray out=new JSONArray();for(JSONObject row:rows.values())out.put(row);return out;
    }
    public static JSONArray merge(JSONArray a,JSONArray b) throws JSONException {
        JSONArray all=new JSONArray();if(a!=null)for(int i=0;i<a.length();i++)all.put(a.get(i));if(b!=null)for(int i=0;i<b.length();i++)all.put(b.get(i));
        // Each input is bounded; de-duplicate before validating the total history size.
        TreeMap<String,JSONObject> rows=new TreeMap<>();
        for(int i=0;i<all.length();i++){JSONObject x=all.getJSONObject(i);String k=x.getString("date")+"/"+x.getString("profile");JSONObject old=rows.get(k);if(old==null)rows.put(k,Book.copy(x));else{JSONObject newest=Instant.parse(x.getString("updatedAt")).compareTo(Instant.parse(old.getString("updatedAt")))>=0?Book.copy(x):Book.copy(old);if(newest.getInt("goal")==0)newest.put("goal",Math.max(x.getInt("goal"),old.getInt("goal")));rows.put(k,newest);}}
        JSONArray out=new JSONArray();for(JSONObject x:rows.values())out.put(x);return normalize(out);
    }
    public static JSONArray record(JSONObject old,JSONObject next,String date,int goal,String updated) throws JSONException {
        JSONArray rows=normalize(old==null?null:old.optJSONArray("readingDays"));
        if(old==null||!old.optString("profile").equals(next.optString("profile"))||old.optInt("currentPage")==next.optInt("currentPage"))return rows;
        String profile=next.getString("profile");int i=0;for(;i<rows.length();i++)if(date.equals(rows.getJSONObject(i).getString("date"))&&profile.equals(rows.getJSONObject(i).getString("profile")))break;
        int start=i<rows.length()?rows.getJSONObject(i).getInt("start"):old.optInt("currentPage");if(goal==0&&i<rows.length())goal=rows.getJSONObject(i).getInt("goal");
        rows.put(i,new JSONObject().put("date",date).put("profile",profile).put("start",start).put("end",next.optInt("currentPage")).put("goal",goal).put("updatedAt",updated));return normalize(rows);
    }
    public static JSONArray goalForToday(JSONArray input,String profile,String date,int goal,String updated) throws JSONException {
        JSONArray rows=normalize(input);for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);if(profile.equals(x.getString("profile"))&&date.equals(x.getString("date")))x.put("goal",goal).put("updatedAt",updated);}return rows;
    }
    /** A status/import/Notion position change is a new baseline, never extra reading. */
    public static JSONArray rebase(JSONObject old,JSONObject next,String date,String updated) throws JSONException {
        JSONArray rows=normalize(next.optJSONArray("readingDays"));if(old==null||!old.optString("profile").equals(next.optString("profile"))||old.optInt("currentPage")==next.optInt("currentPage"))return rows;
        for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);if(date.equals(x.getString("date"))&&next.getString("profile").equals(x.getString("profile"))&&x.getInt("end")!=next.optInt("currentPage")){
            int pages=Math.max(0,x.getInt("end")-x.getInt("start")),end=next.optInt("currentPage");x.put("start",Math.max(0,end-pages)).put("end",end).put("updatedAt",updated);
        }}return rows;
    }
    public static String key(JSONArray input) throws JSONException {return encode(normalize(input));}
    public static int latestGoal(JSONArray books,String profile) throws JSONException {
        String lastDate="";Instant last=Instant.MIN;int goal=0;
        for(int i=0;i<books.length();i++){JSONArray rows=normalize(books.getJSONObject(i).optJSONArray("readingDays"));for(int j=0;j<rows.length();j++){
            JSONObject x=rows.getJSONObject(j);String date=x.getString("date");Instant updated=Instant.parse(x.getString("updatedAt"));
            if(profile.equals(x.getString("profile"))&&date.compareTo(day())<=0&&x.getInt("goal")>0&&(date.compareTo(lastDate)>0||date.equals(lastDate)&&updated.compareTo(last)>0)){lastDate=date;last=updated;goal=x.getInt("goal");}
        }}return goal;
    }
    public static String encode(JSONArray input) throws JSONException {
        JSONArray packed=new JSONArray();for(int i=0;i<input.length();i++){JSONObject x=input.getJSONObject(i);packed.put(new JSONArray().put(x.getString("date")).put(Book.PROFILES.indexOf(x.getString("profile"))).put(x.getInt("start")).put(x.getInt("end")).put(x.getInt("goal")).put(x.getString("updatedAt")));}
        String s=packed.toString();if(s.length()>175000)throw new IllegalArgumentException("Okuma günlüğü Notion alanına sığmıyor; kayıt telefonda korunuyor.");return s;
    }
    public static JSONArray decode(String raw) throws JSONException {
        if(raw.trim().isEmpty())return new JSONArray();JSONArray a=new JSONArray(raw),out=new JSONArray();
        for(int i=0;i<a.length();i++){JSONArray x=a.getJSONArray(i);int p=x.getInt(1);if(p<0||p>=Book.PROFILES.size())throw new IllegalArgumentException("Okuma profili geçersiz.");out.put(new JSONObject().put("date",x.getString(0)).put("profile",Book.PROFILES.get(p)).put("start",x.get(2)).put("end",x.get(3)).put("goal",x.get(4)).put("updatedAt",x.getString(5)));}return normalize(out);
    }
}
