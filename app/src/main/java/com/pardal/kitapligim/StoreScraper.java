package com.pardal.kitapligim;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import org.json.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * Reads prices from shops whose pages only render in a real browser (Trendyol, Hepsiburada).
 * One hidden WebView loads each shop's own search page, the way the person's browser would, and reads the
 * first product card. No CAPTCHA or block is worked around: a shop that refuses the page simply yields no price.
 */
public final class StoreScraper {
    public interface Done { void result(JSONArray offers); }
    private static final class Shop {
        final String name, search, extract;
        Shop(String name,String search,String extract){this.name=name;this.search=search;this.extract=extract;}
    }
    // Shared reader: the first product link, then a price inside its card (specific selector first, then any "… TL").
    private static final String READ="(function(link,priceSel){var a=document.querySelector(link);if(!a)return null;"
        +"var re=/(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)\\s?TL/,card=a,price=null;"
        +"for(var i=0;i<9&&card;i++){var el=priceSel&&card.querySelector(priceSel);var t=el?el.textContent:card.textContent;var m=t&&t.match(re);if(m&&(el||i>0)){price=m[1];break;}card=card.parentElement;}"
        +"return price?JSON.stringify({price:price,url:a.href}):null;})";
    private static final List<Shop> SHOPS=List.of(
        new Shop("Trendyol","https://www.trendyol.com/sr?q=",READ+"('a[data-testid=product-card]','[data-testid=price-section]')"),
        new Shop("Hepsiburada","https://www.hepsiburada.com/ara?q=",READ+"('li[class^=productListContent] a[href]','[data-test-id^=final-price]')")
        // İdefix's firewall rejects any direct load of its search page, so it only gets a search button in the app.
    );
    private static final long PAGE_TIMEOUT=14000,POLL=700;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Context context;
    private WebView web;
    private final Deque<Runnable> queue=new ArrayDeque<>();
    private boolean busy;
    public StoreScraper(Context context){this.context=context.getApplicationContext();}

    /** Query is an ISBN when known, otherwise title and author. Results arrive on the UI thread. */
    public void prices(String query,Done done){ui.post(()->{queue.add(()->runAll(query,done));if(!busy)next();});}
    private void next(){Runnable r=queue.poll();busy=r!=null;if(r!=null)r.run();}

    private void runAll(String query,Done done){
        JSONArray out=new JSONArray();Iterator<Shop> it=SHOPS.iterator();
        Runnable[] step=new Runnable[1];
        step[0]=()->{
            if(!it.hasNext()){done.result(out);next();return;}
            Shop shop=it.next();
            String url;try{url=shop.search+URLEncoder.encode(query,"UTF-8");}catch(Exception e){step[0].run();return;}
            load(url,shop.extract,found->{
                try{if(found!=null){String price=BookSearch.lira(found.getString("price").replace(".","").replace(',','.'));String link=found.optString("url");if(!price.isEmpty()&&link.startsWith("https://"))out.put(new JSONObject().put("store",shop.name).put("price",price).put("buyUrl",link));}}catch(Exception ignored){}
                step[0].run();
            });
        };
        step[0].run();
    }

    private interface Found { void found(JSONObject result); }
    @SuppressLint("SetJavaScriptEnabled")
    private WebView view(){
        if(web!=null)return web;
        web=new WebView(context);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setBlockNetworkImage(true);
        s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        return web;
    }
    /** Loads a page and polls the script until the product list has rendered or the page times out. */
    private void load(String url,String script,Found found){
        WebView w=view();long started=System.currentTimeMillis();boolean[] finished={false};
        Runnable[] poll=new Runnable[1];
        Runnable finish=()->{if(finished[0])return;finished[0]=true;ui.removeCallbacks(poll[0]);w.stopLoading();found.found(null);};
        poll[0]=()->{
            if(finished[0])return;
            if(System.currentTimeMillis()-started>PAGE_TIMEOUT){finish.run();return;}
            w.evaluateJavascript(script,value->{
                if(finished[0])return;
                JSONObject result=null;
                try{if(value!=null&&!value.equals("null")){Object v=new JSONTokener(value).nextValue();if(v instanceof String)result=new JSONObject((String)v);}}catch(Exception ignored){}
                if(result!=null){finished[0]=true;ui.removeCallbacks(poll[0]);found.found(result);}else ui.postDelayed(poll[0],POLL);
            });
        };
        w.setWebViewClient(new WebViewClient(){
            // Only https pages of the shop itself; app links (intent://, market://) are never opened.
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"https".equals(r.getUrl().getScheme());}
        });
        w.loadUrl(url);
        ui.postDelayed(poll[0],1200);
    }
}
