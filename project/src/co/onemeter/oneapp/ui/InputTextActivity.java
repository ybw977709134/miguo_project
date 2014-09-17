package co.onemeter.oneapp.ui;

import android.text.*;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class InputTextActivity extends Activity implements OnClickListener{
    private static final int MAX_NUM = 140;

	private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
	private Button btnOK;
	private EditText newNameInput;
    private TextView txtNum;

    private String strStatus;
    private MessageBox mMsgBox;
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleBack.setOnClickListener(this);

        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        btnTitleConfirm.setOnClickListener(this);
		
		newNameInput=(EditText)findViewById(R.id.new_name);
        txtNum = (TextView) findViewById(R.id.txt_num);
        newNameInput.setText(strStatus);
        newNameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(140)});
        txtNum.setText(String.format("%d/%d", newNameInput.getText().length(), MAX_NUM));
        CharSequence text = newNameInput.getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }
        newNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtNum.setText(String.format("%d/%d", newNameInput.getText().length(), MAX_NUM));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
		
		TextView txtTitle = (TextView)findViewById(R.id.txtTitle);
		Bundle b = this.getIntent().getExtras();
		String title = null;
		if(b != null && (title = b.getString("title")) != null)
			txtTitle.setText(title);
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			InputTextActivity.this.finish();
			break;
        case R.id.title_confirm:
            if (newNameInput.getText().length() > MAX_NUM) {
                mMsgBox.toast(R.string.edit_status_length_oom);
                return;
            }
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("text", newNameInput.getText().toString());
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            break;
		default:
			break;
		}
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inputtext);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        strStatus = getIntent().getStringExtra("status");
        mMsgBox = new MessageBox(InputTextActivity.this);
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
