package com.pardal.kitapligim;

import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

/** Catalog metadata is separate from the family's saved books. One unavailable source never hides another's results. */
public final class BookSearch {
    public interface Fetcher { String get(String url) throws Exception; }
    private final Fetcher fetch;
    private final JSONArray catalog;
    public BookSearch(Fetcher fetch, JSONArray catalog){this.fetch=fetch;this.catalog=catalog;}
    public static String fold(String value){return Normalizer.normalize(value.toLowerCase(Locale.forLanguageTag("tr")).replace('ı','i'),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
    public JSONArray search(String input) throws Exception {
        String query=input.trim();if(query.length()<2)throw new IllegalArgumentException("En az 2 karakter yaz.");
        String encoded=URLEncoder.encode(query,"UTF-8");
        ExecutorService pool=Executors.newFixedThreadPool(3);
        List<Callable<JSONArray>> jobs=List.of(
            ()->publisher(fetch.get("https://www.olimposdukkan.com/search?q="+encoded)),
            ()->google(new JSONObject(fetch.get("https://www.googleapis.com/books/v1/volumes?q="+encoded+"&maxResults=16&printType=books&langRestrict=tr"))),
            ()->openLibrary(new JSONObject(fetch.get("https://openlibrary.org/search.json?q="+encoded+"&lang=tr&limit=16&fields=key,title,author_name,cover_i,isbn,number_of_pages_median,editions,editions.title,editions.language,editions.isbn,editions.cover_i,editions.number_of_pages")),query)
        );
        JSONArray combined=new JSONArray();Set<String> seen=new HashSet<>();
        // A small publisher-verified Turkish catalog remains searchable when public services are quota-limited.
        String[] terms=fold(query).split("\\s+");
        for(int i=0;i<catalog.length();i++){
            JSONObject b=catalog.getJSONObject(i);String text=fold(b.optString("title")+" "+b.optString("author")+" "+b.optString("isbn"));boolean match=true;
            for(String term:terms)if(!text.contains(term))match=false;
            if(match)add(combined,seen,b);
        }
        int successful=0;
        try {
            List<Future<JSONArray>> tasks=pool.invokeAll(jobs,14,TimeUnit.SECONDS);
            for(Future<JSONArray> task:tasks)try{JSONArray results=task.get();successful++;for(int i=0;i<results.length();i++)add(combined,seen,results.getJSONObject(i));}catch(Exception ignored){}
        }finally{pool.shutdownNow();}
        if(combined.length()==0&&successful==0)throw new java.io.IOException("Kitap kataloglarına şu an ulaşılamıyor. İnternet bağlantısını kontrol edip yeniden dene; kitabı elle de ekleyebilirsin.");
        List<JSONObject> sorted=new ArrayList<>();for(int i=0;i<combined.length();i++)sorted.add(combined.getJSONObject(i));
        sorted.sort((a,b)->Integer.compare(rank(b,query),rank(a,query)));
        JSONArray result=new JSONArray();for(JSONObject book:sorted)result.put(book);return result;
    }
    private static int rank(JSONObject b,String q){String query=fold(q),title=fold(b.optString("title"));return b.optString("isbn").equals(q)||title.equals(query)?100:title.startsWith(query)?60:title.contains(query)?40:20;}
    private static void add(JSONArray out,Set<String> seen,JSONObject b) throws JSONException {
        if(b.optString("title").trim().isEmpty())return;
        String isbn=b.optString("isbn").replaceAll("[^0-9Xx]","");String key=isbn.isEmpty()?fold(b.optString("title"))+"|"+fold(b.optString("author")):isbn;
        if(seen.add(key))out.put(b);
    }
    public static JSONArray publisher(String html) throws JSONException {
        Document document=Jsoup.parse(html,"https://www.olimposdukkan.com/");JSONArray books=new JSONArray();
        for(Element card:document.select(".Product[data-prd-barcode]")){
            Element link=card.selectFirst(".Product-content .name a"),image=card.selectFirst("img.prd_img");if(link==null)continue;
            String cover=image==null?"":image.hasAttr("data-src")?image.attr("abs:data-src"):image.attr("abs:src");
            books.put(new JSONObject().put("title",link.text()).put("author",card.select(".writer").text()).put("isbn",card.attr("data-prd-barcode")).put("cover",cover).put("totalPages",0).put("source","Olimpos · Türkçe katalog").put("sourceUrl",link.absUrl("href")));
        }
        return books;
    }
    private static String join(JSONArray a) throws JSONException {if(a==null)return "";List<String> values=new ArrayList<>();for(int i=0;i<a.length();i++)values.add(a.getString(i));return String.join(", ",values);}
    public static JSONArray google(JSONObject response) throws JSONException {
        JSONArray results=new JSONArray(),items=response.optJSONArray("items");if(items==null)return results;
        for(int i=0;i<items.length();i++){
            JSONObject v=items.getJSONObject(i).getJSONObject("volumeInfo"),b=new JSONObject().put("title",v.optString("title")).put("author",join(v.optJSONArray("authors"))).put("totalPages",v.optInt("pageCount")).put("isbn","").put("cover","").put("source","Google Books");
            JSONArray ids=v.optJSONArray("industryIdentifiers");if(ids!=null)for(int j=0;j<ids.length();j++){JSONObject x=ids.getJSONObject(j);if("ISBN_13".equals(x.optString("type"))||b.getString("isbn").isEmpty())b.put("isbn",x.optString("identifier"));}
            JSONObject images=v.optJSONObject("imageLinks");if(images!=null)b.put("cover",images.optString("thumbnail").replace("http://","https://"));results.put(b);
        }
        return results;
    }
    public static JSONArray openLibrary(JSONObject response,String query) throws JSONException {
        JSONArray books=new JSONArray(),docs=response.optJSONArray("docs");if(docs==null)return books;
        for(int i=0;i<docs.length();i++){
            JSONObject work=docs.getJSONObject(i),edition=null;JSONObject editions=work.optJSONObject("editions");JSONArray choices=editions==null?null:editions.optJSONArray("docs");
            if(choices!=null)for(int j=0;j<choices.length();j++){JSONObject e=choices.getJSONObject(j);String languages=join(e.optJSONArray("language"));if(languages.contains("tur")||languages.equals("tr")){edition=e;break;}if(fold(e.optString("title")).contains(fold(query)))edition=e;}
            JSONObject selected=edition==null?work:edition;JSONArray isbns=selected.optJSONArray("isbn");String isbn=isbns!=null&&isbns.length()>0?isbns.getString(0):"";
            long cover=selected.optLong("cover_i",work.optLong("cover_i"));
            books.put(new JSONObject().put("title",selected.optString("title",work.optString("title"))).put("author",join(work.optJSONArray("author_name"))).put("isbn",isbn).put("totalPages",selected.optInt("number_of_pages",work.optInt("number_of_pages_median"))).put("cover",cover>0?"https://covers.openlibrary.org/b/id/"+cover+"-M.jpg":"").put("source","Open Library"));
        }
        return books;
    }
}
