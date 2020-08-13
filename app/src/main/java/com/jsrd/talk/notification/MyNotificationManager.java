package com.jsrd.talk.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.core.app.NotificationCompat;

import com.jsrd.talk.activities.ChatActivity;
import com.jsrd.talk.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class MyNotificationManager {
    private Context mCtx;
    private static MyNotificationManager mInstance;
    private String chatingWith;

    private MyNotificationManager(Context context) {
        mCtx = context;
    }

    public static synchronized MyNotificationManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MyNotificationManager(context);
        }
        return mInstance;
    }


    public void displayNotification(String title, String body, String userId, String chatID) {

        chatingWith = ChatActivity.chatingWith;

        if (chatingWith == null || !chatingWith.equals(title)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationManager mNotificationManager =
                        (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(mCtx.getResources().getString(R.string.CHANNEL_ID), mCtx.getResources().getString(R.string.CHANNEL_NAME), importance);
                mChannel.setDescription(mCtx.getResources().getString(R.string.CHANNEL_DESCRIPTION));
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.RED);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                mNotificationManager.createNotificationChannel(mChannel);
            }

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mCtx, mCtx.getResources().getString(R.string.CHANNEL_ID))
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(getNameByNumber(title))
                            .setContentText(body).
                            setAutoCancel(true);

            Intent resultIntent = new Intent(mCtx, ChatActivity.class);
            resultIntent.putExtra("UserNumber", title);
            resultIntent.putExtra("UserID", userId);
            resultIntent.putExtra("ChatID", chatID);

            PendingIntent pendingIntent = PendingIntent.getActivity(mCtx, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);

            NotificationManager mNotifyMgr =
                    (NotificationManager) mCtx.getSystemService(NOTIFICATION_SERVICE);

            if (mNotifyMgr != null) {
                mNotifyMgr.notify(1, mBuilder.build());
            }
        }
    }

    private String getNameByNumber(String number) {
        String res = null;
        try {
            ContentResolver resolver = mCtx.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor c = resolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            if (c != null) { // cursor not null means number is found contactsTable
                if (c.moveToFirst()) {   // so now find the contact Name
                    res = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                }
                c.close();
            }
        } catch (Exception ex) {
            /* Ignore */
        }
        return res;
    }

}
