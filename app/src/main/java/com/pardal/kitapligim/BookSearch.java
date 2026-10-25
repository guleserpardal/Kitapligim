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
        String query=queryText(input);if(query.length()<2)throw new IllegalArgumentException("En az 2 karakter yaz.");
        String encoded=URLEncoder.encode(query,"UTF-8");
        ExecutorService pool=Executors.newFixedThreadPool(4);
        List<Callable<JSONArray>> jobs=List.of(
            ()->publisher(fetch.get("https://www.olimposdukkan.com/search?q="+encoded)),
            ()->googleSearch(encoded),
            ()->trendyol(fetch.get("https://www.trendyol.com/sr?wc=91&q="+encoded),query),
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
    /** A pasted barcode often contains spaces or hyphens. Keep title punctuation otherwise. */
    static String queryText(String input){String q=input.trim(),digits=q.replaceFirst("(?i)^isbn(?:-1[03])?\\s*:?\\s*","").replaceAll("[\\s-]","");return digits.matches("(?:[0-9]{13}|[0-9]{9}[0-9Xx])")?digits:q;}
    private JSONArray googleSearch(String encoded) throws Exception {
        String url="https://www.googleapis.com/books/v1/volumes?q="+encoded+"&maxResults=16&printType=books";
        JSONArray books=google(new JSONObject(fetch.get(url+"&langRestrict=tr")));
        // Some Turkish editions have missing/wrong language metadata. Retry empty results, never quota errors.
        return books.length()>0?books:google(new JSONObject(fetch.get(url)));
    }
    /** Public book-category search cards, not the marketplace seller API. Never infer author/ISBN/pages from a brand or range. */
    private static boolean bundle(String text){return fold(text).matches(".*\\b(?:set|seti|setli|kitaplik|kutulu|[0-9]+\\s*(?:li|lu|kitap))\\b.*");}
    public static JSONArray trendyol(String html,String query) throws JSONException {
        Document document=Jsoup.parse(html,"https://www.trendyol.com/");JSONArray books=new JSONArray();Set<String> seen=new HashSet<>();
        for(Element card:document.select("a[data-testid=product-card]")){
            Element name=card.selectFirst(".product-name"),image=card.selectFirst("img[data-testid=image-img]");if(name==null)continue;
            String title=name.text(),url=card.absUrl("href"),text=fold(title+" "+card.select(".product-brand").text());
            if(bundle(title)&&!bundle(query))continue;
            if(!url.matches("https://www\\.trendyol\\.com/[^?#]*-p-[0-9]+(?:[?#].*)?")||!seen.add(fold(title)))continue;
            boolean match=queryText(query).matches("[0-9Xx]{10,13}");
            if(!match){match=true;for(String term:fold(query).split("\\s+"))if(!text.contains(term)){match=false;break;}}
            if(!match)continue;
            String cover=image==null?"":image.absUrl("src");if(!cover.startsWith("https://"))cover="";
            books.put(new JSONObject().put("title",title).put("author","").put("isbn","").put("totalPages",0).put("cover",cover).put("source","Trendyol · kitap ilanı").put("sourceUrl",url).put("buyUrl",url).put("store","Trendyol"));
            if(books.length()>=16)break;
        }
        return books;
    }
    private static int rank(JSONObject b,String q){String query=fold(q),title=fold(b.optString("title"));return b.optString("isbn").equals(q)||title.equals(query)?100:title.startsWith(query)?60:title.contains(query)?40:20;}
    private static void add(JSONArray out,Set<String> seen,JSONObject b) throws JSONException {
        if(b.optString("title").trim().isEmpty())return;
        if(b.optString("cover").startsWith("/")&&b.optString("sourceUrl").startsWith("https://"))try{b=new JSONObject(b.toString()).put("cover",java.net.URI.create(b.optString("sourceUrl")).resolve(b.optString("cover")).toString());}catch(Exception ignored){}
        String isbn=b.optString("isbn").replaceAll("[^0-9Xx]","");String key=isbn.isEmpty()?fold(b.optString("title"))+"|"+fold(b.optString("author")):isbn;
        if(seen.add(key))out.put(b);
    }
    public static JSONArray publisher(String html) throws JSONException {
        Document document=Jsoup.parse(html,"https://www.olimposdukkan.com/");JSONArray books=new JSONArray();
        for(Element card:document.select(".Product[data-prd-barcode]")){
            Element link=card.selectFirst(".Product-content .name a"),image=card.selectFirst("img.prd_img");if(link==null)continue;
            String cover=image==null?"":image.hasAttr("data-src")?image.attr("abs:data-src"):image.attr("abs:src");
            JSONObject book=new JSONObject().put("title",link.text()).put("author",card.select(".writer").text()).put("isbn",card.attr("data-prd-barcode")).put("cover",cover).put("totalPages",0).put("source","Olimpos · Türkçe katalog").put("sourceUrl",link.absUrl("href"));
            Element price=card.selectFirst(".price_sale[data-price]");if(price==null)price=card.selectFirst(".price[data-price]");
            String amount=price==null?"":lira(price.attr("data-price"));
            if(!amount.isEmpty()&&link.absUrl("href").startsWith("https://"))book.put("price",amount).put("store","Olimpos").put("buyUrl",link.absUrl("href"));
            books.put(book);
        }
        return books;
    }
    /** "231.00" → "231,00 TL"; anything unparseable yields an empty price rather than a wrong one. */
    public static String lira(String amount){try{double v=Double.parseDouble(amount.trim());return v>0?String.format(Locale.forLanguageTag("tr"),"%,.2f TL",v):"";}catch(Exception e){return "";}}
    /** Current price and purchase link for one edition. D&R is looked up by ISBN; without a match the link opens a D&R search. */
    public JSONObject offer(String isbn,String title,String author) throws Exception {
        String clean=isbn.replaceAll("[^0-9Xx]","");
        if(clean.length()==10||clean.length()==13){
            try{JSONObject found=dr(fetch.get("https://www.dr.com.tr/search?q="+clean),clean);if(found!=null)return found;}catch(Exception ignored){}
        }
        String q=(clean.isEmpty()?(title+" "+author):clean).trim();
        if(q.isEmpty())return new JSONObject();
        return new JSONObject().put("price","").put("store","D&R araması").put("buyUrl","https://www.dr.com.tr/search?q="+URLEncoder.encode(q,"UTF-8")).put("searchOnly",true);
    }
    public static JSONObject dr(String html,String isbn) throws JSONException {
        Document document=Jsoup.parse(html,"https://www.dr.com.tr/");
        Element canonical=document.selectFirst("link[rel=canonical]");String url=canonical==null?"":canonical.absUrl("href");
        for(Element script:document.select("script[type=application/ld+json]")){
            // D&R leaves raw line breaks inside JSON strings; as whitespace they are harmless.
            Object parsed;try{parsed=new JSONTokener(script.data().replaceAll("\\p{Cntrl}"," ").trim()).nextValue();}catch(Exception e){continue;}
            JSONArray list=parsed instanceof JSONArray?(JSONArray)parsed:new JSONArray().put(parsed);
            for(int i=0;i<list.length();i++){
                JSONObject item=list.optJSONObject(i);if(item==null||!item.optString("@type").contains("Product"))continue;
                if(!isbn.equals(item.optString("gtin13"))&&!isbn.equals(item.optString("isbn")))continue;
                Object offers=item.opt("offers");JSONObject offer=offers instanceof JSONArray?((JSONArray)offers).optJSONObject(0):offers instanceof JSONObject?(JSONObject)offers:null;
                if(offer==null)continue;
                String price=lira(offer.optString("price")),link=url.isEmpty()?offer.optString("url"):url;
                if(price.isEmpty()||!link.startsWith("https://"))continue;
                boolean inStock=!offer.optString("availability").contains("OutOfStock");
                return new JSONObject().put("price",price).put("store",inStock?"D&R":"D&R · stokta yok").put("buyUrl",link);
            }
        }
        return null;
    }
    private static String join(JSONArray a) throws JSONException {if(a==null)return "";List<String> values=new ArrayList<>();for(int i=0;i<a.length();i++)values.add(a.getString(i));return String.join(", ",values);}
    public static JSONArray google(JSONObject response) throws JSONException {
        JSONArray results=new JSONArray(),items=response.optJSONArray("items");if(items==null)return results;
        for(int i=0;i<items.length();i++){
            JSONObject v=items.getJSONObject(i).getJSONObject("volumeInfo"),b=new JSONObject().put("title",v.optString("title")).put("author",join(v.optJSONArray("authors"))).put("totalPages",v.optInt("pageCount")).put("isbn","").put("cover","").put("source","Google Books");
            JSONArray ids=v.optJSONArray("industryIdentifiers");if(ids!=null)for(int j=0;j<ids.length();j++){JSONObject x=ids.getJSONObject(j);if("ISBN_13".equals(x.optString("type"))||b.getString("isbn").isEmpty())b.put("isbn",x.optString("identifier"));}
            JSONObject images=v.optJSONObject("imageLinks");if(images!=null)b.put("cover",images.optString("thumbnail").replace("http://","https://"));
            JSONObject sale=items.getJSONObject(i).optJSONObject("saleInfo"),retail=sale==null?null:sale.optJSONObject("retailPrice");
            if(retail!=null&&"FOR_SALE".equals(sale.optString("saleability"))&&sale.optString("buyLink").startsWith("https://")){String code=retail.optString("currencyCode");String amount=lira(retail.optString("amount"));if(!amount.isEmpty())b.put("price","TRY".equals(code)?amount:amount.replace(" TL"," "+code)).put("store","Google Play · e-kitap").put("buyUrl",sale.getString("buyLink"));}
            results.put(b);
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
