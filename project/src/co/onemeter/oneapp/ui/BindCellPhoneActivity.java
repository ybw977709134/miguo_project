package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.Timer;
import java.util.TimerTask;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 绑定手机号码
 * Created by hutianfeng on 15-8-3.
 */
public class BindCellPhoneActivity extends Activity implements View.OnClickListener {


    private static int BIND_cellphone_PAGE = 1;//验证绑定手机号码阶段
    private static int AUTH_CODE_PAGE = 2;//验证验证码阶段
    //验证阶段判断标志
    private int pageFlag = BIND_cellphone_PAGE;//默认为验证绑定手机号码阶段

    private RelativeLayout layout_bind_cellphone;

    private TextView textView_bindcellphone_back;//文本返回
    private TextView textView_findPassword_cancel;//取消
    private ImageButton title_back;//箭头返回

    //验证手机号码

    private RelativeLayout layout_verification_cellphone;
    private EditText txt_bind_cellphone;//绑定手机号码
    private ImageButton field_clear_cellphone;//清除绑定手机号码按钮图片
    private TextView textView_verification_cellphone_result;//手机号码格式验证的结果
    private Button btn_verification_cellphone;//确定验证绑定手机号码信息

    //验证验证码

    private RelativeLayout layout_verification_auth_code;
    private TextView textView_show_bind_cellphone;//显示验证后绑定的手机号码
    private EditText txt_auth_code;//手机号码收到的验证码
    private ImageButton field_clear_auth_code;//清除验证码的按钮图片
    private TextView textView_verification_authCode_result;//验证码验证结果
    private Button btn_verification_auth_code;//确认验证验证码
    private Button btn_again_receive_auth_code;//重新获取验证码


