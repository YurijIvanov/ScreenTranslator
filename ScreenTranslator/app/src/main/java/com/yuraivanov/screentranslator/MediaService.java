package com.yuraivanov.screentranslator;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MediaService extends Service {
    private static final int NOTIFICATION_ID = 101;
    private static Intent service, mainIntent;
    public static boolean isRecording=true;
    private static final String CHANNEL_ID = "ScreeTranslator", TAG="MediaService";
    public MediaService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            if(service==null) {
                service = intent;
            }
        }else{
            stopSelf();
        }
        if(!isRecording){
            stopSelf();
        }
        return START_STICKY;
    }

    public static void setMainIntent(Intent mainIntent) {
        MediaService.mainIntent = mainIntent;
    }
    public static void stopRecording() {
        Log.e(TAG,"stopRecording");
        isRecording=false;
        service=null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"onCreate");
        isRecording=true;
        if(Build.VERSION.SDK_INT>=34){
            startForeground(NOTIFICATION_ID,buildNotification(),FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }else startForeground(NOTIFICATION_ID, buildNotification());
    }
    private Notification buildNotification(){
        createNotificationChannel();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent,PendingIntent.FLAG_IMMUTABLE);
        Intent notificationButton = new Intent(this,NotificationButton.class);
        notificationButton.setAction("ACTION_CANSEL");
        PendingIntent canselButton = PendingIntent.getBroadcast(this,0,notificationButton,PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Recording in progress")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .addAction(0,"Cansel",canselButton)
                .build();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Screen Recording Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        stopRecording();
    }
}