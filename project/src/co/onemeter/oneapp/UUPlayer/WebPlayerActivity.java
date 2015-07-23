package co.onemeter.oneapp.UUPlayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wowtalk.ui.MessageBox;

import co.onemeter.oneapp.R;

/**
 * 同过url直接加载视频
 * Created by hutianfeg on 15-7-23.
 */
public class WebPlayerActivity extends Activity{
    private WebView webView;
    private MessageBox messageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player_web);
        messageBox = new MessageBox(this);

        String device_id = getIntent().getStringExtra("device_id");
        String url =  "http://demo.anyan.com/demo_bh.html?device_id="+device_id+"&channel_id=1&rate=700";

        Log.d("---rul:---",url);

        webView = (WebView) findViewById(R.id.webView_main);

        webView.setHorizontalScrollBarEnabled(false);//水平不显示
        webView.setVerticalScrollBarEnabled(false); //垂直不显示

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //加载播放视频的网页必须设置
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl(url);
    }


    /**
     * 视频访问网络加载进度
     */
    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            messageBox.showWait();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            messageBox.dismissWait();
        }
    }
}