    InputMethodManager mInputMethodManager ;
    private MessageBox mMsgBox;
    private int time = 60;
    private Timer mTimer;


    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                btn_again_receive_auth_code.setText("重新获取验证码");
                btn_again_receive_auth_code.setEnabled(true);
                btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue_10));
                mTimer.cancel();
            } else {
                btn_again_receive_auth_code.setText("重新获取验证码" + "("+ time + "s" + ")");
            }
        };
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_cellphone);
        initView();
        mMsgBox = new MessageBox(this);

        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        txt_bind_cellphone.setFocusable(true);
        txt_bind_cellphone.setFocusableInTouchMode(true);
        txt_bind_cellphone.requestFocus();

        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {

            @Override
            public void run() {
                mInputMethodManager.showSoftInput(txt_bind_cellphone, InputMethodManager.RESULT_SHOWN);
                mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }, 200);

    }


    /**
     * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            closeSoftKeyboard();
        }
        return super.onTouchEvent(event);
    }

    //如何在绑定手机号码的过程中，任何时候按物理的返回键都绑定不成功，即使前面绑定了，还需要解绑
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (pageFlag == BIND_cellphone_PAGE) {//绑定手机号码起始页

                if (getIntent().getBooleanExtra(FixBindCellPhoneActivity.FIX_BIND_CELLPHONE, false)) {
                    Intent intent = new Intent(BindCellPhoneActivity.this,AccountSettingActivity.class);
                    startActivity(intent);
                }

                closeSoftKeyboard();
                BindCellPhoneActivity.this.finish();
            } else {
                pageFlag = BIND_cellphone_PAGE;

                unBindcellphoneAddress();//解绑
                if (mTimer != null) {
                    mTimer.cancel();
                }
                layout_verification_auth_code.setVisibility(View.GONE);
                layout_verification_cellphone.setVisibility(View.VISIBLE);
                txt_auth_code.setText("");

                txt_bind_cellphone.setText("");
                textView_verification_cellphone_result.setVisibility(View.GONE);
                textView_verification_authCode_result.setVisibility(View.GONE);
            }

        }

        return super.onKeyDown(keyCode, event);
    }


    private void closeSoftKeyboard() {

        if (txt_bind_cellphone.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_bind_cellphone.getWindowToken() , 0);
            Log.i("---txt_bind_cellphone");
        }

        if (txt_auth_code.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
            Log.i("---txt_auth_code");
        }

    }

    @Override
    protected void onPause() {
        closeSoftKeyboard();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        super.onDestroy();
    }

    /**
     * 初始化控件
     */
    private void initView() {

        //点击布局的空白处使得edittext失去说有的焦点和光标
        layout_bind_cellphone = (RelativeLayout) findViewById(R.id.layout_bind_cellphone);
        layout_bind_cellphone.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout_bind_cellphone.setFocusable(true);
                layout_bind_cellphone.setFocusableInTouchMode(true);
                layout_bind_cellphone.requestFocus();
                field_clear_cellphone.setVisibility(View.GONE);
                field_clear_auth_code.setVisibility(View.GONE);
                return false;
            }
        });

        //页标题
        textView_bindcellphone_back = (TextView) findViewById(R.id.textView_bindcellphone_back);
        textView_findPassword_cancel = (TextView) findViewById(R.id.textView_findPassword_cancel);
        title_back = (ImageButton) findViewById(R.id.title_back);

        //验证手机号码
        layout_verification_cellphone = (RelativeLayout) findViewById(R.id.layout_verification_cellphone);
        txt_bind_cellphone = (EditText) findViewById(R.id.txt_bind_cellphone);
        field_clear_cellphone = (ImageButton) findViewById(R.id.field_clear_cellphone);
        textView_verification_cellphone_result = (TextView) findViewById(R.id.textView_verification_cellphone_result);
        btn_verification_cellphone = (Button) findViewById(R.id.btn_verification_cellphone);

        //初始化绑定手机号码按钮的状态
        btn_verification_cellphone.setTextColor(getResources().getColor(R.color.white_40));
        btn_verification_cellphone.setEnabled(false);


        //验证验证码
        layout_verification_auth_code = (RelativeLayout) findViewById(R.id.layout_verification_auth_code);
        textView_show_bind_cellphone = (TextView) findViewById(R.id.textView_show_bind_cellphone);
        txt_auth_code = (EditText) findViewById(R.id.txt_auth_code);
        field_clear_auth_code = (ImageButton) findViewById(R.id.field_clear_auth_code);
        textView_verification_authCode_result = (TextView) findViewById(R.id.textView_verification_authCode_result);
        btn_verification_auth_code = (Button) findViewById(R.id.btn_verification_auth_code);
        btn_verification_auth_code.setEnabled(false);
        btn_again_receive_auth_code = (Button) findViewById(R.id.btn_again_receive_auth_code);

        //对各个控件设置监听事件

        //输入绑定手机号码时，清除图片按钮的控制
        txt_bind_cellphone.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    field_clear_cellphone.setVisibility(View.GONE);
                    btn_verification_cellphone.setTextColor(getResources().getColor(R.color.white_40));
                    btn_verification_cellphone.setEnabled(false);
                } else {
                    field_clear_cellphone.setVisibility(View.VISIBLE);
                    btn_verification_cellphone.setTextColor(getResources().getColor(R.color.white));
                    btn_verification_cellphone.setEnabled(true);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        txt_bind_cellphone.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
//					txt_bind_cellphone.setText("");
                    if (txt_bind_cellphone.getText().toString().length() > 0) {
                        field_clear_cellphone.setVisibility(View.VISIBLE);
                    }

                } else {
                    field_clear_cellphone.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_bind_cellphone.getWindowToken() , 0);
                }

            }
        });

        //输入验证码时，清除图片按钮的控制
        txt_auth_code.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    field_clear_auth_code.setVisibility(View.GONE);
                    btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white_40));
                    btn_verification_auth_code.setEnabled(false);

                } else {
                    field_clear_auth_code.setVisibility(View.VISIBLE);
                    btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white));
                    btn_verification_auth_code.setEnabled(true);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        txt_auth_code.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
