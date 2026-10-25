package com.pardal.kitapligim;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.android.gms.tasks.Tasks;
import org.json.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private WebView web;
    private String photoRequest;
    private boolean photoTextOnly;
    private Uri cameraUri;
    private boolean ready;
    private ScheduledFuture<?> pendingSync;
    private StoreScraper scraper;
    private static final String ORIGIN="https://appassets.androidplatform.net";
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);
        getWindow().setDecorFitsSystemWindows(false);
        android.widget.FrameLayout content=new android.widget.FrameLayout(this);
        content.setBackgroundColor(Color.rgb(248,244,236));
        web=new WebView(this);web.setBackgroundColor(Color.rgb(248,244,236));
        content.addView(web,new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(content);
        // Apply insets outside WebView: padding WebView itself does not constrain CSS position:fixed.
        content.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            return WindowInsets.CONSUMED;
        });
        content.requestApplyInsets();
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setBuiltInZoomControls(false);s.setTextZoom(100);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).addPathHandler("/covers/",new WebViewAssetLoader.InternalStoragePathHandler(this,AppRuntime.covers(this))).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){WebResourceResponse local=loader.shouldInterceptRequest(request.getUrl());if(local!=null)return local;String host=request.getUrl().getHost();if(!request.isForMainFrame()&&"https".equals(request.getUrl().getScheme())&&!ORIGIN.equals(request.getUrl().getScheme()+"://"+host))return null;return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Map.of(),new ByteArrayInputStream(new byte[0]));}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r){return true;}
        });
        web.addJavascriptInterface(new Bridge(),"Android");web.loadUrl(ORIGIN+"/assets/index.html");AppRuntime.schedule(this);
    }
    @Override public void onResume(){super.onResume();if(ready)queueSync();}
    @Override public void onBackPressed(){web.evaluateJavascript("window.onNativeBack&&window.onNativeBack()",value->{if(!"true".equals(value))super.onBackPressed();});}
    private void reply(String id,Object value,String error){if(isFinishing()||isDestroyed())return;runOnUiThread(()->web.evaluateJavascript("window.nativeResult("+JSONObject.quote(id)+","+(value==null?"null":value.toString())+","+(error==null?"null":JSONObject.quote(error))+")",null));}
    private JSONObject state() throws Exception {JSONObject d=AppRuntime.store(this).state();d.put("connected",!new Credentials(this).get().isEmpty());JSONObject as;try{as=assistant().summary();}catch(Exception e){as=new JSONObject().put("configured",false);}d.put("assistant",as);d.put("platform","android");return d;}
    private Assistant assistant(){return new Assistant(new Credentials(this,"assistant","kitapligim-assistant"));}
    private void emitState(){if(!ready||isFinishing()||isDestroyed())return;try{String js=state().toString();runOnUiThread(()->web.evaluateJavascript("window.receiveState("+js+")",null));}catch(Exception ignored){}}
    private void queueSync(){if(pendingSync!=null)pendingSync.cancel(false);pendingSync=AppRuntime.EXEC.schedule(()->{try{AppRuntime.sync(this);}catch(Exception ignored){}emitState();},2,TimeUnit.SECONDS);}
    final class Bridge {
        @JavascriptInterface public void request(String id,String action,String payload){
            if(action.equals("photo")){runOnUiThread(()->selectPhoto(id,payload));return;}
            // Price lookups use their own threads so slow shops never delay saving or syncing.
            if(action.equals("shopPrices")){runOnUiThread(()->{try{if(scraper==null)scraper=new StoreScraper(MainActivity.this);JSONObject p=new JSONObject(payload);String isbn=p.optString("isbn").replaceAll("[^0-9Xx]","");String q=isbn.length()==10||isbn.length()==13?isbn:(p.optString("title")+" "+p.optString("author")).trim();if(q.isEmpty()){reply(id,new JSONArray(),null);return;}scraper.prices(q,offers->reply(id,offers,null));}catch(Exception e){reply(id,null,"Mağaza fiyatları alınamadı.");}});return;}
            if(action.equals("offers")){OFFERS.execute(()->{try{reply(id,offers(new JSONObject(payload).getJSONArray("books")),null);}catch(Exception e){reply(id,null,"Fiyat bilgisi alınamadı.");}});return;}
            AppRuntime.EXEC.execute(()->{try{JSONObject p=new JSONObject(payload);Object result;
                switch(action){
                    case "state":ready=true;result=state();queueSync();break;
                    case "save":AppRuntime.store(MainActivity.this).save(p);result=state();queueSync();break;
                    case "delete":AppRuntime.store(MainActivity.this).delete(p.getString("id"));result=state();queueSync();break;
                    case "settings":AppRuntime.store(MainActivity.this).settings(p);result=state();if(p.has("dailyGoal"))queueSync();break;
                    case "resolve":{BookStore st=AppRuntime.store(MainActivity.this);JSONObject local=st.book(p.getString("id"));if(local==null)throw new Exception("Kitap bulunamadı.");st.put(SyncMerge.resolve(local,p.getBoolean("remote")));result=state();queueSync();break;}
                    case "search":result=search(p.getString("q"));break;
                    case "connect":{String token=p.getString("token").trim();if(token.isEmpty())throw new Exception("Notion bağlantı anahtarını gir.");new NotionSync(AppRuntime.store(MainActivity.this),token,AppRuntime.covers(MainActivity.this)).check();new Credentials(MainActivity.this).save(token);result=state();queueSync();break;}
                    case "assistantSave":assistant().save(p.getString("url"),p.optString("altUrl"),p.optString("token"));result=state();break;
                    case "assistantTest":result=assistant().test();break;
                    case "assistantClear":assistant().clear();result=state();break;
                    case "chat":result=assistant().chat(p);break;
                    case "disconnect":new Credentials(MainActivity.this).clear();result=state();break;
                    case "sync":AppRuntime.sync(MainActivity.this);result=state();break;
                    case "export":{JSONObject data=AppRuntime.store(MainActivity.this).state();File dir=new File(getCacheDir(),"exports");dir.mkdirs();File f=new File(dir,"Kitapligim-"+java.time.LocalDate.now()+".json");try(FileWriter out=new FileWriter(f)){out.write(data.toString(2));}Uri uri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".files",f);runOnUiThread(()->{Intent share=new Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(share,"Kitaplık bilgilerini dışa aktar"));});result=new JSONObject().put("ok",true);break;}
                    case "openLink":{String url=p.getString("url");if(!url.startsWith("https://"))throw new Exception("Bağlantı açılamadı.");runOnUiThread(()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));result=new JSONObject().put("ok",true);break;}
                    case "open":{String url=p.getString("url");if(!url.startsWith("https://www.notion.so/")&&!url.startsWith("https://app.notion.com/")&&!url.startsWith("https://www.notion.com/"))throw new Exception("Bağlantı açılamadı.");runOnUiThread(()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));result=new JSONObject().put("ok",true);break;}
                    default:throw new Exception("İşlem tanınmadı.");
                }
                reply(id,result,null);
            }catch(Exception e){String msg=e.getMessage();if(e instanceof java.net.UnknownHostException||e instanceof java.net.SocketTimeoutException)msg="İnternet bağlantısını kontrol edip yeniden dene. Kayıtların telefonda duruyor.";reply(id,null,msg==null?"İşlem tamamlanamadı.":msg);}});
        }
    }
    private JSONArray search(String query) throws Exception {
        JSONArray catalog;
        try(InputStream in=getAssets().open("catalog-tr.json")){catalog=new JSONArray(new String(Http.readLimited(in,200000),StandardCharsets.UTF_8));}
        return new BookSearch(url->Http.getText(url),catalog).search(query);
    }
    private static final ExecutorService OFFERS=Executors.newFixedThreadPool(4);
    private JSONArray offers(JSONArray books) throws Exception {
        BookSearch search=new BookSearch(url->Http.getText(url),new JSONArray());List<Callable<JSONObject>> jobs=new ArrayList<>();
        for(int i=0;i<Math.min(books.length(),12);i++){JSONObject b=books.getJSONObject(i);jobs.add(()->search.offer(b.optString("isbn"),b.optString("title"),b.optString("author")));}
        ExecutorService pool=Executors.newFixedThreadPool(4);JSONArray out=new JSONArray();
        try{for(Future<JSONObject> f:pool.invokeAll(jobs,12,TimeUnit.SECONDS)){JSONObject offer;try{offer=f.get();}catch(Exception e){offer=new JSONObject();}out.put(offer);}}finally{pool.shutdownNow();}
        return out;
    }
    private void selectPhoto(String id,String payload){
        try{if(photoRequest!=null)throw new Exception("Bir fotoğraf işlemi zaten açık.");photoRequest=id;JSONObject photoOptions=new JSONObject(payload);String source=photoOptions.optString("source");photoTextOnly=photoOptions.optBoolean("textOnly");
            if(source.equals("camera")){File dir=new File(getCacheDir(),"camera");dir.mkdirs();File photo=File.createTempFile("capture-",".jpg",dir);cameraUri=FileProvider.getUriForFile(this,getPackageName()+".files",photo);Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,cameraUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(intent,41);}
            else {Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,42);}
        }catch(Exception e){photoRequest=null;reply(id,null,"Fotoğraf açılamadı. Kamera veya galeriyi tekrar dene.");}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=41&&req!=42)return;String id=photoRequest;photoRequest=null;if(id==null)return;boolean textOnly=photoTextOnly;photoTextOnly=false;if(result!=RESULT_OK){reply(id,new JSONObject(),null);return;}Uri uri=req==41?cameraUri:data==null?null:data.getData();if(uri==null){reply(id,null,"Fotoğraf alınamadı.");return;}
        AppRuntime.EXEC.execute(()->{
            TextRecognizer text=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);BarcodeScanner barcode=BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_CODE_128).build());
            try{
                // Bound memory before recognition. ImageDecoder applies EXIF orientation on modern Android.
                Bitmap bitmap;
                if(Build.VERSION.SDK_INT>=28){bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double scale=Math.min(1.0,1800.0/Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});}
                else {BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,opt);}opt.inSampleSize=Math.max(1,Math.max(opt.outWidth,opt.outHeight)/1600);opt.inJustDecodeBounds=false;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,opt);}if(bitmap==null)throw new IOException("Fotoğraf okunamadı.");}
                File f=new File(AppRuntime.covers(this),"cover-"+UUID.randomUUID()+".jpg");try(FileOutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.JPEG,86,out);}
                InputImage image=InputImage.fromBitmap(bitmap,0);Text recognized=Tasks.await(text.process(image),30,TimeUnit.SECONDS);List<Barcode> codes=Tasks.await(barcode.process(image),15,TimeUnit.SECONDS);String isbn="";for(Barcode code:codes){String raw=code.getRawValue();if(raw!=null&&(raw.startsWith("978")||raw.startsWith("979"))&&raw.matches("\\d{13}")){isbn=raw;break;}}
                List<Text.Line> lines=new ArrayList<>();for(Text.TextBlock block:recognized.getTextBlocks())lines.addAll(block.getLines());lines.sort((a,b)->Integer.compare(b.getBoundingBox()==null?0:b.getBoundingBox().height(),a.getBoundingBox()==null?0:a.getBoundingBox().height()));JSONArray suggestions=new JSONArray();for(Text.Line line:lines){String s=line.getText().trim();if(s.length()>1&&suggestions.length()<14)suggestions.put(s);}
                bitmap.recycle();
                // A page photographed for an underlined passage is only read, never kept.
                if(textOnly){f.delete();JSONArray lines2=new JSONArray();for(Text.TextBlock block:recognized.getTextBlocks())for(Text.Line line:block.getLines())lines2.put(line.getText().trim());reply(id,new JSONObject().put("text",recognized.getText()).put("lines",lines2),null);return;}
                reply(id,new JSONObject().put("localCover",f.getName()).put("photoKey",f.getName()).put("text",recognized.getText()).put("suggestions",suggestions).put("isbn",isbn),null);
            }catch(Exception e){reply(id,null,"Fotoğraftaki yazı okunamadı. Daha net bir fotoğraf dene veya kitabı elle ekle.");}finally{text.close();barcode.close();}
        });
    }
}
