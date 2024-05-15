package com.yuraivanov.screentranslator;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class NotificationButton extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 101;
    @Override
    public void onReceive(Context context, Intent intent){
        if(intent.getAction().equals("ACTION_CANSEL")){
            MainActivity.stopSharing();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}