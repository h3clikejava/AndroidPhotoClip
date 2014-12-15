package com.h3c.androidclipsquare.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.h3c.androidclipsquare.ClipSquareImageView;

import java.io.ByteArrayOutputStream;


public class MainActivity extends Activity {
    private ClipSquareImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().hide();

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.t2);
        imageView = (ClipSquareImageView) findViewById(R.id.clipSquareIV);
        imageView.setImageBitmap(bmp);

        findViewById(R.id.doneBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 此处获取剪裁后的bitmap
                Bitmap bitmap = imageView.clip();

                // 由于Intent传递bitmap不能超过40k,此处使用二进制数组传递
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] bitmapByte = baos.toByteArray();

                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("bitmap", bitmapByte);
                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
