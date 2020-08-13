package com.jsrd.talk.notification;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ProgressSuccessCallBack;
import com.jsrd.talk.notification.MyNotificationManager;

import java.util.List;

import static com.jsrd.talk.utils.Utils.isAppIsRunning;

public class FirebaseCloudMessagingService extends FirebaseMessagingService {

    private String TAG = "FirebaseCloudMessagingService";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //getting the title , body and chat id

        String body = remoteMessage.getData().get("body");
        String title = remoteMessage.getData().get("title");
        String chatId = remoteMessage.getData().get("chatID");
        String userId = remoteMessage.getData().get("userID");


        if (isAppIsRunning(getApplicationContext())) {

        } else {
            MyNotificationManager.getInstance(this).displayNotification(title, body, userId, chatId);
        }
    }


}