//					txt_auth_code.setText("");
                    if (txt_auth_code.getText().toString().length() > 0) {
                        field_clear_auth_code.setVisibility(View.VISIBLE);
//						 Handler hanlder = new Handler();
//	        		        hanlder.postDelayed(new Runnable() {
//
//	        					@Override
//	        					public void run() {
//	        						mInputMethodManager.showSoftInput(txt_auth_code, InputMethodManager.RESULT_SHOWN);
//	        						mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
//	        					}
//	        				}, 200);
                    }
                } else {
                    field_clear_auth_code.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
                }

            }
        });

        textView_bindcellphone_back.setOnClickListener(this);
        textView_findPassword_cancel.setOnClickListener(this);
        title_back.setOnClickListener(this);
        field_clear_cellphone.setOnClickListener(this);
        btn_verification_cellphone.setOnClickListener(this);
        field_clear_auth_code.setOnClickListener(this);
        btn_verification_auth_code.setOnClickListener(this);
        btn_again_receive_auth_code.setOnClickListener(this);

    }

    /**
     * 处理各个控件的监听事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //清除绑定手机号码中文本框中的内容
            case R.id.field_clear_cellphone:
                txt_bind_cellphone.setText("");
                textView_verification_cellphone_result.setVisibility(View.GONE);
                break;
            //清除验证码文本框中的内容
            case R.id.field_clear_auth_code:
                txt_auth_code.setText("");
                textView_verification_authCode_result.setVisibility(View.GONE);

                break;
            //返回
            case R.id.textView_bindcellphone_back:
            case R.id.title_back:
                if (pageFlag == BIND_cellphone_PAGE) {
                    if (getIntent().getBooleanExtra(FixBindCellPhoneActivity.FIX_BIND_CELLPHONE, false)) {
                        Intent intent = new Intent(BindCellPhoneActivity.this,AccountSettingActivity.class);
                        startActivity(intent);
                    }
                    closeSoftKeyboard();
                    BindCellPhoneActivity.this.finish();
                } else {
                    pageFlag = BIND_cellphone_PAGE;

                    unBindcellphoneAddress();//解绑
                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                    layout_verification_auth_code.setVisibility(View.GONE);
                    layout_verification_cellphone.setVisibility(View.VISIBLE);
                    txt_auth_code.setText("");
                    txt_bind_cellphone.setText("");
                    textView_verification_cellphone_result.setVisibility(View.GONE);
                    textView_verification_authCode_result.setVisibility(View.GONE);
                }
                break;

            //取消
            case R.id.textView_findPassword_cancel:

                MessageDialog dialog = new MessageDialog(BindCellPhoneActivity.this);
                dialog.setMessage("你确定要取消绑定手机号码吗？");
                dialog.setCancelable(false);
                dialog.setRightBold(true);
                dialog.setOnLeftClickListener("继续绑定", null);
                dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        unBindcellphoneAddress();
                        Intent intent = new Intent(BindCellPhoneActivity.this,AccountSettingActivity.class);
                        startActivity(intent);
                        BindCellPhoneActivity.this.finish();
                    }
                });
                dialog.show();
                break;

            //验证绑定手机号码
            case R.id.btn_verification_cellphone:
                mInputMethodManager.hideSoftInputFromWindow(txt_bind_cellphone.getWindowToken() , 0);
                bindcellphoneAddress(txt_bind_cellphone.getText().toString());

                Handler hanlder = new Handler();
                hanlder.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mInputMethodManager.showSoftInput(txt_auth_code, InputMethodManager.RESULT_SHOWN);
                        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                }, 200);

                break;

            //验证验证码
            case R.id.btn_verification_auth_code:
                mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
                verifyBindcellphoneAddress(txt_auth_code.getText().toString(),txt_bind_cellphone.getText().toString());
                break;

            //重新获得验证码
            case R.id.btn_again_receive_auth_code:
                bindcellphoneAddress(txt_bind_cellphone.getText().toString());
                break;

            default:
                break;
        }

    }

    /**
     *
     * 绑定手机号码地址
     * @param cellphoneAddress
     * @author hutianfeng
     * @date 2015/3/4
     */
    private void bindcellphoneAddress(final String cellphoneAddress) {
        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(BindCellPhoneActivity.this).fBindEmialAddress(cellphoneAddress);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0
                        pageFlag = AUTH_CODE_PAGE;
                        layout_verification_auth_code.setVisibility(View.VISIBLE);
                        layout_verification_cellphone.setVisibility(View.GONE);
                        textView_show_bind_cellphone.setVisibility(View.VISIBLE);
                        textView_show_bind_cellphone.setText("你输入的手机号码" + txt_bind_cellphone.getText().toString());


                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        stopGetAccessCode();
                        break;

//                    case ErrorCode.cellphone_ADDRESS_VERIFICATION_CODE_ERROR://18:手机号码格式错误
//                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
//                        textView_verification_cellphone_result.setText(getString(R.string.bind_cellphone_format_error_msg));
//                        break;
//
//                    case ErrorCode.AUTH://2:认证失败，错误的用户名或密码
//                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
//                        textView_verification_cellphone_result.setText(getString(R.string.bind_cellphone_pwd_err));
//                        break;
//
//                    case ErrorCode.ACCESS_CODE_ERROR_OVER://24:验证码一天最多只能验证5次
//                        MessageDialog dialog = new MessageDialog(BindCellPhoneActivity.this, false, MessageDialog.SIZE_NORMAL);
//                        dialog.setTitle("");
//                        dialog.setCancelable(false);
//                        dialog.setMessage("今天手机号码验证次数已用完" + "\n" + "请明天再试。");
//                        dialog.show();
//                        break;
//
//                    case ErrorCode.cellphone_USED_BY_OTHERS://28：该手机号码已被其他用户绑定
//                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
//                        textView_verification_cellphone_result.setText(getString(R.string.settings_account_cellphone_used_by_others));
//                        break;
//
//                    case ErrorCode.ERR_OPERATION_DENIED://37:该用户已绑定其它手机号码
//                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
//                        textView_verification_cellphone_result.setText(getString(R.string.settings_account_cellphone_used));
//                        break;
//
//                    default://手机号码绑定失败
//                        mMsgBox.showWaitImageWorng(getString(R.string.bind_cellphone_failed));
//                        break;
                }

            }
        });

    }

    /**
     * 控制重新获取验证的按钮状态
     * @author hutainfeng
     * @date 2015/3/4
     *
     */
    private void stopGetAccessCode() {
        //60秒内不可点击  重新获取验证码
        btn_again_receive_auth_code.setEnabled(false);
        btn_again_receive_auth_code.setTextColor(getResources().getColor(R.color.blue_11));
        time = 60;

        mTimer = new Timer();

        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mHandler.sendEmptyMessage(time--);
            }
        }, 0, 1000);

