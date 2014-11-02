package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

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

    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
    private EditText edtName;

    private String defaultValue = "";
    private boolean allowEmpty = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_singleline_text);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        String title = "";
        String desc = "";

        Bundle b;
        if (savedInstanceState != null) {
            b = savedInstanceState;
        } else {
            b = getIntent().getExtras();
        }
        if (b != null) {
            allowEmpty = b.getBoolean(EXTRA_ALLOW_EMPTY);
            defaultValue = b.getString(EXTRA_VALUE);
            title = b.getString(EXTRA_TITLE);
            desc = b.getString(EXTRA_DESCRIPTION);
        }

        AQuery q = new AQuery(this);

        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        edtName = (EditText) findViewById(R.id.edt_value);

        edtName.setText(defaultValue);
        q.find(R.id.txt_desc).text(desc);
        setTitle(title);

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
                if (!allowEmpty && TextUtils.isEmpty(name)) {
                    MessageBox msgBox = new MessageBox(InputPlainTextActivity.this);
                    msgBox.toast(R.string.input_single_line_text_empty_not_allowed);
                    return;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_VALUE, name);
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
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        new AQuery(this).find(R.id.txt_title).text(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
