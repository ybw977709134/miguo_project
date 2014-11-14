package com.pzy.paint;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by pzy on 11/14/14.
 */
public class MyActivity extends Activity {
    private static final int REQ_DOODLE = 123;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startActivityForResult(
                new Intent(this, DoodleActivity.class)
                .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, "/sdcard/tmp/titanic_i_see_you_3060.jpg")
                .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, "/sdcard/tmp/doodle.jpg"),
                REQ_DOODLE
        );
    }
}