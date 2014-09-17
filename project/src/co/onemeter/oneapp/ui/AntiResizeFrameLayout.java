package co.onemeter.oneapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Will not re-layout when soft keyboard turns on.
 */
public class AntiResizeFrameLayout extends FrameLayout {
    private int mMaxHeight = 0;

    public AntiResizeFrameLayout(Context context) {
        super(context, null, 0);
    }
    public AntiResizeFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }
    public AntiResizeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        Log.e(String.format("AntiResizeFrameLayout.onLayout %s %d, %d, %d, %d",
//                changed, left, top, right, bottom));
        if(bottom - top < mMaxHeight)
            return; // never shrink!
        mMaxHeight = bottom - top;
        super.onLayout(changed, left, top, right, bottom);
    }

}
