package co.onemeter.oneapp.UUPlayer;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import ulucu.api.client.http.listener.HttpListener;

/**
 * Created by hutianfneg on 15-7-21.
 */
public class BaseActivity extends Activity implements HttpListener {

    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        dialog = new Dialog(this);
        dialog.setTitle("请耐心等待");
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    /*
     *
     * 用于监听网络状态 处理错误消息
     */
    public void HttpListenerRespondError(String message, int code) {
        dialog.hide();
        Toast.makeText(this, "error code" + code, Toast.LENGTH_SHORT).show();

    }

	/*
	 *
	 * 监听网络状态 处理正确事件
	 */

    @Override
    public void HttpListenerRespondSuccess() {
        dialog.hide();

    }

    @Override
    public void HttpListenerWaiting() {
        dialog.show();

    }

    /*
     *
     * 监听网络事件 抛出错误消息
     */
    @Override
    public void HttpListenerNetError() {
        dialog.hide();
        Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();

    }

    /*
     *
     * 监听网络事件 抛出错误消息
     */
    @Override
    public void HttpListenerRespondError(int code) {
        // TODO Auto-generated method stub

    }
}
