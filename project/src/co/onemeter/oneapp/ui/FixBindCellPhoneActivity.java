package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 修改绑定手机号码
 * Created by hanson on 15-8-3.
 */
public class FixBindCellPhoneActivity extends Activity implements View.OnClickListener {


    private RelativeLayout layout_fix_bind_cellphone;
    InputMethodManager mInputMethodManager ;
    private ImageButton title_back;
    private TextView textView_fixBindcellphone_back;//返回

    private TextView textView_fixBindcellphone_cancel;//取消

    private TextView txt_access_code;//验证码
    private Button btn_access_code;//获取验证码按钮

    private TextView textView_verification_code_result;//验证结果

    private Button btn_verification_code;//确认

    public static final String FIX_BIND_CELLPHONE = "fix_bind_cellphone";//修改绑定手机页面的请求码

    private MessageBox mMsgBox;
    String bindcellphone= null;

    private int time = 60;
    private Timer mTimer;
    private static FixBindCellPhoneActivity instance;

    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                btn_access_code.setText("重新获取验证码");
                btn_access_code.setEnabled(true);
                btn_access_code.setTextColor(getResources().getColor(R.color.blue_12));
                btn_access_code.setBackground(getResources().getDrawable(R.drawable.btn_small_valid));
                mTimer.cancel();
            } else {
                btn_access_code.setText("重新发送验证码" + "("+ time + "s" + ")");
            }
        };
    };


    public static final FixBindCellPhoneActivity instance() {
        if (instance != null)
            return instance;
        return null;
    }

    public static final boolean isInstanciated() {
        return instance != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fix_bind_cellphone);

        instance = this;
        mMsgBox = new MessageBox(this);
        initView();

        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        txt_access_code.setFocusable(true);
        txt_access_code.setFocusableInTouchMode(true);
        txt_access_code.requestFocus();

        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {

            @Override
            public void run() {
                mInputMethodManager.showSoftInput(txt_access_code, InputMethodManager.RESULT_SHOWN);
                mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }, 200);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){
                closeSoftKeyboard();

            }
        }
        return super.onTouchEvent(event);
    }

    private void closeSoftKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(txt_access_code.getWindowToken() , 0);
        Log.i("---fix_txt_access_code");
    }


    /**
     * 初始化各个控件
     */
    private void initView() {
        layout_fix_bind_cellphone = (RelativeLayout) findViewById(R.id.layout_fix_bind_cellphone);

        layout_fix_bind_cellphone.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout_fix_bind_cellphone.setFocusable(true);
                layout_fix_bind_cellphone.setFocusableInTouchMode(true);
                layout_fix_bind_cellphone.requestFocus();
                return false;
            }
        });

        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_fixBindcellphone_back = (TextView) findViewById(R.id.textView_fixBindcellphone_back);
        textView_fixBindcellphone_cancel = (TextView) findViewById(R.id.textView_fixBindcellphone_cancel);

        txt_access_code = (TextView) findViewById(R.id.txt_access_code);

        //输入验证码时，清除图片按钮的控制
        txt_access_code.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {

                    btn_verification_code.setTextColor(getResources().getColor(R.color.white_40));
                    btn_verification_code.setEnabled(false);
                    btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_gray_medium_selector));
                } else {

                    btn_verification_code.setTextColor(getResources().getColor(R.color.white));
                    btn_verification_code.setEnabled(true);
                    btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_blue_medium_selector));
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

        btn_access_code = (Button) findViewById(R.id.btn_access_code);

        textView_verification_code_result = (TextView) findViewById(R.id.textView_verification_code_result);
        btn_verification_code = (Button) findViewById(R.id.btn_verification_code);
        btn_verification_code.setEnabled(false);
        btn_verification_code.setBackground(getResources().getDrawable(R.drawable.btn_gray_medium_selector));
        title_back.setOnClickListener(this);
        textView_fixBindcellphone_back.setOnClickListener(this);

        textView_fixBindcellphone_cancel.setOnClickListener(this);
        btn_access_code.setOnClickListener(this);
        btn_verification_code.setOnClickListener(this);
    }


    @Override
    protected void onPause() {

        if (txt_access_code.hasFocus()) {
            closeSoftKeyboard();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        if (instance != null) {
            instance = null;
        }
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
            case R.id.textView_fixBindcellphone_back:
                finish();
                break;

            case R.id.textView_fixBindcellphone_cancel:

                MessageDialog dialog = new MessageDialog(FixBindCellPhoneActivity.this);

                dialog.setMessage("你要取消修改绑定手机吗？");
                dialog.setOnLeftClickListener("继续修改", null);
                dialog.setCancelable(false);
                dialog.setRightBold(true);
                dialog.setOnRightClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        Intent intent = new Intent(FixBindCellPhoneActivity.this,AccountSettingActivity.class);
                        startActivity(intent);
                        FixBindCellPhoneActivity.this.finish();
                    }
                });
                dialog.show();

                break;

            case R.id.btn_access_code:
                textView_verification_code_result.setVisibility(View.GONE);
                sendSMS(PrefUtil.getInstance(FixBindCellPhoneActivity.this).getMyPhoneNumber());
                break;

            case R.id.btn_verification_code://确认，跳转到绑定手机界面

                if (txt_access_code.getText().toString().length() == 4) {

                    checkSMS(PrefUtil.getInstance(FixBindCellPhoneActivity.this).getMyPhoneNumber(),txt_access_code.getText().toString());

                } else {
                    textView_verification_code_result.setVisibility(View.VISIBLE);
                    textView_verification_code_result.setText("验证码不正确");
                }


                break;
            default:
                break;
        }

    }



    /**
     * 控制重新获取验证的按钮状态
     * @author hutainfeng
     * @date 2015/3/5
     *
     */
    private void stopGetAccessCode() {
        //60秒内不可点击  重新获取验证码
        btn_access_code.setEnabled(false);
        btn_access_code.setTextColor(getResources().getColor(R.color.gray_13));
        btn_access_code.setBackground(getResources().getDrawable(R.drawable.btn_small_invalid));
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
     * 发送验证码
     * @param cellPhone
     * created at 2015/8/7 by hutianfeng
     */

    private void sendSMS (final String cellPhone) {
        mMsgBox.showWaitProgressbar("发送中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindCellPhoneActivity.this).fSms_Send_SMS(cellPhone,"bind");
            }


            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0//手机号码没有绑定

                        mMsgBox.toast("验证码以短信已经发送到你的手机，请注意接收");

                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        stopGetAccessCode();

                        break;

                    case ErrorCode.ERR_VERIFICATION_CODE_NOT_SENT://53:短信接口报错
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("短信接口出错");
                        break;

                    case ErrorCode.ERR_VERIFICATION_CODE_TOO_MANY://54:短信验证码请求次数太多
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("短信验证码请求次数太多");
                        break;

                    case ErrorCode.ERR_SMS_MORE_TIMES://56:一天超过5次
                        MessageDialog dialog = new MessageDialog(FixBindCellPhoneActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("提示");
                        dialog.setMessage("今天手机号验证次数已超5次,请明天再试");
                        dialog.setCancelable(false);
                        dialog.setOnLeftClickListener("确定",null);
                        dialog.show();
                        break;

                    case ErrorCode.ERR_SMS_PHONE_NOT_CHECK://55:手机号码格式不正确
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("手机号码格式不正确");
                        break;

                    default:
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("访问的服务器出错");
                        break;
                }
            }
        });
    }


    /**
     * 验证手机短信获得的验证码
     * @param cellPhone
     * created at 2015/8/7 by hutianfeng
     */

    private void checkSMS (final String cellPhone,final String code) {
        mMsgBox.showWaitProgressbar("验证中");

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FixBindCellPhoneActivity.this).fSms_Check_Code(cellPhone,code);
            }


            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                switch (result) {
                    case ErrorCode.OK://0//验证码验证成功

                        //跳转到绑定手机号码页面
                        Intent bindPhone = new Intent(FixBindCellPhoneActivity.this,BindCellPhoneActivity.class);
                        bindPhone.putExtra(FIX_BIND_CELLPHONE, true);
                        startActivity(bindPhone);
                        FixBindCellPhoneActivity.this.finish();

                        break;

                    case ErrorCode.ERR_SMS_PHONE_NOT_CHECK://55:手机号码格式不正确
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("手机号码格式不正确");
                        break;

                    case ErrorCode.ERR_SMS_MORE_TIMES://56:一天超过5次
                        MessageDialog dialog = new MessageDialog(FixBindCellPhoneActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("提示");
                        dialog.setMessage("今天手机号验证次数已超5次,请明天再试");
                        dialog.setCancelable(false);
                        dialog.setOnLeftClickListener("确定",null);
                        dialog.show();
                        break;

                    case ErrorCode.ERR_SMS_CODE_OVER://57:验证码过期
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("验证码过期");
                        break;

                    case ErrorCode.ERR_SMS_CODE_NOT_CHECK://58:验证码验证不通过
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("验证码不正确");
                        break;

                    case ErrorCode.ERR_VERIFICATION_CODE_TOO_MANY://54:短信验证码请求次数太多
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("短信验证码请求次数太多");
                        break;


                    default:
                        textView_verification_code_result.setVisibility(View.VISIBLE);
                        textView_verification_code_result.setText("访问的服务器出错");
                        break;
                }
            }
        });
    }


}
