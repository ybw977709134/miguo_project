package org.wowtalk.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
    
    boolean leftFlag = false;
    boolean rightFlag = false;
    boolean singleFlag = false;

    private int mTitleColor = -1;
    private int mMsgColor = -1;
    private int mBtn_l_color = -1;
    private int mBtn_r_color = -1;

    private int mMessageTxtSize = -1;

    private boolean mWith2Buttons = false;

    private int mSize = SIZE_NORMAL;

    /**
     * this constractor create default duoble button
     * @param context
     */
    public MessageDialog(Context context){
        this(context,0, true, SIZE_NORMAL);
    }

    /**
     * @param context
     * @param with2Buttons with 2 or 1 button(s)?
     */
    public MessageDialog(Context context, boolean with2Buttons, int size){
        this(context,0, with2Buttons,size);
    }

    protected MessageDialog(Context context, int theme, boolean with2Buttons, int size) {
        super(context, theme);
        mContext = context;
        mWith2Buttons = with2Buttons;
        mSize = size;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lay_messagedialog);
        if (mWith2Buttons){
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
        if(mTitleColor != -1){
            android.util.Log.i("--color", "" + mTitleColor);
            txt_title.setTextColor(mTitleColor);
        }
        
        TextView txt_msg = (TextView) findViewById(R.id.txt_msg);
        txt_msg.setMovementMethod(ScrollingMovementMethod.getInstance());
        
        if (!mWith2Buttons){
             txt_msg.setGravity(Gravity.CENTER);
        }
        if(mTitle.length() == 0){
        	txt_title.setVisibility(View.GONE);
            txt_msg.setGravity(Gravity.CENTER);
        }
        txt_msg.setText(mMsg);
        if(mMsgColor != -1){
            txt_msg.setTextColor(mMsgColor);
        }
        
        if (leftFlag) {
        	btn_ok.getPaint().setFakeBoldText(true);
        }
        
        if (rightFlag) {
        	btn_cancel.getPaint().setFakeBoldText(true);
        }
        
        if (singleFlag) {
        	btn_single.getPaint().setFakeBoldText(true);
        }

        btn_ok.setText(mBtnLeftText);
        btn_cancel.setText(mBtnRightText);
        btn_single.setText(mBtnLeftText);

        if(mBtn_l_color != -1){
            btn_ok.setTextColor(mBtn_l_color);
            btn_single.setTextColor(mBtn_l_color);
        }
        if(mBtn_r_color != -1){
            btn_cancel.setTextColor(mBtn_r_color);
        }

        if(mMessageTxtSize != -1){
            txt_msg.setTextSize(mMessageTxtSize);
        }
    }

    public MessageDialog setOnLeftClickListener(String txt, MessageDialogClickListener listener) {
        mOnOkClickListener = listener;
        if(txt != null) {
            mBtnLeftText = txt;
        }
        return this;
    }
    
    //设置粗体、颜色、大小
    public void setLeftBold(boolean flag){
    	leftFlag = flag;
    }
    
    public void setRightBold(boolean flag){
    	rightFlag = flag;
    }
    
    public void setSingleBold(boolean flag){
    	singleFlag = flag;
    }

    public void setTitle(String title){
        this.mTitle = title;
    }

    public void setTitleColor(int color){
        mTitleColor = color;
    }

    public void setMessageColor(int color){
        mMsgColor = color;
    }

    public void setTextColorBtnLeftOrSingle(int color){
        mBtn_l_color = color;
    }

    public void setTextColorBtnRight(int color){
        mBtn_r_color = color;
    }

    public void setMessageTextSize(int size){
        mMessageTxtSize = size;
    }

    public void setTitle(int strId){
        this.mTitle = mContext.getString(strId);
    }

    public MessageDialog setMessage(String message){
        this.mMsg = message;
        return this;
    }

    public MessageDialog setMessage(int strId){
        this.mMsg = mContext.getString(strId);
        return this;
    }

    public Button getBtnCancel(){
    	return btn_cancel;
    }
    public MessageDialog setOnRightClickListener(String txt, MessageDialogClickListener listener){
        mCancelListener = listener;
        if(txt != null){
            mBtnRightText = txt;
        }
        return this;
    }
    
    public void setIsDouleBtn(boolean mIsDouleBtn) {
    	this.mWith2Buttons = mIsDouleBtn;
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
