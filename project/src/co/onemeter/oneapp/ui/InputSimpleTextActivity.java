package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

public class InputSimpleTextActivity extends Activity implements OnClickListener{

    public static final String CATEGORY_KEY = "category";
    public static final int CATEGORY_UNSET = 0;
    public static final int CATEGORY_PRONUNCIATION = 1;
    public static final int CATEGORY_PHONE = 2;
    public static final int CATEGORY_MOBILE = 3;
    public static final int CATEGORY_EMAIL = 4;
    public static final int CATEGORY_BRANCH_STORE = 5;
    public static final int CATEGORY_EMPLOYEE_ID = 6;

    public static final String ORIGINAL_VALUE_KEY = "original_value";
    public static final String RESULT_VALUE_KEY = "result_value";

	private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
	private EditText mInputText;

	private int mCategory;
    private String mOriginalValue;
    private MessageBox mMsgBox;
    private String mTitleCore;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleBack.setOnClickListener(this);

        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        btnTitleConfirm.setOnClickListener(this);

        TextView txtTitle = (TextView)findViewById(R.id.txt_title);
        mInputText=(EditText)findViewById(R.id.input_text);
        mInputText.setInputType(InputType.TYPE_CLASS_TEXT);
        switch (mCategory) {
        case CATEGORY_PRONUNCIATION:
            mTitleCore = getString(R.string.settings_pinyin);
            break;
        case CATEGORY_PHONE:
            mTitleCore = getString(R.string.contactinfo_phone);
            mInputText.setInputType(InputType.TYPE_CLASS_PHONE);
            break;
        case CATEGORY_MOBILE:
            mTitleCore = getString(R.string.contactinfo_mobile);
            mInputText.setInputType(InputType.TYPE_CLASS_PHONE);
            break;
        case CATEGORY_EMAIL:
            mTitleCore = getString(R.string.contactinfo_email);
            break;
        case CATEGORY_BRANCH_STORE:
            mTitleCore = getString(R.string.contactinfo_branch_store);
            break;
        case CATEGORY_EMPLOYEE_ID:
            mTitleCore = getString(R.string.contactinfo_employee_id);
            break;
        default:
            break;
        }
        txtTitle.setText(String.format(getString(R.string.settings_change_field), mTitleCore));

        mInputText.setText(mOriginalValue);
        mInputText.setSelection(mOriginalValue.length());
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
        case R.id.title_confirm:
            confirmChanged();
            break;
		default:
			break;
		}
	}

    private void confirmChanged() {
        final String newValue = mInputText.getText().toString().trim();
        // 可以为空，比如清空此字段的值
//        if (TextUtils.isEmpty(newValue)) {
//            mMsgBox.show(null, String.format(getString(R.string.inputsimpletext_empty), mTitleCore));
//            return;
//        }

        if (newValue.equals(mOriginalValue)) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_VALUE_KEY, newValue);
            setResult(RESULT_OK, intent);
            finish();
            return;
        }

        //check value is valid
        if (!TextUtils.isEmpty(newValue)) {
            switch (mCategory) {
            case CATEGORY_MOBILE:
                if(!Utils.verifyPhoneNumber(newValue)) {
                    mMsgBox.toast(R.string.bind_phone_format_error_msg);
                    return;
                }
                break;
            case CATEGORY_PHONE:
                if(!Utils.verifyInnerPhoneNumber(newValue)) {
                    mMsgBox.toast(R.string.bind_phone_format_error_msg);
                    return;
                }
                break;
            case CATEGORY_EMAIL:
                if(!Utils.verifyEmail(newValue)) {
                    mMsgBox.toast(R.string.bind_email_format_error_msg);
                    return;
                }
                break;
            default:
                break;
            }
        }

        mMsgBox.showWait();
        final Database dbHelper = new Database(InputSimpleTextActivity.this);
        final Buddy me = dbHelper.fetchBuddyDetail(new Buddy(PrefUtil.getInstance(InputSimpleTextActivity.this).getUid()));
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int whichField = Buddy.FIELD_FLAG_NONE;
                switch (mCategory) {
                    case CATEGORY_PRONUNCIATION:
                        whichField = Buddy.FIELD_FLAG_PRONUNCIATION;
                        me.pronunciation = newValue;
                        break;
                    case CATEGORY_PHONE:
                        whichField = Buddy.FIELD_FLAG_PHONE;
                        me.phoneNumber = newValue;
                        break;
                    case CATEGORY_MOBILE:
                        whichField = Buddy.FIELD_FLAG_MOBILE;
                        me.mobile = newValue;
                        break;
                    case CATEGORY_EMAIL:
                        whichField = Buddy.FIELD_FLAG_EMAIL;
                        me.setEmail(newValue);
                        break;
                    case CATEGORY_BRANCH_STORE:
                        whichField = Buddy.FIELD_FLAG_AREA;
                        me.area = newValue;
                        break;
                    default:
                        break;
                }

                // 此情况不会发生，发生则异常
                if (whichField == Buddy.FIELD_FLAG_NONE) {
                    return -1;
                }

                int resultCode = WowTalkWebServerIF.getInstance(InputSimpleTextActivity.this)
                        .fUpdateMyProfile(me, whichField);
                if (resultCode == ErrorCode.OK) {
                    dbHelper.updateMyselfInfo(me, whichField);
                }
                return resultCode;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.i("InputSimpleTextActivity#confirmChanged, fUpdateMyProfile result code is " + result);
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_VALUE_KEY, newValue);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    mMsgBox.show(null, String.format(getString(R.string.inputsimpletext_failure), mTitleCore));
                }
            }

        });
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_input_simple_text);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mCategory = getIntent().getIntExtra(CATEGORY_KEY, CATEGORY_UNSET);
        mOriginalValue = getIntent().getStringExtra(ORIGINAL_VALUE_KEY);
        mOriginalValue = (null == mOriginalValue) ? "" : mOriginalValue.trim();
        mMsgBox = new MessageBox(InputSimpleTextActivity.this);
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
