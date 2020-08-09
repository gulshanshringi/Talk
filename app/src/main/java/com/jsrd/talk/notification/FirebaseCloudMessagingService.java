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

//        Log.i("FirebaseCloudMessaging",title + " " + body);
//        FirebaseUtils firebaseUtils = new FirebaseUtils(getApplicationContext());
//        firebaseUtils.putChatDataOnFirebaseForSender(title, userId, chatId, new ProgressSuccessCallBack() {
//            @Override
//            public void onSuccess(boolean success) {
//                displayNotification(title, body);
//            }
//        });

        if (isForeground(getApplicationContext())) {

        } else {
            MyNotificationManager.getInstance(this).displayNotification(title, body, userId,chatId);
        }
    }

    private static boolean isForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : tasks) {
            if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == appProcess.importance && packageName.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }

}
