package org.wowtalk.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.wowtalk.ui.messagebox.R;

/**
 * Created by jacky on 15-3-12.
 * this is a simple AlertDialog,can use it show some info.
 */
public class MessageDialog extends AlertDialog implements View.OnClickListener{
    /**
     * large dialog
     */
    public static final int  SIZE_LARGE = 3;

    /**
     * normal dialog
     */
    public static final int SIZE_NORMAL = 2;

    /**
     * small dialog
     */
    public static final int SIZE_SMALL = 1;


    public static interface MessageDialogClickListener{
        void onclick(MessageDialog dialog);
    }

    
    private Button btn_ok;
    private Button btn_cancel;
    private Button btn_single;
    private TextView txt_title;
    
    private Context mContext;
    private MessageDialogClickListener mOnOkClickListener;
    private MessageDialogClickListener mCancelListener;

    private String mTitle = "";
    private String mMsg = "";
    private String mBtnLeftText = "确定";
    private String mBtnRightText = "取消";

    private boolean mIsDouleBtn = false;

    private int mSize = SIZE_NORMAL;

    /**
     * this constractor create default duoble button
     * @param context
     */
    public MessageDialog(Context context){
        this(context,0, true, SIZE_NORMAL);
    }

    /**
     * isDoubleBtn is false,dialog has one btn
     * @param context
     * @param isDouleBtn
     */
    public MessageDialog(Context context, boolean isDouleBtn, int size){
        this(context,0, isDouleBtn,size);
    }

    protected MessageDialog(Context context, int theme, boolean isDouleBtn, int size) {
        super(context, theme);
        mContext = context;
        mIsDouleBtn = isDouleBtn;
        mSize = size;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lay_messagedialog);
        if (mIsDouleBtn){
            findViewById(R.id.lay_btn_double).setVisibility(View.VISIBLE);
            findViewById(R.id.lay_btn_single).setVisibility(View.GONE);
        }else{
            findViewById(R.id.lay_btn_double).setVisibility(View.GONE);
            findViewById(R.id.lay_btn_single).setVisibility(View.VISIBLE);
        }

        btn_ok = (Button) findViewById(R.id.buttonOK);
        btn_cancel = (Button) findViewById(R.id.btnCancel);
        btn_single = (Button) findViewById(R.id.btnOK_single);
         txt_title = (TextView) findViewById(R.id.txt_title);

        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_single.setOnClickListener(this);


        txt_title.setText(mTitle);
        
        TextView txt_msg = (TextView) findViewById(R.id.txt_msg);
        if (!mIsDouleBtn){
             txt_msg.setGravity(Gravity.CENTER);
        }
        if(mTitle.length() == 0){
        	txt_title.setVisibility(View.GONE);
            txt_msg.setGravity(Gravity.CENTER);
        }
        txt_msg.setText(mMsg);
        btn_ok.setText(mBtnLeftText);
        btn_cancel.setText(mBtnRightText);
        btn_single.setText(mBtnLeftText);
    }

    public void setOnLeftClickListener(String txt, MessageDialogClickListener listener) {
        mOnOkClickListener = listener;
        if(txt != null) {
            mBtnLeftText = txt;
        }
    }

    public void setTitle(String title){
        this.mTitle = title;
    }

    public void setTitle(int strId){
        this.mTitle = mContext.getString(strId);
    }

    public void setMessage(String message){
        this.mMsg = message;
    }

    public void setMessage(int strId){
        this.mMsg = mContext.getString(strId);
    }

    public Button getBtnCancel(){
    	return btn_cancel;
    }
    public void setOnRightClickListener(String txt, MessageDialogClickListener listener){
        mCancelListener = listener;
        if(txt != null){
            mBtnRightText = txt;
        }
    }
    
    public void setIsDouleBtn(boolean mIsDouleBtn) {
    	this.mIsDouleBtn = mIsDouleBtn;
    }

    @Override
    public void show() {
        super.show();
        getWindow().setAttributes(getWinParams());
    }

    private WindowManager.LayoutParams getWinParams(){
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int mScreenWidth = manager.getDefaultDisplay().getWidth();
        int mScreenHeight = manager.getDefaultDisplay().getHeight();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        int width = (int)(mScreenWidth * 80 / 100);
        int height = (int)(mScreenHeight * 25 / 100);
        switch (mSize){
            case SIZE_LARGE:
                width = mScreenWidth * 85 /100;
                height = mScreenHeight * 30 /100;
                break;
            case SIZE_NORMAL:
                break;
            case SIZE_SMALL:
                width = mScreenWidth * 75 /100;
                height = mScreenHeight * 20 /100;
                break;
            default:

                break;
        }
        params.width = width;
        //params.height = height;
        return params;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.buttonOK) {
            if (mOnOkClickListener != null) {
                mOnOkClickListener.onclick(this);
            } else {
                dismiss();
            }

        } else if (i == R.id.btnCancel) {
            if (mCancelListener != null) {
                mCancelListener.onclick(this);
            } else {
                dismiss();
            }

        } else if (i == R.id.btnOK_single) {
            if (mOnOkClickListener != null) {
                mOnOkClickListener.onclick(this);
            } else {
                dismiss();
            }

        }
    }
}
