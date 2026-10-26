package com.deepseeklife.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;
import java.util.concurrent.TimeUnit;

public class AutoWorker extends Worker {
    public AutoWorker(@NonNull Context ctx,@NonNull WorkerParameters params){super(ctx,params);}
    @NonNull @Override public Result doWork(){
        Context c=getApplicationContext();
        if(!Brain.prefs(c).getBoolean("auto",false))return Result.success();
        try {
            String post=Brain.ask(c,"以自己的口吻创作一条新的生活动态，最多80字。不要声称真实发生过的事情。只输出动态正文。");
            Brain.addPost(c,post);
            String message=Brain.ask(c,"主动向好友发一条轻松的简短聊天消息，可以分享想法或提出一个问题，最多60字。只输出消息。");
            Brain.prefs(c).edit().putString("last_message",message).apply();
            NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new NotificationChannel("life","DeepSeek 主动聊天",NotificationManager.IMPORTANCE_DEFAULT));
            if(android.os.Build.VERSION.SDK_INT<33 || c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED){
                Notification notification=new Notification.Builder(c,"life").setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle(Brain.prefs(c).getString("name","小深")+" 发来消息")
                    .setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                    .setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message)).setAutoCancel(true).build();
                manager.notify(101,notification);
            }
            Brain.prefs(c).edit().remove("last_error").apply();
            return Result.success();
        }catch(Exception e){
            Brain.prefs(c).edit().putString("last_error",String.valueOf(e.getMessage())).apply();
            // Do not repeat a partially completed pair of paid API calls.
            // Periodic work can try again at its next scheduled interval.
            return Result.failure();
        }
    }
}