//		mTimer.schedule(mTimerTask, 0, 1000);

    }

    /**
     * 验证绑定手机号码收到的验证码
     * @param access_code
     * @param cellphoneAddress
     * @author hutainfeng
     * @date 2015/3/4
     */
    private void verifyBindcellphoneAddress(final String access_code,final String cellphoneAddress) {
        mMsgBox.showWaitProgressbar("验证中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
//                return WowTalkWebServerIF.getInstance(BindCellPhoneActivity.this).fVerifycellphoneAddress(access_code,cellphoneAddress);
                return 1;
            }


            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0

                        mMsgBox.showWaitImageSuccess("手机号码绑定成功");
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                BindCellPhoneActivity.this.finish();

                            }
                        }).start();


                        break;

                    case ErrorCode.ACCESS_CODE_ERROR://22:无效的验证码，验证码有有效期，目前是一天的有效期
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText(getString(R.string.access_code_error));
                        break;

                    default:
//                        mMsgBox.showWaitImageWorng( getString(R.string.bind_cellphone_failed));
                        break;
                }
            }
        });
    }


    /**
     * 解除绑定手机号码
     * @author hutianfeng
     * @date 2015/3/5
     */
    private void unBindcellphoneAddress() {

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
//                return WowTalkWebServerIF.getInstance(BindCellPhoneActivity.this).fUnBindcellphoneAddress();
                return 1;
            }

            @Override
            protected void onPostExecute(Integer result) {

                switch (result) {
                    case ErrorCode.OK://0
//                    	Toast.makeText(BindCellPhoneActivity.this, "没有绑定手机号码", Toast.LENGTH_SHORT).show();
//                    	mMsgBox.show(null, "绑定手机号码失败");
                        break;

                    default:
                        Toast.makeText(BindCellPhoneActivity.this, "连接服务器失败,请检查网络", Toast.LENGTH_SHORT).show();
//                        mMsgBox.show(null, "请检查网络");
                        break;
                }
            }
        });
    }

}