package org.wowtalk.ui.msg;

import org.wowtalk.Log;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * This is a RelativeLayout with the ability to notify height change,
 * usually caused by toggling of soft keyboard. So it can be useful
 * for detecting keyboard state.
 *
 * <p>
 * View.addOnLayoutChangeListener() should be utilized to detect height
 * change, problem is that it requires API LEVEL 11.
 * </p>
 */
public class HeightAwareRelativeLayout extends RelativeLayout
        implements IHeightAwareView{

    public static final byte KEYBOARD_STATE_INIT = -1;
    public static final byte KEYBOARD_STATE_HIDE = 0;
    public static final byte KEYBOARD_STATE_SHOW = 1;

    private boolean mHasInit;
    private boolean mHasKeybord;
    private int mHeight;
    private IKeyboardStateChangedListener mKeyboardListener;

    private int mMaxHeight = 0;
    private int mRequestHeight = 0;
    private IHeightAwareView.OnHeightChangedListener mL = null;

    public HeightAwareRelativeLayout(Context context) {
        super(context, null, 0);
    }

    public HeightAwareRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public HeightAwareRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        Log.e(String.format("HeightAwareRelativeLayout.onLayout %s %d, %d, %d, %d",
//                changed, left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);

        if(mRequestHeight != bottom - top && mL != null) {
            mL.onHeightChanged(bottom - top,
                    (mMaxHeight < bottom - top ? bottom - top : mMaxHeight));
        }

        mRequestHeight = bottom - top;
        if(mRequestHeight > mMaxHeight) {
            mMaxHeight = mRequestHeight;
        }

        // keyboard state
        setKeyboardState(bottom);
    }

    private void setKeyboardState(int bottom) {
        if (!mHasInit) {
            mHasInit = true;
            mHeight = bottom;
            if (mKeyboardListener != null) {
                mKeyboardListener.onKeyboardStateChanged(KEYBOARD_STATE_INIT);
            }
        } else {
            mHeight = mHeight < bottom ? bottom : mHeight;
        }

        if (mHasInit && mHeight > bottom) {
            mHasKeybord = true;
            if (mKeyboardListener != null) {
                mKeyboardListener.onKeyboardStateChanged(KEYBOARD_STATE_SHOW);
            }
            Log.d("show keyboard.......");
        }
        if (mHasInit && mHasKeybord && mHeight == bottom) {
            mHasKeybord = false;
            if (mKeyboardListener != null) {
                mKeyboardListener.onKeyboardStateChanged(KEYBOARD_STATE_HIDE);
            }
            Log.d("hide keyboard.......");
        }
    }

    @Override
    public void setOnHeightChangedListener(IHeightAwareView.OnHeightChangedListener l) {
        mL = l;
    }

    public int maxHeight() {
        return mMaxHeight;
    }

    public int requestHeight() {
        return mRequestHeight;
    }

    public void setKeyboardChangedListener(IKeyboardStateChangedListener listener) {
        mKeyboardListener = listener;
    }

    public interface IKeyboardStateChangedListener {
        public void onKeyboardStateChanged(int state);
    }
}
