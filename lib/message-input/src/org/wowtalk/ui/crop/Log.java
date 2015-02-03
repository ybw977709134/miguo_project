package org.wowtalk.ui.crop;

class Log {

    private static final String TAG = "android-crooper";

    public static final void e(String msg) {
        android.util.Log.e(TAG, msg);
    }

    public static final void e(String msg, Throwable e) {
        android.util.Log.e(TAG, msg, e);
    }

}
