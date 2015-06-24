package org.wowtalk.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.wowtalk.ui.messagebox.R;

/**
 * Show a alert dialog, progress dialog, or toast.
 * <p>show type:
 * <ul>
 *     <li>msg(default): show text message</li>
 *     <li>toast: show text message, auto dismiss</li>
 *     <li>wait: show progress bar</li>
 * </ul></p></p>
 */
public class MessageBox {

    private Context mContext;
    private Activity mActivity;
    private Dialog mMsgDlg;
    private Dialog mWaitDlg;
    private TextView mWatiTextView;
    private Toast mToast;

    public MessageBox(Context context) {
        mContext = context;
        if (context instanceof Activity)
            mActivity = (Activity)context;
    }

    /**
     * Show a {@link AlertDialog} as message box.
     *
     * <p>Will try to run on current thread, if this is not possible
     * (because the context passed in ctor is not a instance of
     * {@link Activity}, run on current thread.</p>
     *
     * @param title can be null.
     * @param msg
     */
    public void show(final String title, final String msg) {
        if (mActivity != null && !mActivity.isFinishing()) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    showOnCurrThrd(title, msg);
                }
            });
        } else {
            showOnCurrThrd(title, msg);
        }
    }

    /**
     * Show a {@link AlertDialog} as message box, running on current thread.
     * @param title can be null.
     * @param msg
     */
    private void showOnCurrThrd(final String title, final String msg) {
        _msgOnCurrThrd(msg);
    }

    public void showDlg(final String title, final String msg) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _dlgOnCurThrd(title, msg);
                }
            });
        } else {
            _dlgOnCurThrd(title, msg);
        }
    }

    public boolean isWaitShowing () {
        return null != mWaitDlg && mWaitDlg.isShowing();
    }


    /**
     * 只显示进度条的弹框
     * @author hutianfeng
     * @date 2015/3/13
     */
    public void showWait() {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait, null);
       
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    /**
     * 显示进度条和文本弹框
     * @param message
     * @author hutianfeng
     * @date 2015/3/13
     */
    public void showWaitProgressbar(String message) {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait_progressbar, null);
        mWatiTextView = (TextView) waitView.findViewById(R.id.wait_message);
        if (TextUtils.isEmpty(message)) {
            mWatiTextView.setVisibility(View.GONE);
        } else {
            mWatiTextView.setVisibility(View.VISIBLE);
            mWatiTextView.setText(message);
        }
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    /**
     * 显示图片和文本的弹框 (成功时)
     * @param message
     * @author hutianfeng
     * @date 2015/3/13
     */
    public void showWaitImageSuccess(String message) {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait_image, null);
        mWatiTextView = (TextView) waitView.findViewById(R.id.wait_message);
        if (TextUtils.isEmpty(message)) {
            mWatiTextView.setVisibility(View.GONE);
        } else {
            mWatiTextView.setVisibility(View.VISIBLE);
            mWatiTextView.setText(message);
        }
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    
    /**
     * 显示图片和文本的弹框 (小图框，成功时)
     * @param message
     * @author hutianfeng
     * @date 2015/4/13
     */
    public void showWaitImageSuccessSmall(String message) {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait_image, null);
        mWatiTextView = (TextView) waitView.findViewById(R.id.wait_message);
        ((ImageView) waitView.findViewById(R.id.imageView_icon_success)).setImageResource(R.drawable.icon_success_small);
        if (TextUtils.isEmpty(message)) {
            mWatiTextView.setVisibility(View.GONE);
        } else {
            mWatiTextView.setVisibility(View.VISIBLE);
            mWatiTextView.setText(message);
        }
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    
    /**
     * 显示图片和文本的弹框 (警告时)
     * @param message
     * @author hutianfeng
     * @date 2015/4/13
     */
    public void showWaitImageCaution(String message) {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait_image, null);
        mWatiTextView = (TextView) waitView.findViewById(R.id.wait_message);
       ((ImageView) waitView.findViewById(R.id.imageView_icon_success)).setImageResource(R.drawable.icon_caution);
        
        
        if (TextUtils.isEmpty(message)) {
            mWatiTextView.setVisibility(View.GONE);
        } else {
            mWatiTextView.setVisibility(View.VISIBLE);
            mWatiTextView.setText(message);
        }
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    
    /**
     * 显示图片和文本的弹框 (失败时)
     * @param message
     * @author hutianfeng
     * @date 2015/4/13
     */
    public void showWaitImageWorng(String message) {
        // 每次 showWait()时，都需要重新布局，否则会出现滚动条不转动的情况
        mWaitDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View waitView = LayoutInflater.from(mContext).inflate(R.layout.msgbox_wait_image, null);
        mWatiTextView = (TextView) waitView.findViewById(R.id.wait_message);
        ((ImageView) waitView.findViewById(R.id.imageView_icon_success)).setImageResource(R.drawable.icon_wrong);
        if (TextUtils.isEmpty(message)) {
            mWatiTextView.setVisibility(View.GONE);
        } else {
            mWatiTextView.setVisibility(View.VISIBLE);
            mWatiTextView.setText(message);
        }
        mWaitDlg.setContentView(waitView);
        Window window = mWaitDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if(android.os.Build.VERSION.SDK_INT >= 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mWaitDlg.setCanceledOnTouchOutside(false);
        try {
            mWaitDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }

    public void showOrUpdateWatiMsg(int resId) {
        showOrUpdateWatiMsg(mContext.getString(resId));
    }

    public void showOrUpdateWatiMsg(String message) {
        if (isWaitShowing()) {
            if (null != mWatiTextView) {
                if (TextUtils.isEmpty(message)) {
                    mWatiTextView.setVisibility(View.GONE);
                } else {
                    mWatiTextView.setVisibility(View.VISIBLE);
                    mWatiTextView.setText(message);
                }
            }
        } else {
            showWaitProgressbar(message);
        }
    }

    /**
     * Try to dismiss the progress dialog shown by
     * {@link #showWait()}.
     *
     * Can be called on non-UI thread.
     */
    public void dismissWait() {
        if (mActivity != null && Looper.myLooper() != Looper.getMainLooper()) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissProgressDialogOnCurrThrd();
                }
            });
        } else {
            dismissProgressDialogOnCurrThrd();
        }
    }

    /**
     * Try to dismiss the progress dialog shown by
     * {@link #showWait()}, running on current thread.
     */
    private void dismissProgressDialogOnCurrThrd() {
        if (mWaitDlg != null) {
            try {
                mWaitDlg.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mWaitDlg = null;
            }
        }
    }

    /**
     * Make a Toast and show it.
     *
     * <p>Will try to run on current thread, if this is not possible
     * (because the context passed in ctor is not a instance of
     * {@link Activity}), run on current thread.</p>
     *
     * <p>If a previous Toast is showing, it will be updated to show the
     * new message immediately.</p>
     *
     * @param msgResId
     * @param durationMs duration in ms, 0 means forever.
     */
    public void toast(final int msgResId, final int durationMs) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toastOnCurrThrd(msgResId, durationMs);
                }
            });
        } else {
            toastOnCurrThrd(msgResId, durationMs);
        }
    }

    public void toast(final int msgResId) {
        toast(msgResId, 2000);
    }

    /**
     * Make a Toast and show it.
     *
     * <p>If a previous Toast is showing, it will be updated to show the
     * new message immediately.</p>
     *
     * @param msgResId
     */
    private void toastOnCurrThrd(int msgResId, int durationMs) {
        _toastOnCurrThrd(mContext.getString(msgResId), durationMs);
    }

    /**
     * Make a Toast and show it.
     *
     * <p>Will try to run on current thread, if this is not possible
     * (because the context passed in ctor is not a instance of
     * {@link Activity}), run on current thread.</p>
     *
     * <p>If a previous Toast is showing, it will be updated to show the
     * new message immediately.</p>
     *
     * @param msg
     * @param durationMs duration in ms, 0 means forever.
     */
    public void toast(final String msg, final int durationMs) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toastOnCurrThrd(msg, durationMs);
                }
            });
        } else {
            toastOnCurrThrd(msg, durationMs);
        }
    }

    public void toast(final String msg) {
        toast(msg, 2000);
    }

    /**
     * Make a Toast and show it.
     *
     * <p>If a previous Toast is showing, it will be updated to show the
     * new message immediately.</p>
     *
     * @param msg
     */
    private void toastOnCurrThrd(String msg, int durationMs) {
        _toastOnCurrThrd(msg, durationMs);
    }

    private void _msgOnCurrThrd(String msg) {
        if (mMsgDlg == null) {
        }
        mMsgDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        mMsgDlg.setContentView(R.layout.msgbox_msg);
        Window window = mMsgDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT > 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mMsgDlg.setCanceledOnTouchOutside(true);
        ((TextView)mMsgDlg.findViewById(R.id.text1)).setText(msg);
        try {
            mMsgDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }

    private void _dlgOnCurThrd(String title, String msg) {
        if (mMsgDlg == null) {
        }
        mMsgDlg = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        mMsgDlg.setContentView(R.layout.msgbox_dialog);
        Window window = mMsgDlg.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT > 14) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        mMsgDlg.setCanceledOnTouchOutside(true);
        ((TextView) mMsgDlg.findViewById(R.id.title)).setText(title);
        ((TextView) mMsgDlg.findViewById(R.id.text)).setText(msg);
        mMsgDlg.findViewById(R.id.btn_single).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsgDlg.dismiss();
            }
        });
        try {
            mMsgDlg.show();
        } catch (Exception e) {
            // try to catch: android.view.WindowManager$BadTokenException
            e.printStackTrace();
        }
    }
    
    /**
     * 自定义提示对话框提示3秒后消失
     */
    public void dismissDialog(){
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					mMsgDlg.dismiss();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		}).start();
    }

    private void _toastOnCurrThrd(String msg, int durationMs) {
        TextView text;

        if (mToast == null) {
            mToast = new Toast(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View layout = inflater.inflate(R.layout.msgbox_toast, null);

            text = (TextView)layout.findViewById(R.id.text1);
            text.setText(msg);

            mToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            mToast.setDuration(Toast.LENGTH_SHORT);
            mToast.setView(layout);
        } else {
            text = (TextView)mToast.getView().findViewById(R.id.text1);
        }
        text.setText(msg);
        mToast.show();
//        dismissToast();
    }

    /**
     * Try to dismiss toast.
     *
     * Can be called on non-UI thread.
     */
    public void dismissToast() {
        if (mActivity != null && Looper.myLooper() != Looper.getMainLooper()) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        _dismissToast();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            _dismissToast();
        }
    }

    private void _dismissToast() {
        if (mToast != null)
            mToast.cancel();
    }
}
