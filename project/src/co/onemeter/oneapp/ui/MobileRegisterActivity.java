package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.Timer;
import java.util.TimerTask;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 *通过手机号找回密码
 * Created by hanson on 15-8-3.
 */
public class MobileRegisterActivity extends Activity implements View.OnClickListener {


    private RelativeLayout layout_find_password;

    private ImageButton title_back;
    private TextView textView_findPassword_back;//返回
    private TextView textView_findPassword_cancel;//取消

    //验证账号和手机号码

    private RelativeLayout layout_verification_cellphone;
    private EditText txt_bind_account;//输入账号
    private ImageButton field_clear_account;//清除账号
    private EditText txt_bind_cellphone;//输入绑定手机号码
    private ImageButton field_clear_cellphone;//清除手机号码
    private TextView textView_verification_cellphone_result;//账号和手机号码的验证结果
    private Button btn_verification_cellphone;//确认找回密码按钮


    //验证验证码

    private RelativeLayout layout_verification_auth_code;
    private TextView textView_show_bind_cellphone;//显示验证后绑定的手机号码
    private EditText txt_auth_code;//手机号码收到的验证码
    private ImageButton field_clear_auth_code;//清除验证码的按钮图片
    private TextView textView_verification_authCode_result;//验证码验证结果
    private Button btn_verification_auth_code;//确认验证验证码
    private Button btn_again_receive_auth_code;//重新获取验证码


    //重置新密码

    private RelativeLayout layout_reset_password;
    private EditText txt_new_password;//新密码
    private EditText txt_confirm_new_password;//确认新密码
    private ImageView field_clear_pwd;//清除密码
    private ImageView field_clear_confirm;//清除确认密码
    private ImageView imageview_show_password;//显示密码
    private ImageView imageview_hint_password;//隐藏密码
    private TextView textView_isshow_password;//显示密码文本提示
    private TextView textView_verification_newPassword;//验证新设置的密码两次设置是否一致
    private Button btn_newPassWord_ok;//提交重新设置后的密码


    InputMethodManager mInputMethodManager ;
    private MessageBox mMsgBox;
    private int time;
    private Timer mTimer;
    private int codeFlag = 1;//1-代表第一次获取验证码，2-代表再次获取验证码

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



