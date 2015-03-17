package com.pzy.paint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import com.androidquery.AQuery;

import java.io.FileOutputStream;

public class DoodleActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_BACKGROUND_FILENAME = "bgfilename";
    public static final String EXTRA_MAX_WIDTH = "maxw";
    public static final String EXTRA_MAX_HEIGHT = "maxh";
    public static final String EXTRA_OUTPUT_FILENAME = "outputfilename";

    DoodleSurfaceView surfaceView;
    Bitmap bmp;
    String inFilename;
    String outFilename;
    int width = 1280;
    int height = 800;

    // stroke attrs
    int strokeWidth = 10;
    int strokeOpacity = 255; // [0,255]
    int strokeColor = Color.BLUE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getLayoutResId());

        surfaceView = (DoodleSurfaceView) findViewById(R.id.doodle_canvas);

        getData(savedInstanceState);
        setCanvasBackground();
        updateUI();
        updatePaint();
        setupEventHandlers();
    }

    private void updateUI() {
        AQuery q = new AQuery(this);
        //q.find(R.id.stroke_width).getSeekBar().setProgress(strokeWidth);
        //q.find(R.id.stroke_opacity).getSeekBar().setProgress(strokeOpacity);
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.title_confirm).clicked(this);
        q.find(R.id.linearLayout_cancel).clicked(this);
    }

    private void setupEventHandlers() {
        AQuery q = new AQuery(this);
        /*q.find(R.id.stroke_width).getSeekBar().setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            strokeWidth = progress;
                            updatePaint();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );

        q.find(R.id.stroke_opacity).getSeekBar().setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            strokeOpacity = progress;
                            updatePaint();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );
        */
        ViewGroup palette =(ViewGroup) findViewById(R.id.palette);
        for (int i = 0, n = palette.getChildCount(); i < n; ++i) {
            View v = palette.getChildAt(i);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    strokeColor = ((ColorDrawable)view.getBackground()).getColor();
                    updatePaint();
                }
            });
        }
    }

    private void updatePaint() {
        surfaceView.setStrokeAttr(strokeWidth, strokeColor, strokeOpacity);
    }

    private void getData(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            inFilename = getIntent().getStringExtra(EXTRA_BACKGROUND_FILENAME);
            outFilename = getIntent().getStringExtra(EXTRA_OUTPUT_FILENAME);
            width = getIntent().getIntExtra(EXTRA_MAX_WIDTH, width);
            height = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, height);
        } else {
            inFilename = savedInstanceState.getString(EXTRA_BACKGROUND_FILENAME);
            outFilename = savedInstanceState.getString(EXTRA_OUTPUT_FILENAME);
            width = savedInstanceState.getInt(EXTRA_MAX_WIDTH, width);
            height = savedInstanceState.getInt(EXTRA_MAX_HEIGHT, height);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_BACKGROUND_FILENAME, inFilename);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    private void setCanvasBackground() {
        if (inFilename != null) {
            bmp = BmpUtils.decodeFile(inFilename, width, height);
            bmp = bmp.copy(Config.ARGB_8888, true);
        }

        if (bmp == null) {
            bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        }

        width = bmp.getWidth();
        height = bmp.getHeight();

        surfaceView.setBackgroundBmp(bmp);
    }

    protected int getLayoutResId() {
        return R.layout.activity_doodle;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.title_back) {
            onBackPressed();
        } else if (i == R.id.title_confirm) {
            setResult();
            finish();
        }else if(i == R.id.linearLayout_cancel){
        	surfaceView.clear();
        }
    }

    private void setResult() {
        try {
            if (outFilename != null) {
                Rect canvasSize = surfaceView.getCanvasRect();
                Bitmap outBmp = Bitmap.createBitmap(canvasSize.width(), canvasSize.height(), Config.ARGB_8888);
                Canvas c = new Canvas(outBmp);
                surfaceView.draw(c);
                outBmp.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(outFilename));
                setResult(Activity.RESULT_OK, new Intent());
            } else {
                setResult(Activity.RESULT_CANCELED, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
