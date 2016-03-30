package com.samourai.wallet.util;

import android.content.Context;
import android.graphics.Typeface;

public class TypefaceUtil {

    public static int awesome_arrow_down = 0xf063;
    public static int awesome_arrow_up = 0xf062;

    private static Typeface awesome_font = null;

    private static TypefaceUtil instance = null;

    private TypefaceUtil() { ; }

    public static TypefaceUtil getInstance(Context ctx) {

        if(instance == null) {
            instance = new TypefaceUtil();
            awesome_font = Typeface.createFromAsset(ctx.getAssets(), "fontawesome-webfont.ttf");
        }

        return instance;
    }

    public Typeface getAwesomeTypeface() {
        return awesome_font;
    }

}
