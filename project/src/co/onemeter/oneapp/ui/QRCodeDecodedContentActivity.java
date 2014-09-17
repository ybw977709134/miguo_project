package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-5
 * Time: 下午3:05
 * To change this template use File | Settings | File Templates.
 */
public class QRCodeDecodedContentActivity extends Activity implements View.OnClickListener{
    public static String ACTIVITY_INTENT_EXT_KEY_CONTENT="content";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_code_decoded_content_layout);

        TextView tvContent=(TextView) findViewById(R.id.tv_decoded_content);
        String content=getIntent().getStringExtra(ACTIVITY_INTENT_EXT_KEY_CONTENT);
        tvContent.setText(content);

        findViewById(R.id.title_back).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }
}