    private static int BIND_cellphone_PAGE = 1;//验证绑定手机号码阶段
    private static int AUTH_CODE_PAGE = 2;//验证验证码阶段
    //验证阶段判断标志
    private int pageFlag = BIND_cellphone_PAGE;//默认为验证绑定手机号码阶段


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_mobile);
        mMsgBox = new MessageBox(this);
        initView();

        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        txt_bind_account.setFocusable(true);
        txt_bind_account.setFocusableInTouchMode(true);
        txt_bind_account.requestFocus();

        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {

            @Override
            public void run() {
                mInputMethodManager.showSoftInput(txt_bind_account, InputMethodManager.RESULT_SHOWN);
                mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }, 200);
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

        if (mInputMethodManager != null) {
            mInputMethodManager = null;
        }
        super.onDestroy();
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


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (pageFlag == BIND_cellphone_PAGE) {

                closeSoftKeyboard();
                MobileRegisterActivity.this.finish();

            } else {

                MessageDialog dialog = new MessageDialog(MobileRegisterActivity.this);
                dialog.setTitle("提示");
                dialog.setRightBold(true);
                dialog.setCancelable(false);
                dialog.setMessage("别急着走哦，发送验证码可能需要一些时间，请耐心稍等，留在该页还是确定返回");
                dialog.setOnLeftClickListener("返回上页",new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();

                        pageFlag = BIND_cellphone_PAGE;

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
                });

                dialog.setOnRightClickListener("继续等待",null);
                dialog.show();
            }

        }

        return super.onKeyDown(keyCode, event);
    }

    private void closeSoftKeyboard() {
        if (txt_bind_cellphone.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_bind_cellphone.getWindowToken() , 0);
            Log.i("---find_txt_bind_cellphone");
        }

        if (txt_bind_account.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_bind_account.getWindowToken() , 0);
            Log.i("---find_txt_bind_account");
        }

        if (txt_auth_code.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
            Log.i("---find_txt_auth_code");
        }

        if (txt_new_password.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_new_password.getWindowToken() , 0);
            Log.i("---find txt_new_password");
        }

        if (txt_confirm_new_password.hasFocus()) {
            mInputMethodManager.hideSoftInputFromWindow(txt_confirm_new_password.getWindowToken() , 0);
            Log.i("---find txt_confirm_new_password");
        }

    }

    private void initView() {

        layout_find_password = (RelativeLayout) findViewById(R.id.layout_find_password);

        //点击布局时，使得布局获得事件的焦点
        layout_find_password.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout_find_password.setFocusable(true);
                layout_find_password.setFocusableInTouchMode(true);
                layout_find_password.requestFocus();
                return false;
            }
        });

        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_findPassword_back = (TextView) findViewById(R.id.textView_findPassword_back);
        textView_findPassword_cancel = (TextView) findViewById(R.id.textView_findPassword_cancel);

        //验证账号和手机号码
        layout_verification_cellphone = (RelativeLayout) findViewById(R.id.layout_verification_cellphone);
        txt_bind_account = (EditText) findViewById(R.id.txt_bind_account);
        field_clear_account = (ImageButton) findViewById(R.id.field_clear_account);
        txt_bind_cellphone = (EditText) findViewById(R.id.txt_bind_cellphone);
        field_clear_cellphone = (ImageButton) findViewById(R.id.field_clear_cellphone);
        textView_verification_cellphone_result = (TextView) findViewById(R.id.textView_verification_cellphone_result);
        btn_verification_cellphone = (Button) findViewById(R.id.btn_verification_cellphone);


        //验证验证码
        layout_verification_auth_code = (RelativeLayout) findViewById(R.id.layout_verification_auth_code);
        textView_show_bind_cellphone = (TextView) findViewById(R.id.textView_show_bind_cellphone);
        txt_auth_code = (EditText) findViewById(R.id.txt_auth_code);
        field_clear_auth_code = (ImageButton) findViewById(R.id.field_clear_auth_code);
        textView_verification_authCode_result = (TextView) findViewById(R.id.textView_verification_authCode_result);
        btn_verification_auth_code = (Button) findViewById(R.id.btn_verification_auth_code);
        btn_again_receive_auth_code = (Button) findViewById(R.id.btn_again_receive_auth_code);

        //重置新密码
        layout_reset_password = (RelativeLayout) findViewById(R.id.layout_reset_password);
        txt_new_password =  (EditText) findViewById(R.id.txt_new_password);
        txt_confirm_new_password =  (EditText) findViewById(R.id.txt_confirm_new_password);
        field_clear_pwd = (ImageView) findViewById(R.id.field_clear_pwd);
        field_clear_confirm = (ImageView) findViewById(R.id.field_clear_confirm);

        imageview_show_password = (ImageView) findViewById(R.id.imageview_show_password);
        imageview_hint_password = (ImageView) findViewById(R.id.imageview_hint_password);
        textView_isshow_password = (TextView) findViewById(R.id.textView_isshow_password);
        textView_verification_newPassword = (TextView) findViewById(R.id.textView_verification_newPassword);
        btn_newPassWord_ok = (Button) findViewById(R.id.btn_newPassWord_ok);
        field_clear_account.setOnClickListener(this);
        field_clear_auth_code.setOnClickListener(this);
        btn_verification_auth_code.setOnClickListener(this);
        btn_again_receive_auth_code.setOnClickListener(this);
        field_clear_pwd.setOnClickListener(this);
        field_clear_confirm.setOnClickListener(this);
        imageview_show_password.setOnClickListener(this);
        imageview_hint_password.setOnClickListener(this);
        btn_newPassWord_ok.setOnClickListener(this);


        //对各个控件设置监听事件

        //输入账号时，清除图片按钮的控制
        txt_bind_account.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    field_clear_account.setVisibility(View.GONE);
                    btn_verification_cellphone.setTextColor(getResources().getColor(R.color.white_40));
                    btn_verification_cellphone.setEnabled(false);
                } else {
                    field_clear_account.setVisibility(View.VISIBLE);
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

        txt_bind_account.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
//					txt_bind_account.setText("");
                    if (txt_bind_account.getText().toString().length() > 0 ) {
                        field_clear_account.setVisibility(View.VISIBLE);
                    }
                } else {
                    field_clear_account.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_bind_account.getWindowToken() , 0);
                }

            }
        });

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
                    if (txt_bind_cellphone.getText().toString().length() > 0 ) {
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
                    if (txt_auth_code.getText().toString().length() > 0 ) {
                        field_clear_auth_code.setVisibility(View.VISIBLE);
                    }
                } else {
                    field_clear_auth_code.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_auth_code.getWindowToken() , 0);
                }

            }
        });


        txt_new_password.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    field_clear_pwd.setVisibility(View.GONE);

                } else {
                    field_clear_pwd.setVisibility(View.VISIBLE);
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


        txt_new_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    if (txt_new_password.getText().toString().length() > 0 ) {
                        field_clear_pwd.setVisibility(View.VISIBLE);
                    }
                } else {
                    field_clear_pwd.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_new_password.getWindowToken() , 0);
                }

            }
        });



        txt_confirm_new_password.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    field_clear_confirm.setVisibility(View.GONE);

                } else {
                    field_clear_confirm.setVisibility(View.VISIBLE);

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


        txt_confirm_new_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    if (txt_confirm_new_password.getText().toString().length() > 0 ) {
                        field_clear_confirm.setVisibility(View.VISIBLE);
                    }
                } else {
                    field_clear_confirm.setVisibility(View.GONE);
                    mInputMethodManager.hideSoftInputFromWindow(txt_confirm_new_password.getWindowToken() , 0);
                }

            }
        });


        title_back.setOnClickListener(this);
        textView_findPassword_back.setOnClickListener(this);
        textView_findPassword_cancel.setOnClickListener(this);
        field_clear_cellphone.setOnClickListener(this);
        btn_verification_cellphone.setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //清空账号输入框中的内容
            case R.id.field_clear_account:
                txt_bind_account.setText("");
                break;
            //清除绑定手机号码中文本框中的内容
            case R.id.field_clear_cellphone:
                txt_bind_cellphone.setText("");
                textView_verification_cellphone_result.setVisibility(View.GONE);
                btn_verification_cellphone.setTextColor(getResources().getColor(R.color.white_40));
                btn_verification_cellphone.setEnabled(false);
                break;
            //清除验证码文本框中的内容	
            case R.id.field_clear_auth_code:
                txt_auth_code.setText("");
                textView_verification_authCode_result.setVisibility(View.GONE);
                btn_verification_auth_code.setTextColor(getResources().getColor(R.color.white_40));
                btn_verification_auth_code.setEnabled(false);
                break;
            case R.id.title_back:
            case R.id.textView_findPassword_back:

                if (pageFlag == BIND_cellphone_PAGE) {

                    closeSoftKeyboard();
                    MobileRegisterActivity.this.finish();

                } else {

                    MessageDialog dialog = new MessageDialog(MobileRegisterActivity.this);
                    dialog.setTitle("提示");
                    dialog.setRightBold(true);
                    dialog.setCancelable(false);
                    dialog.setMessage("别急着走哦，发送验证码可能需要一些时间，请耐心稍等，留在该页还是确定返回");
                    dialog.setOnLeftClickListener("返回上页",new MessageDialog.MessageDialogClickListener() {
                        @Override
                        public void onclick(MessageDialog dialog) {
                            dialog.dismiss();

                            pageFlag = BIND_cellphone_PAGE;

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
                    });

                    dialog.setOnRightClickListener("继续等待",null);
                    dialog.show();
                }

                break;
            case R.id.textView_findPassword_cancel:
                break;

            case R.id.btn_verification_cellphone://验证手机号码
                codeFlag = 1;

                if (isPhoneNum(txt_bind_cellphone.getText().toString())) {
                    getAccessCode(txt_bind_cellphone.getText().toString());

                } else {
                    textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                    textView_verification_cellphone_result.setText("你填写的手机号码格式不正确");
                }

                break;

            //验证验证码	
            case R.id.btn_verification_auth_code:

                if (txt_auth_code.getText().toString().length() == 4) {

                    checkSMS(txt_bind_cellphone.getText().toString(),txt_auth_code.getText().toString());

                } else {
                    textView_verification_authCode_result.setVisibility(View.VISIBLE);
                    textView_verification_authCode_result.setText("验证码不正确");
                }

                break;

            //重新获得验证码	
            case R.id.btn_again_receive_auth_code:
                codeFlag = 2;
                getAccessCode(txt_bind_cellphone.getText().toString());

                break;

            case R.id.field_clear_pwd:
                txt_new_password.setText("");
                break;

            case R.id.field_clear_confirm:
                txt_confirm_new_password.setText("");
                break;

            //显示密码
            case R.id.imageview_show_password:
                imageview_show_password.setVisibility(View.GONE);
                imageview_hint_password.setVisibility(View.VISIBLE);
                txt_new_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                txt_confirm_new_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                textView_isshow_password.setText("隐藏密码");
                break;

            //隐藏密码	
            case R.id.imageview_hint_password:
                imageview_show_password.setVisibility(View.VISIBLE);
                imageview_hint_password.setVisibility(View.GONE);
                txt_new_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                txt_confirm_new_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                textView_isshow_password.setText("显示密码");
                break;

            //重置后的密码
            case R.id.btn_newPassWord_ok:
                if (txt_new_password.getText().toString().length() < 6) {
                    textView_verification_newPassword.setVisibility(View.VISIBLE);
                    textView_verification_newPassword.setText("密码至少6位");

                    break;
                } else  if (txt_new_password.getText().toString().length() > 20) {
                    textView_verification_newPassword.setVisibility(View.VISIBLE);
                    textView_verification_newPassword.setText("密码最多20位");
                    break;
                } else  if (txt_confirm_new_password.getText().toString().length() < 6) {
                    textView_verification_newPassword.setVisibility(View.VISIBLE);
                    textView_verification_newPassword.setText("确认密码至少6位");
                    break;
                } else  if (txt_confirm_new_password.getText().toString().length() > 20) {
                    textView_verification_newPassword.setVisibility(View.VISIBLE);
                    textView_verification_newPassword.setText("确认密码最多20位");

                    break;
                } else  if (!txt_new_password.getText().toString().equals(txt_confirm_new_password.getText().toString())) {
                    textView_verification_newPassword.setVisibility(View.VISIBLE);
                    textView_verification_newPassword.setText("密码和确认密码输入不一致");
                    break;
                }else {
                    resetPasswordByMobile(txt_bind_cellphone.getText().toString(),txt_new_password.getText().toString());
                    break;
                }


            default:
                break;
        }

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

    }



    /**
     * 获取验证码
     * @param cellPhone
     * created at 2015/8/10 by hutianfeng
     */

    private void getAccessCode (final String cellPhone) {
        mMsgBox.showWaitProgressbar("发送中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(MobileRegisterActivity.this).fCheckMobileExist(cellPhone);
            }


            @Override
            protected void onPostExecute(Integer result) {
//                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0//手机号码不存在数据库中，可以注册

                        //向手机发送短信验证码
                        sendSMS(cellPhone);

                        break;

                    case ErrorCode.USER_ALREADY_EXISTS://6:手机号码存在
                        mMsgBox.dismissWait();

                        if (codeFlag == 1) {
                            textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                            textView_verification_cellphone_result.setText("手机号码已被注册或绑定，请使用其他手机号注册");
                        } else {
                            textView_verification_authCode_result.setVisibility(View.VISIBLE);
                            textView_verification_authCode_result.setText("手机号码已被注册或绑定，请使用其他手机号注册");
                        }

                        break;

                    case ErrorCode.DB://3:数据库操作错误
                        mMsgBox.dismissWait();

                        if (codeFlag == 1) {
                            textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                            textView_verification_cellphone_result.setText("数据库操作错误");
                        } else {
                            textView_verification_authCode_result.setVisibility(View.VISIBLE);
                            textView_verification_authCode_result.setText("数据库操作错误");
                        }

                        break;

                    case ErrorCode.INVALID_ARGUMENT://1:传入的参数有误
                        mMsgBox.dismissWait();

                        if (codeFlag == 1) {
                            textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                            textView_verification_cellphone_result.setText("传入的参数有误");
                        } else {
                            textView_verification_authCode_result.setVisibility(View.VISIBLE);
                            textView_verification_authCode_result.setText("传入的参数有误");
                        }

                        break;

                    default:
                        mMsgBox.dismissWait();

                        if (codeFlag == 1) {
                            textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                            textView_verification_cellphone_result.setText("请检查网络连接");
                        } else {
                            textView_verification_authCode_result.setVisibility(View.VISIBLE);
                            textView_verification_authCode_result.setText("请检查网络连接");
                        }


                        break;
                }
            }
        });
    }


    /**
     * 发送验证码
     * @param cellPhone
     * created at 2015/8/10 by hutianfeng
     */

    private void sendSMS (final String cellPhone) {
//        mMsgBox.showWaitProgressbar("验证中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(MobileRegisterActivity.this).fSms_Send_SMS(cellPhone,"register");
            }


            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0//手机号码没有绑定

                        mMsgBox.toast("验证码以短信已经发送到你的手机，请注意接收");


                        //打开输入法
                        Handler hanlder = new Handler();
                        hanlder.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                mInputMethodManager.showSoftInput(txt_auth_code, InputMethodManager.RESULT_SHOWN);
                                mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                            }
                        }, 200);

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

                    case ErrorCode.ERR_VERIFICATION_CODE_NOT_SENT://53:短信接口报错
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("短信接口出错");
                        break;

                    case ErrorCode.ERR_VERIFICATION_CODE_TOO_MANY://54:短信验证码请求次数太多
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("短信验证码请求次数太多");
                        break;

                    case ErrorCode.ERR_SMS_MORE_TIMES://56:一天超过5次
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("一天超过5次");
                        break;

                    case ErrorCode.ERR_SMS_PHONE_NOT_CHECK://55:手机号码格式不正确
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("手机号码格式不正确");
                        break;

                    default:
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("请检查网络连接");
                        break;
                }
            }
        });
    }


    /**
     * 验证手机短信获得的验证码
     * @param cellPhone
     * created at 2015/8/10 by hutianfeng
     */

    private void checkSMS (final String cellPhone,final String code) {
        mMsgBox.showWaitProgressbar("验证中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(MobileRegisterActivity.this).fSms_Check_Code(cellPhone,code);
            }


            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {

                    case ErrorCode.OK://0//验证码验证成功

                        //跳转到注册密码输入页面

                       Intent registerIntent = new Intent(MobileRegisterActivity.this,RegisterActivity.class);
                       registerIntent.putExtra("cellPhone",cellPhone);

                       startActivity(registerIntent);


                        break;

                    case ErrorCode.ERR_SMS_PHONE_NOT_CHECK://55:手机号码格式不正确
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("手机号码格式不正确");
                        break;


                    case ErrorCode.ERR_SMS_CODE_OVER://57:验证码过期
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("验证码过期");
                        break;

                    case ErrorCode.ERR_SMS_CODE_NOT_CHECK://58:验证码验证不通过
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("验证码不正确");
                        break;

                    case ErrorCode.ERR_VERIFICATION_CODE_TOO_MANY://54:短信验证码请求次数太多
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("短信验证码请求次数太多");
                        break;


                    default:
                        textView_verification_authCode_result.setVisibility(View.VISIBLE);
                        textView_verification_authCode_result.setText("请检查网络连接");
                        break;
                }
            }
        });
    }



    /**
     * 重置密码
     * @param cellPhone
     * @param password
     * @date 2015/8/11
     */
    private void resetPasswordByMobile(final String cellPhone,final String password) {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(MobileRegisterActivity.this).fResetPasswordByMobile(cellPhone,password);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0
                        mMsgBox.showWaitImageSuccess("新密码设置成功");

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                finish();

                            }
                        }).start();

                        break;

                    case ErrorCode.FORGET_PWD_ACCESS_CODE_FALSE://6用户不存在
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("用户不存在");
                        break;

                    case ErrorCode.DB://3
                        textView_verification_cellphone_result.setVisibility(View.VISIBLE);
                        textView_verification_cellphone_result.setText("内部数据库错误");
                        break;


                    default://重置密码失败
                        mMsgBox.showWaitImageWorng("密码重置失败");
                        break;
                }
            }
        });
    }




    /**
     * 验证手机号码的格式是否正确
     * @author hutianfeng created at 2015/8/17
     * @param phNum
     * @return
     */
    private boolean isPhoneNum(String phNum){
        /*
    移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
    联通：130、131、132、152、155、156、185、186
    电信：133、153、180、189、（1349卫通）
    总结起来就是第一位必定为1，第二位必定为3或5或8，其他位置的可以为0-9
    */
        String telRegex = "[1][358]\\d{9}";//"[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(phNum)) {
            return false;
        } else {
            return phNum.matches(telRegex);
        }

    }


}


