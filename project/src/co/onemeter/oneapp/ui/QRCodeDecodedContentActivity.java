package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private WebView webView_show;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_code_decoded_content_layout);

//        TextView tvContent=(TextView) findViewById(R.id.tv_decoded_content);
        webView_show  = (WebView) findViewById(R.id.webView_show);
//        String content = getIntent().getStringExtra(ACTIVITY_INTENT_EXT_KEY_CONTENT).trim();
//        Log.d("---------------", content);
//        tvContent.setText(content);
        webView_show.getSettings().setJavaScriptEnabled(true);
        webView_show.setWebChromeClient(new WebChromeClient());
        webView_show.setWebViewClient(new WebViewClient());
        webView_show.getSettings().setSupportZoom(true);
        webView_show.getSettings().setBuiltInZoomControls(true);
        webView_show.loadUrl(getIntent().getStringExtra(ACTIVITY_INTENT_EXT_KEY_CONTENT).trim());
        webView_show.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
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
