package org.wowtalk.ui.bitmapfun.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * A BitmapDrawable with additional attr to indicate whether it's in use.
 *
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-6
 * Time: AM10:47
 */
public class ResuableBitmapDrawable extends BitmapDrawable {
    private boolean inUse = false;

    public ResuableBitmapDrawable(Resources res, Bitmap bitmap, boolean inUse) {
        super(res, bitmap);
        this.inUse = inUse;
    }

    public void setUseState(boolean state) {
        inUse=state;
        if(!inUse) {
            Bitmap bmp=getBitmap();
            if(null != bmp && !bmp.isRecycled()) {
                //as reusable,if memCached is enabled in ImageCache,will be cached there,can not recycle
//                bmp.recycle();
            }
        }
    }

    public boolean isUsing() {
        return inUse;
    }
}
