package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;

public class SettingWowTalkIdActivity extends Activity {
    public static final String EXTRA_WOWID = "wowid";
    public static final String EXTRA_READONLY = "rdonly";

    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
    private ImageButton fieldClear;
    private EditText edtWowTalkId;
    private MessageBox mMsgBox;
    private boolean mReadonly = false;
    private String mOrigWowId = null;
    private WebServerIF mWeb;

    private void resetWowTalkId(final String wowtalkId) {
        mMsgBox.showWait();

        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return WebServerIF.getInstance(SettingWowTalkIdActivity.this)
                        .fChangeWowtalkId(wowtalkId);
            }
            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                if (result == ErrorCode.OK) {
                    finish();
                } else {
                    if(mMsgBox == null)
                        mMsgBox = new MessageBox(SettingWowTalkIdActivity.this);

                    if (result == ErrorCode.USER_ALREADY_EXISTS) {
                        mMsgBox.show(getString(R.string.operation_failed),
                                getString(R.string.settings_account_wowid_used_by_others));
                    } else if (result == ErrorCode.WOWID_NOT_CHANGED) {
                        mMsgBox.show(getString(R.string.operation_failed),
                                getString(R.string.settings_account_wowid_already_changed));
                    } else {
                        mMsgBox.show(null, getString(R.string.operation_failed));
                    }
                }
            }
        }.execute((Void)null);
    }

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        edtWowTalkId = (EditText) findViewById(R.id.edt_wowtalkid);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);

        edtWowTalkId.setEnabled(!mReadonly);
        edtWowTalkId.setText(mOrigWowId == null ? "" : mOrigWowId);

        btnTitleBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }

        });

        btnTitleConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mReadonly) {
                    finish();
                    return;
                }

                final String strWowTalkId = edtWowTalkId.getText().toString().trim();

                // doesn't modify.
                if (!TextUtils.isEmpty(mOrigWowId) && mOrigWowId.equals(strWowTalkId)) {
                    finish();
                    return;
                }

                if (!Utils.verifyWowTalkId(strWowTalkId)) {
                    mMsgBox.show(null, getString(R.string.setting_wowtalkid_format_error));
                    return;
                }

                new AlertDialog.Builder(SettingWowTalkIdActivity.this)
                        .setTitle(R.string.setting_wowtalkid_dialog_title)
                        .setMessage(String.format(getResources().getString(R.string.setting_wowtalkid_dialog_msg), strWowTalkId))
                        .setPositiveButton(R.string.confirm,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resetWowTalkId(strWowTalkId);
                                    }
                                })
                        .create().show();
            }
        });

        fieldClear.setOnClickListener(new OnClickListener() {
        	
            @Override
            public void onClick(View v) {
                edtWowTalkId.setText("");
            }
        });
        edtWowTalkId.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (s.length() == 0) {
                    fieldClear.setVisibility(View.GONE);
//                    btnTitleConfirm.setEnabled(false);
                } else {
                    if (!mReadonly) {
                        fieldClear.setVisibility(View.VISIBLE);
                    } else {
                        fieldClear.setVisibility(View.GONE);
                    }
                    btnTitleConfirm.setEnabled(true);
                }
            }

        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_wowtalkid);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mWeb = WebServerIF.getInstance(this);
        mReadonly = getIntent().getBooleanExtra(EXTRA_READONLY, false);
        mOrigWowId = getIntent().getStringExtra(EXTRA_WOWID);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
