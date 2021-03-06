package com.pzy.paint;

import android.content.Context;
import android.graphics.*;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by pzy on 9/7/14.
 */
public class DoodleSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback, View.OnTouchListener {

    static class StrokeAttr implements Cloneable {
        public int width;
        public int color;
        public int opacity;

        public StrokeAttr() {
            this.width = 0;
            this.color = Color.BLUE;
            this.opacity = 255;
        }

        public StrokeAttr(int width, int color, int opacity) {
            this.width = width;
            this.color = color;
            this.opacity = opacity;
        }

        @Override
        public StrokeAttr clone() {
            return new StrokeAttr(width, color, opacity);
        }
    }

    private static final String TAG = "DoodleSurfaceView";
    LinkedList<Path> strokes = new LinkedList<Path>();
    Path currStroke = new Path();
    LinkedList<StrokeAttr> strokeAttrs = new LinkedList<StrokeAttr>();
    StrokeAttr currStrokeAttr = new StrokeAttr();
    float prevX, prevY;
    boolean dirty = true;
    Object drawSignal = new Object();
    private Paint paint = new Paint();
    private Bitmap backgroundBmp;
    private Rect canvasRect;

    public DoodleSurfaceView(Context context) {
        super(context, null, 0);
        setOnTouchListener(this);
        new DrawingThread().start();
    }

    public DoodleSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        setOnTouchListener(this);
        new DrawingThread().start();
    }

    public DoodleSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
        new DrawingThread().start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        getHolder().removeCallback(this);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final int bgcolor = 0xffddcc44;

        // draw background bmp

        paint.setAlpha(255);
        if (backgroundBmp != null) {
            Rect src = new Rect(0, 0, backgroundBmp.getWidth(), backgroundBmp.getHeight());
            if (canvasRect == null) {
                canvasRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                float hw1 = (float) src.height() / src.width();
                float hw2 = (float) canvasRect.height() / canvasRect.width();
                if (hw1 < hw2) {
                    canvasRect.bottom = (int) (canvasRect.width() * hw1);
                } else if (hw1 > hw2) {
                    canvasRect.right = (int) (canvasRect.height() / hw1);
                }
            }
            canvas.drawBitmap(backgroundBmp, src, canvasRect, paint);
        } else {
            canvas.drawColor(bgcolor);
        }

        //
        // draw strokes
        //

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        synchronized (strokes) {

            Iterator<StrokeAttr> i = strokeAttrs.iterator();
            for (Path path: strokes) {
                StrokeAttr a = i.next();
                paint.setStrokeWidth(a.width);
                paint.setColor(a.color);
                paint.setAlpha(a.opacity);
                canvas.drawPath(path, paint);
            }
        }

        synchronized (currStroke) {
            StrokeAttr a = currStrokeAttr;
            paint.setStrokeWidth(a.width);
            paint.setColor(a.color);
            paint.setAlpha(a.opacity);
            canvas.drawPath(currStroke, paint);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            synchronized (currStroke) {
                // 在同一个位置，有可能连续发生多次 ACTION_MOVE，虽然压力可能不同，但只要坐标相同，就不再采纳。
                if (currStroke.isEmpty()) {
                    currStroke.moveTo(event.getX(), event.getY());
                    setDirty();
                } else if ((int)prevX != (int)event.getX() || (int)prevY != (int)event.getY()) {
                    currStroke.lineTo(event.getX(), event.getY());
                    setDirty();
                }
                prevX = event.getX();
                prevY = event.getY();
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            synchronized (strokes) {
                strokes.add(currStroke);
                strokeAttrs.add(currStrokeAttr);
                currStroke = new Path();
                currStrokeAttr = currStrokeAttr.clone();
                setDirty();
            }
        }
        return true;
    }

    public Rect getCanvasRect() {
        return canvasRect;
    }

    private void setDirty() {
        dirty = true;
        synchronized (drawSignal) {
            drawSignal.notifyAll();
        }
    }

    public void clear() {
        synchronized (strokes) {
            strokes.clear();
            strokeAttrs.clear();
        }

        synchronized (currStroke) {
            currStroke = new Path();
        }


        setDirty();
    }


    public void setStrokeAttr(int width, int color, int opacity) {
        currStrokeAttr.width = width;
        currStrokeAttr.color = color;
        currStrokeAttr.opacity = opacity;
    }

    public void setBackgroundBmp(Bitmap backgroundBmp) {
        this.backgroundBmp = backgroundBmp;
    }

    private class DrawingThread extends Thread {
        DoodleSurfaceView view;

        public DrawingThread() {
            this.view = DoodleSurfaceView.this;
        }

        @Override
        public void run() {

            while (true) {
                if (dirty) {
                    Canvas canvas = view.getHolder().lockCanvas();
                    if (canvas != null) {
                        dirty = false;
                        draw(canvas);

                        view.getHolder().unlockCanvasAndPost(canvas);
                    }
                }

                try {
                    synchronized (drawSignal) {
                        drawSignal.wait(50);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
