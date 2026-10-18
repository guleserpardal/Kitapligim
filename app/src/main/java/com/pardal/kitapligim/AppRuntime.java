package com.pardal.kitapligim;

import android.app.job.*;
import android.content.*;
import java.io.File;
import java.util.concurrent.*;

public final class AppRuntime {
    public static final ScheduledExecutorService EXEC=Executors.newSingleThreadScheduledExecutor();
    private static BookStore store;
    public static synchronized BookStore store(Context c) throws Exception {if(store==null)store=new BookStore(c.getApplicationContext());return store;}
    public static File covers(Context c){File f=new File(c.getFilesDir(),"covers");f.mkdirs();return f;}
    public static void sync(Context c) throws Exception {BookStore s=store(c);try{new NotionSync(s,new Credentials(c).get(),covers(c)).run();}catch(Exception e){s.syncResult(e.getMessage()==null?"İnternet bağlantısını kontrol edip yeniden dene.":e.getMessage());throw e;}}
    public static void schedule(Context c){JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);scheduler.schedule(new JobInfo.Builder(103,new ComponentName(c,SyncJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(15*60*1000L).build());}
}
