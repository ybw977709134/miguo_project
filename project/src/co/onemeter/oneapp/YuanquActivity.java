package co.onemeter.oneapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ImageView;
import co.onemeter.oneapp.ui.GlobalValue;
import co.onemeter.oneapp.ui.IncallActivity;
import co.onemeter.oneapp.ui.LoginActivity;
import co.onemeter.oneapp.ui.StartActivity;
import co.onemeter.oneapp.utils.TimeHelper;
import com.splunk.mint.Mint;
import org.wowtalk.NetworkManager;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;

import java.io.*;

public class YuanquActivity extends Activity {
	
	private SharedPreferences prefs;
	private boolean flag;
	
	private ImageView imageView_splash;
	protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Mint 错误捕捉
        //
        // 帐号信息
        // 用户名 panzhiyong@onemeter.co
        // 密码 oEBH5z84
        // https://mint.splunk.com
        if (!BuildConfig.DEBUG) {
            /*
            java.lang.OutOfMemoryError: (Heap Size=69428KB, Allocated=42146KB)
                at java.lang.AbstractStringBuilder.enlargeBuffer(AbstractStringBuilder.java:~94)
                at java.lang.AbstractStringBuilder.append0(AbstractStringBuilder.java:145)
                at java.lang.StringBuffer.append(StringBuffer.java:219)
                at com.splunk.mint.network.io.InputStreamMonitor.b(SourceFile:67)
                at com.splunk.mint.network.io.InputStreamMonitor.write(SourceFile:58)
                at org.wowtalk.api.OssClient.upload(SourceFile:153)
             */
            Mint.disableNetworkMonitoring();
            Mint.initAndStartSession(this, "579f6a2f");
        }

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.yuanqu);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        imageView_splash = (ImageView) findViewById(R.id.imageView_splash);
        //把是否启动欢迎页的信息写入配置参数里面
        prefs = getSharedPreferences("onemeter_visit_log", Context.MODE_PRIVATE);
		flag = prefs.getBoolean("visit", false);
		
        Handler hdl = new Handler();
		hdl.postDelayed(new splashHandler(), 500);

        fixVoipState();

        TimeHelper.syncTime(this);
    }

    private void fixVoipState() {
        boolean _userIsLogin = PrefUtil.getInstance(YuanquActivity.this).isLogined();
        //not calling
        if(null == IncallActivity.instance() && _userIsLogin) {
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    if (!(WowTalkVoipIF.fIsWowTalkServiceReady())) {
//                        WowTalkVoipIF.getInstance(YuanquActivity.this).fStartWowTalkService();
//                    }

                    if(NetworkManager.isConnected){
                        WowTalkVoipIF.getInstance(YuanquActivity.this).fBecomeActive();
                        
                    }
                }
            }).start();
        }
    }

    class splashHandler implements Runnable {
		public void run() {
			boolean _userIsLogin = PrefUtil.getInstance(YuanquActivity.this).isLogined();
			Intent intent = new Intent();
			if (_userIsLogin) {
			    GlobalValue.IS_BOOT_FROM_LOGIN = false;
			    imageView_splash.setBackgroundResource(R.drawable.splashscreen);
				intent.setClass(YuanquActivity.this, StartActivity.class);
			} else {
			    GlobalValue.IS_BOOT_FROM_LOGIN = true;
//				intent.setClass(YuanquActivity.this, LoginInvitedActivity.class);
			    if (!flag) {//还未安装本软件，跳转到欢迎引导页
			    	imageView_splash.setBackgroundResource(R.drawable.icon);
			    	intent.setClass(YuanquActivity.this, WelcomeActivity.class);
			    } else {
			    	imageView_splash.setBackgroundResource(R.drawable.splashscreen);
			    	intent.setClass(YuanquActivity.this, LoginActivity.class);
			    }
			    
			}
			startActivity(intent);
			
			YuanquActivity.this.finish();
		}		
	}	
}
