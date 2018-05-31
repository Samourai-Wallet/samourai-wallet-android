package com.samourai.wallet;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.samourai.wallet.widgets.ArcProgress;

public class TestUI2Activity extends AppCompatActivity {

    private ArcProgress progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ui2);
        progress = findViewById(R.id.progress);
        progress.start(2);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageView v = findViewById(R.id.imageView);
                Drawable d = v.getDrawable();
                if (d instanceof Animatable) {
                    Log.i("SD", "run: AKDO");
                    ((Animatable) d).start();
                }
            }
        },3000);

    }
}
