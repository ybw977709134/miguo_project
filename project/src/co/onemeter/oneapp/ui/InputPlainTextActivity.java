package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import com.androidquery.AQuery;
import org.wowtalk.ui.MessageBox;

/**
 * User: jianxd
 * Date: 4/12/13
 * Time: 3:31 PM
 */
public class InputPlainTextActivity extends Activity {

    /** page title */
    public static final String EXTRA_TITLE = "title";
    /** set default value, or get inputted value. */
    public static final String EXTRA_VALUE = "default";
    /** description. */
    public static final String EXTRA_DESCRIPTION = "desc";
    /** allow empty? */
    public static final String EXTRA_ALLOW_EMPTY = "allowempty";
    public static final String EXTRA_INPUTTYPE = "inputtype";

    private TextView btnTitleBack;
    private TextView btnTitleConfirm;
    private EditText edtValue;
    private ImageButton field_clear;
    private String defaultValue = "";
    private boolean allowEmpty = false;
    private RelativeLayout layout_input_plain_text;
    InputMethodManager mInputMethodManager ;

    String title = "";
    String desc = "";
    int inputType = -1;
    MessageBox msgBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_plain_text);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

//        String title = "";
//        String desc = "";
//        int inputType = -1;
        msgBox = new MessageBox(this);

        Bundle b;
        if (savedInstanceState != null) {
            b = savedInstanceState;
        } else {
            b = getIntent().getExtras();
        }
        if (b != null) {
            allowEmpty = b.getBoolean(EXTRA_ALLOW_EMPTY);
            defaultValue = b.getString(EXTRA_VALUE);
            inputType = b.getInt(EXTRA_INPUTTYPE, -1);
            title = b.getString(EXTRA_TITLE);
            desc = b.getString(EXTRA_DESCRIPTION);
        }

        AQuery q = new AQuery(this);
        layout_input_plain_text = (RelativeLayout) findViewById(R.id.layout_input_plain_text);
        btnTitleBack = (TextView) findViewById(R.id.title_back);
        btnTitleConfirm = (TextView) findViewById(R.id.title_confirm);
        edtValue = (EditText) findViewById(R.id.edt_value);
        field_clear = (ImageButton) findViewById(R.id.field_clear);
        
        mInputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        
        edtValue.setFocusable(true);
        edtValue.setFocusableInTouchMode(true);
        edtValue.setText(defaultValue);
        edtValue.requestFocus();
        
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
			
			@Override
			public void run() {
//				InputMethodManager imm = (InputMethodManager)edtValue.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);  
//				imm.showSoftInput(edtValue, 0);
				
				mInputMethodManager.showSoftInput(edtValue, InputMethodManager.RESULT_SHOWN);
				mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
		}, 200);
        
        if (inputType != -1)
            edtValue.setInputType(inputType);

        q.find(R.id.txt_desc).text(desc);
        setTitle(title);

        CharSequence text = edtValue.getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }

        if (text.length() > 0){
            field_clear.setVisibility(View.VISIBLE);
        } else {
            field_clear.setVisibility(View.GONE);
        }

        edtValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                onDone();
                return false;
            }
        });
        
        edtValue.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					field_clear.setVisibility(View.GONE);
				} else {
					field_clear.setVisibility(View.VISIBLE);
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
        
        edtValue.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					edtValue.setCursorVisible(true);
				} else {
					edtValue.setCursorVisible(false);
				}
				
			}
		});

        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                closeSoftKeyboard();
            }
        });

        btnTitleConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
                closeSoftKeyboard();
            }
        });
        
        //清除设置的文本
        field_clear.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				edtValue.setText("");
			}
		});
        
        layout_input_plain_text.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_input_plain_text.setFocusable(true);
				layout_input_plain_text.setFocusableInTouchMode(true);
				layout_input_plain_text.requestFocus();
				return false;
			}
		});
    }
    
    
	/**
	 * 重写onTouchEvent方法，获得向下点击事件，隐藏输入法
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

    	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
    		  if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
//    			  mInputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);  
    			  closeSoftKeyboard();
    			  
    			  }
    		  }
    	return super.onTouchEvent(event);
    }

    private void closeSoftKeyboard() {
    		mInputMethodManager.hideSoftInputFromWindow(edtValue.getWindowToken() , 0);
    }
    
    private void onDone() {
        String name = edtValue.getText().toString();
        if (!allowEmpty && TextUtils.isEmpty(name)) {
//            MessageBox msgBox = new MessageBox(this);
            msgBox.toast(R.string.input_single_line_text_empty_not_allowed);
            return;
        }

        //如果是手机号码的话需要进一步的验证
        if (title.equals("联系方式")) {
            if (!Utils.isPhoneNum(name)) {
                msgBox.toast("你填写的手机号码格式不正确");
                return;
            }
        }

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_VALUE, name);
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        new AQuery(this).find(R.id.txt_title).text(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


}
