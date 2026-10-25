package com.pardal.kitapligim;

import org.json.*;
import java.util.*;

public final class NotionCodec {
    /** Columns added after the first release; a table without them still syncs everything else. */
    public static final List<String> OPTIONAL_COLUMNS = List.of("Fiyat","Mağaza","Satın Alma Linki","Altı Çizilenler",ReadingProgress.COLUMN);
    private static JSONObject value(String k, Object v) throws JSONException { return new JSONObject().put(k, v); }
    private static JSONArray rich(String s) throws JSONException {
        JSONArray r=new JSONArray();
        // Notion limits each text.content element to 2,000 UTF-16 code units.
        for (int i=0;i<s.length();) { int end=Math.min(i+1800,s.length()); if(end<s.length() && Character.isHighSurrogate(s.charAt(end-1)))end--; r.put(value("type","text").put("text",value("content",s.substring(i,end)))); i=end; }
        return r;
    }
    private static String text(JSONObject p, String key, String type) throws JSONException {
        JSONArray a=p.optJSONObject(key)==null?null:p.getJSONObject(key).optJSONArray(type); StringBuilder s=new StringBuilder();
        if (a!=null) for(int i=0;i<a.length();i++) {JSONObject x=a.getJSONObject(i); s.append(x.has("plain_text")?x.optString("plain_text"):x.optJSONObject("text")!=null?x.getJSONObject("text").optString("content"):"");}
        return s.toString();
    }
    private static int number(JSONObject p,String k) throws JSONException {return p.optJSONObject(k)==null?0:p.getJSONObject(k).optInt("number");}
    private static String select(JSONObject p,String k) throws JSONException {JSONObject x=p.optJSONObject(k);if(x==null || x.optJSONObject("select")==null)return "";return x.getJSONObject("select").optString("name");}
    private static String url(JSONObject p,String k) throws JSONException {JSONObject x=p.optJSONObject(k);return x==null||x.isNull("url")?"":x.optString("url");}
    private static String date(JSONObject p,String k) throws JSONException {JSONObject x=p.optJSONObject(k);if(x==null||x.optJSONObject("date")==null)return "";return x.getJSONObject("date").optString("start").split("T")[0];}
    /** Readable in Notion: one passage per block, blank line between, e.g. “…” then "— s. 124" and "Not: …". */
    public static String highlightText(JSONArray list) throws JSONException {
        StringBuilder s=new StringBuilder();if(list==null)return "";
        for(int i=0;i<list.length();i++){JSONObject h=list.getJSONObject(i);if(s.length()>0)s.append("\n\n");s.append('“').append(h.optString("text")).append('”');if(h.optInt("page")>0)s.append("\n— s. ").append(h.optInt("page"));if(!h.optString("note").isEmpty())s.append("\nNot: ").append(h.optString("note"));}
        return s.toString();
    }
    /** Lenient on purpose: a passage typed in Notion without quotes or page is still one passage. */
    public static JSONArray parseHighlights(String raw) throws JSONException {
        JSONArray out=new JSONArray();int n=0;
        for(String block:raw.replace("\r","").split("\n[ \t]*\n")){
            List<String> lines=new ArrayList<>(Arrays.asList(block.trim().split("\n")));String note="";int page=0;
            for(int i=lines.size()-1;i>0;i--){String l=lines.get(i).trim();
                if(l.startsWith("Not:")&&note.isEmpty()&&page==0){note=l.substring(4).trim();lines.remove(i);}
                else if(l.matches("—\\s*s\\.\\s*\\d{1,6}")&&page==0){page=Integer.parseInt(l.replaceAll("\\D",""));lines.remove(i);}
                else break;}
            String text=String.join("\n",lines).trim();
            if(text.startsWith("“")&&text.endsWith("”")&&text.length()>=2)text=text.substring(1,text.length()-1).trim();
            if(text.isEmpty())continue;
            out.put(new JSONObject().put("id","n"+(n++)).put("text",text).put("page",page).put("note",note).put("createdAt",""));
        }
        return out;
    }
    public static JSONObject toProperties(JSONObject b) throws JSONException {
        JSONObject p=new JSONObject();
        p.put("Kitap",value("title",rich(b.getString("title"))));
        p.put("Profil",value("select",value("name",b.getString("profile"))));
        p.put("Durum",value("select",value("name",Book.statusName(b.getString("status")))));
        for(String[] f:new String[][]{{"Yazar","author"},{"ISBN","isbn"},{"Notlar","notes"},{"Uygulama ID","id"}})p.put(f[0],value("rich_text",rich(b.optString(f[1]))));
        for(String[] f:new String[][]{{"Sayfa Sayısı","totalPages"},{"Kaldığım Sayfa","currentPage"},{"Puan","rating"}})p.put(f[0],value("number",b.optInt(f[1])));
        for(String[] f:new String[][]{{"Başlama","startedAt"},{"Bitiş","finishedAt"}})p.put(f[0],value("date",b.optString(f[1]).isEmpty()?JSONObject.NULL:value("start",b.getString(f[1]))));
        p.put("Kapak",value("url",b.optString("cover").isEmpty()?JSONObject.NULL:b.getString("cover")));
        p.put("Silindi",value("checkbox",b.optBoolean("deleted")));
        if(b.optString("photoKey").isEmpty())p.put("Kapak Fotoğrafı",value("files",new JSONArray()));
        p.put("Fiyat",value("rich_text",rich(b.optString("price"))));
        p.put("Mağaza",value("rich_text",rich(b.optString("store"))));
        p.put("Satın Alma Linki",value("url",b.optString("buyUrl").isEmpty()?JSONObject.NULL:b.getString("buyUrl")));
        p.put("Altı Çizilenler",value("rich_text",rich(highlightText(b.optJSONArray("highlights")))));
        if(b.has("readingDays"))p.put(ReadingProgress.COLUMN,value("rich_text",rich(ReadingProgress.encode(b.getJSONArray("readingDays")))));
        return p;
    }
    public static JSONObject fromPage(JSONObject page) throws JSONException {
        JSONObject p=page.getJSONObject("properties"); String profile=select(p,"Profil");
        if(!Book.PROFILES.contains(profile)) throw new IllegalArgumentException("Notion satırında Profil seçilmeli: "+text(p,"Kitap","title"));
        String id=text(p,"Uygulama ID","rich_text"); if(id.isEmpty()) id="notion-"+page.getString("id");
        JSONObject b=new JSONObject().put("id",id).put("title",text(p,"Kitap","title")).put("profile",profile).put("status",Book.statusId(select(p,"Durum")));
        for(String[] f:new String[][]{{"Yazar","author"},{"ISBN","isbn"},{"Notlar","notes"}})b.put(f[1],text(p,f[0],"rich_text"));
        b.put("totalPages",number(p,"Sayfa Sayısı")).put("currentPage",number(p,"Kaldığım Sayfa")).put("rating",number(p,"Puan"));
        b.put("cover",url(p,"Kapak"));
        b.put("startedAt",date(p,"Başlama")).put("finishedAt",date(p,"Bitiş"));
        // Purchase columns are optional in the table; a missing column leaves the key out so local values survive.
        if(p.has("Fiyat"))b.put("price",text(p,"Fiyat","rich_text"));
        if(p.has("Mağaza"))b.put("store",text(p,"Mağaza","rich_text"));
        if(p.has("Satın Alma Linki"))b.put("buyUrl",url(p,"Satın Alma Linki"));
        if(p.has("Altı Çizilenler"))b.put("highlights",parseHighlights(text(p,"Altı Çizilenler","rich_text")));
        if(p.has(ReadingProgress.COLUMN))b.put("readingDays",ReadingProgress.decode(text(p,ReadingProgress.COLUMN,"rich_text")));
        boolean deleted=page.optBoolean("archived")||page.optBoolean("is_archived")||page.optBoolean("in_trash")||(p.optJSONObject("Silindi")!=null && p.getJSONObject("Silindi").optBoolean("checkbox"));
        b.put("deleted",deleted).put("remotePageId",page.getString("id")).put("remoteEditedAt",page.getString("last_edited_time")).put("updatedAt",page.getString("last_edited_time")).put("syncStatus","synced");
        JSONArray photos=p.optJSONObject("Kapak Fotoğrafı")==null?null:p.getJSONObject("Kapak Fotoğrafı").optJSONArray("files");
        b.put("photoKey", "");
        if(photos!=null && photos.length()>0){JSONObject f=photos.getJSONObject(0); b.put("photoKey",f.optString("name")); JSONObject file=f.optJSONObject("file"); if(file!=null)b.put("remotePhotoUrl",file.optString("url"));}
        return Book.normalize(b);
    }
}
