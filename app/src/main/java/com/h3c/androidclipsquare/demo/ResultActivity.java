package com.h3c.androidclipsquare.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

/**
 * Created by H3c on 12/15/14.
 */
public class ResultActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);

        byte[] bis = getIntent().getByteArrayExtra("bitmap");
        if(bis != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
            if(bitmap != null){
                ((ImageView) findViewById(R.id.clipResultIV)).setImageBitmap(bitmap);
            }
        }
    }
}
