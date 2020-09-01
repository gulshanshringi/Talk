package com.jsrd.talk.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.core.app.NotificationCompat;

import com.jsrd.talk.activities.ChatActivity;
import com.jsrd.talk.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class MyNotificationManager {
    private Context mContext;
    private static MyNotificationManager mInstance;
    private String chatingWith;

    private MyNotificationManager(Context context) {
        mContext = context;
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
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(mContext.getResources().getString(R.string.CHANNEL_ID), mContext.getResources().getString(R.string.CHANNEL_NAME), importance);
                mChannel.setDescription(mContext.getResources().getString(R.string.CHANNEL_DESCRIPTION));
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.RED);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                mNotificationManager.createNotificationChannel(mChannel);
            }

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext, mContext.getResources().getString(R.string.CHANNEL_ID))
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(getNameByNumber(title))
                            .setContentText(body).
                            setAutoCancel(true);

            Intent resultIntent = new Intent(mContext, ChatActivity.class);
            resultIntent.putExtra("UserNumber", title);
            resultIntent.putExtra("UserID", userId);
            resultIntent.putExtra("ChatID", chatID);

            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);

            NotificationManager mNotifyMgr =
                    (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

            if (mNotifyMgr != null) {
                mNotifyMgr.notify(1, mBuilder.build());
            }

        } else if (chatingWith.equalsIgnoreCase(title)) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(mContext, notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getNameByNumber(String number) {
        String res = null;
        try {
            ContentResolver resolver = mContext.getContentResolver();
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
