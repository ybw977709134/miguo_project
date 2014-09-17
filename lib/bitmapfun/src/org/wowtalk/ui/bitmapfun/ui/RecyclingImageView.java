package org.wowtalk.ui.bitmapfun.ui;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import org.wowtalk.ui.bitmapfun.BuildConfig;
import org.wowtalk.ui.bitmapfun.util.RecyclingBitmapDrawable;
import org.wowtalk.ui.bitmapfun.util.ResuableBitmapDrawable;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/22/13
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecyclingImageView extends ImageView {
    private static final String LOG_TAG = "RecyclingImageView";
    private boolean clickDim = false;

    public RecyclingImageView(Context context) {
        super(context);
    }

    public RecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setClickDim(boolean dim) {
        clickDim = dim;
    }

    private void makeImageDim() {
        float[] colorFilters;
        ColorMatrix colorMatrix;
        ColorMatrixColorFilter cmcf;

        float multiFactor = 0.7f;
        float addFactor = 0;

        if (!clickDim)
            return;

        colorFilters = new float[] {
                multiFactor, 0, 0, 0, addFactor,
                0, multiFactor, 0, 0, addFactor,
                0, 0, multiFactor, 0, addFactor,
                0, 0, 0, 1.0f, 0
        };
        colorMatrix = new ColorMatrix(colorFilters);
        cmcf = new ColorMatrixColorFilter(colorMatrix);
        setColorFilter(cmcf);
        invalidate();
    }

    private void revertToOriginImage() {
        float[] colorFilters;
        ColorMatrix colorMatrix;
        ColorMatrixColorFilter cmcf;

        if (!clickDim)
            return;

        colorFilters = new float[] {
                1.0f, 0, 0, 0, 0,
                0, 1.0f, 0, 0, 0,
                0, 0, 1.0f, 0, 0,
                0, 0, 0, 1.0f, 0
        };
        colorMatrix = new ColorMatrix(colorFilters);
        cmcf = new ColorMatrixColorFilter(colorMatrix);
        setColorFilter(cmcf);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                makeImageDim();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                revertToOriginImage();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        final Drawable previousDrawable = getDrawable();
        super.setImageDrawable(drawable);
        notifyDrawable(drawable, true);

        notifyDrawable(previousDrawable, false);
    }

    private static void notifyDrawable(Drawable drawable, final boolean isDisplayed) {
        if(null == drawable) {
            return;
        }
        boolean expectedClass = false;
        if (drawable instanceof RecyclingBitmapDrawable) {
            ((RecyclingBitmapDrawable) drawable).setIsDisplayed(isDisplayed);
            expectedClass = true;
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0, z = layerDrawable.getNumberOfLayers(); i < z; i++) {
                notifyDrawable(layerDrawable.getDrawable(i), isDisplayed);
            }
            expectedClass = true;
        } else if (drawable instanceof ResuableBitmapDrawable) {
            ((ResuableBitmapDrawable)drawable).setUseState(isDisplayed);
            expectedClass = true;
        }

        if (BuildConfig.DEBUG && drawable != null && !expectedClass)
            Log.d(LOG_TAG, "unexpected drawable class: " + drawable.getClass().getName());
    }
}
