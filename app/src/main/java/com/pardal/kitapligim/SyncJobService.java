package com.pardal.kitapligim;
import android.app.job.*;
public final class SyncJobService extends JobService {
    private volatile boolean stopped;
    @Override public boolean onStartJob(JobParameters p){stopped=false;AppRuntime.EXEC.execute(()->{try{AppRuntime.sync(this);}catch(Exception ignored){}if(!stopped)jobFinished(p,false);});return true;}
    @Override public boolean onStopJob(JobParameters p){stopped=true;return true;}
}
