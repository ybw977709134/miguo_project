package org.wowtalk.ui.msg;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * <p>在照片上涂鸦。</p>
 * Created by pzy on 10/28/14.
 */
public class DoodleActivity extends Activity {
    public static final String EXTRA_BACKGROUND_FILENAME = "bgfilename";
    public static final String EXTRA_MAX_WIDTH = "maxw";
    public static final String EXTRA_MAX_HEIGHT = "maxh";
    public static final String EXTRA_OUTPUT_FILENAME = "outputfilename";

    ImageView surfaceView;
    Bitmap bmp;
    String filename;
    int width = 1280;
    int height = 800;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doodle);

        surfaceView = (ImageView) findViewById(R.id.canvas);

        if (savedInstanceState == null) {
            filename = getIntent().getStringExtra(EXTRA_BACKGROUND_FILENAME);
            width = getIntent().getIntExtra(EXTRA_MAX_WIDTH, width);
            height = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, height);
        } else {
            filename = savedInstanceState.getString(EXTRA_BACKGROUND_FILENAME);
            width = savedInstanceState.getInt(EXTRA_MAX_WIDTH, width);
            height = savedInstanceState.getInt(EXTRA_MAX_HEIGHT, height);
        }

        if (filename != null) {
            bmp = BmpUtils.decodeFile(filename, width, height);
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        }

        if (bmp == null) {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        canvas.drawCircle(50, 50, 20, paint);
        surfaceView.setImageBitmap(bmp);

        setResult(Activity.RESULT_OK,
                new Intent()
                        .putExtra(EXTRA_OUTPUT_FILENAME, filename)
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_BACKGROUND_FILENAME, filename);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 9, new FileOutputStream(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}