package co.onemeter.oneapp.widget.scale_viewpager;

import co.onemeter.oneapp.ui.Log;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Found at http://stackoverflow.com/questions/7814017/is-it-possible-to-disable-scrolling-on-a-viewpager.
 * Convenient way to temporarily disable ViewPager navigation while interacting with ImageView.
 * 
 * Julia Zudikova
 */

/**
 * Hacky fix for Issue #4 and
 * http://code.google.com/p/android/issues/detail?id=18990
 * <p/>
 * ScaleGestureDetector seems to mess up the touch events, which means that
 * ViewGroups which make use of onInterceptTouchEvent throw a lot of
 * IllegalArgumentException: pointerIndex out of range.
 * <p/>
 * There's not much I can do in my code for now, but we can mask the result by
 * just catching the problem and ignoring it.
 *
 * @author Chris Banes
 */
public class PhotoViewPager extends ViewPager {

    private boolean mIsLocked;

    public PhotoViewPager(Context context) {
        super(context);
        mIsLocked = false;
    }

    public PhotoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsLocked = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mIsLocked) {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException exception) {
                Log.w("photo_view ", exception);
                return false;
            } catch (ArrayIndexOutOfBoundsException exception) {
                Log.w("photo_view ", exception);
                return false;
            } catch (Exception exception) {
                Log.w("photo_view ", exception);
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsLocked) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    public void toggleLock() {
        mIsLocked = !mIsLocked;
    }

    public void setLocked(boolean isLocked) {
        mIsLocked = isLocked;
    }

    public boolean isLocked() {
        return mIsLocked;
    }

}
