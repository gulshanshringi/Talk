package com.jsrd.talk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {


    public Utils() {
    }

    public static String getNameByNumber(Context context, String number) {
        String res = null;
        try {
            ContentResolver resolver = context.getContentResolver();
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


    public static String getDateTime(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss");
        Date date;
        String time = null;
        try {
            date = sdf.parse(dateTime);
            if (isToday(date)) {
                SimpleDateFormat sdf1 = new SimpleDateFormat("KK:mm a", Locale.getDefault());
                time = sdf1.format(date);
                return time;
            }else {
                SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                time = sdf1.format(date);
                return time;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return time;
    }

    private static boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar specifiedDate = Calendar.getInstance();
        specifiedDate.setTime(date);

        return today.get(Calendar.DAY_OF_MONTH) == specifiedDate.get(Calendar.DAY_OF_MONTH)
                && today.get(Calendar.MONTH) == specifiedDate.get(Calendar.MONTH)
                && today.get(Calendar.YEAR) == specifiedDate.get(Calendar.YEAR);
    }


}
