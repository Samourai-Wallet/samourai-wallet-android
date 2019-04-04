package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    private static DateUtil instance = null;
    private static Date now = null;
    private static Context context = null;

    private DateUtil() { ; }

    public static DateUtil getInstance(Context ctx) {

        now = new Date();
        context = ctx;

        if(instance == null) {
            instance = new DateUtil();
        }

        return instance;
    }

    public String formatted(long ts) {
        String ret = null;

        Date localTime = new Date(ts);
        long date = localTime.getTime();

        long hours24 = 60L * 60L * 24;
        long now = System.currentTimeMillis() / 1000L;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(now * 1000L));
        int nowYear = cal.get(Calendar.YEAR);
        int nowDay = cal.get(Calendar.DAY_OF_MONTH);

        cal.setTime(new Date(date * 1000L));
        int thenYear = cal.get(Calendar.YEAR);
        int thenDay = cal.get(Calendar.DAY_OF_MONTH);

        // within 24h
        if(now - date < hours24) {
            if(thenDay < nowDay) {
                SimpleDateFormat sd = new SimpleDateFormat("E dd MMM HH:mm");
                sd.setTimeZone(TimeZone.getDefault());
                ret = sd.format(date * 1000L);
            }
            else {
                SimpleDateFormat sd = new SimpleDateFormat("HH:mm");
                sd.setTimeZone(TimeZone.getDefault());
                ret = group(date) + " " + sd.format(date * 1000L);
            }

        }
        else {
            if(thenYear < nowYear) {
                SimpleDateFormat sd = new SimpleDateFormat("dd MMM yyyy");
                sd.setTimeZone(TimeZone.getDefault());
                ret = sd.format(date * 1000L);
            }
            else {
                SimpleDateFormat sd = new SimpleDateFormat("E dd MMM HH:mm");
                sd.setTimeZone(TimeZone.getDefault());
                ret = sd.format(date * 1000L);
            }
        }

        return ret;
    }

    public String group(long date) {
        String ret = null;

        Date localTime = new Date(date);
        date = localTime.getTime();

        long hours24 = 60L * 60L * 24;
        long now = System.currentTimeMillis() / 1000L;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(now * 1000L));
        int nowYear = cal.get(Calendar.YEAR);
        int nowDay = cal.get(Calendar.DAY_OF_MONTH);

        cal.setTime(new Date(date * 1000L));
        int thenYear = cal.get(Calendar.YEAR);
        int thenDay = cal.get(Calendar.DAY_OF_MONTH);

        // within 24h
        if(now - date < hours24) {
            if(thenDay < nowDay) {
                ret = context.getString(R.string.timeline_older);
            }
            else {
                ret = context.getString(R.string.timeline_today);
            }
        }
        else {
            ret = context.getString(R.string.timeline_older);
        }

        return ret;
    }

}
