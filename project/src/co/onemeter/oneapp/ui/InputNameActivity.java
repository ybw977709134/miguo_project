package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 4/12/13
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputNameActivity extends Activity {

    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
    private EditText edtName;

    private String strNickname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_name);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        strNickname = getIntent().getStringExtra("name");

        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        edtName = (EditText) findViewById(R.id.edt_name);

        edtName.setText(strNickname);
        CharSequence text = edtName.getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }

        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnTitleConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtName.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    MessageBox msgBox = new MessageBox(InputNameActivity.this);
                    msgBox.toast(R.string.change_nickname_empty);
                    return;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("text", name);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
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
