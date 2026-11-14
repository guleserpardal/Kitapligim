package com.pardal.kitapligim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.time.*;

/** Device-local notifications; works without a PC, an account or an internet connection. */
public final class ReadingReminder extends BroadcastReceiver {
    public static final String CHANNEL="reading_reminder";
    private static final String ACTION="com.pardal.kitapligim.READING_REMINDER";
    private static final int REQUEST=427,NOTICE=427;
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("reading-reminder",Context.MODE_PRIVATE);}
    private static NotificationManager manager(Context c){return c.getSystemService(NotificationManager.class);}
    private static void channel(Context c){
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Okuma hatırlatması",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Günlük okumanı kaydetmen için seçtiğin saatte hatırlatma.");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);manager(c).createNotificationChannel(channel);
    }
    public static boolean permission(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    public static boolean allowed(Context c){channel(c);return permission(c)&&manager(c).areNotificationsEnabled()&&manager(c).getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    public static JSONObject state(Context c)throws JSONException{SharedPreferences p=prefs(c);return new JSONObject().put("enabled",p.getBoolean("enabled",false)).put("hour",p.getInt("hour",23)).put("minute",p.getInt("minute",0)).put("allowed",allowed(c));}
    public static void save(Context c,JSONObject value)throws Exception{
        double hour=value.getDouble("hour"),minute=value.getDouble("minute");
        if(!Double.isFinite(hour)||!Double.isFinite(minute)||hour!=Math.floor(hour)||minute!=Math.floor(minute)||hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("Geçerli bir hatırlatma saati seç.");
        prefs(c).edit().putBoolean("enabled",value.getBoolean("enabled")).putInt("hour",(int)hour).putInt("minute",(int)minute).apply();
        schedule(c);
    }
    private static PendingIntent alarm(Context c,long next){return PendingIntent.getBroadcast(c,REQUEST,new Intent(c,ReadingReminder.class).setAction(ACTION).putExtra("scheduledFor",next),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void schedule(Context c){
        SharedPreferences p=prefs(c);AlarmManager alarms=c.getSystemService(AlarmManager.class);
        PendingIntent pending=alarm(c,0);alarms.cancel(pending);
        if(!p.getBoolean("enabled",false)){manager(c).cancel(NOTICE);return;}
        if(!allowed(c))return;
        long next=ReminderTime.next(Instant.now(),ZoneId.systemDefault(),p.getInt("hour",23),p.getInt("minute",0));
        // An approximate evening reminder needs no special exact-alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,alarm(c,next));
    }
    @SuppressLint("MissingPermission")
    public static void notify(Context c,boolean test){
        if(!allowed(c))throw new IllegalStateException("Bildirim izni kapalı. Telefonun bildirim ayarlarından Kitaplığım’a izin ver.");
        Intent open=new Intent(c,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,REQUEST,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(test?"Hatırlatman hazır":"Bugünkü okumanı kaydet 📚")
            .setContentText(test?"Kitaplığım sana seçtiğin saatte böyle hatırlatacak.":"Okuduğun sayfaları ekle, okuma zincirine bir halka daha kat.")
            .setContentIntent(tap).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER).build();
        manager(c).notify(test?NOTICE+1:NOTICE,notification);
    }
    @Override public void onReceive(Context context,Intent intent){
        if(!ACTION.equals(intent.getAction())){schedule(context);return;}
        long planned=intent.getLongExtra("scheduledFor",0);SharedPreferences p=prefs(context);
        if(p.getBoolean("enabled",false)&&planned>0&&ReminderTime.timely(Instant.ofEpochMilli(planned),Instant.now(),ZoneId.systemDefault())&&allowed(context)){
            String day=LocalDate.now().toString();
            if(!day.equals(p.getString("lastSentDay",""))){notify(context,false);p.edit().putString("lastSentDay",day).apply();}
        }
        schedule(context);
    }
}
