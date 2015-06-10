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
import org.wowtalk.api.JapaneseHelper;
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
        if (!BuildConfig.DEBUG)
            Mint.initAndStartSession(this, "579f6a2f");

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

        initJapaneseItaijiDictionary();
        initJapaneseKanwaDictionary();
        JapaneseHelper.setJapaneseDict(this);

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

    /**
     * 将res/raw/下的kanwadict_x字典拷贝合并为/data/data/co.onemeter.oneapp/files/kanwadict文件
     */
    private void initJapaneseKanwaDictionary() {
        // 判断是否已经拷贝完成
        final File destFile = new File(JapaneseHelper.JA_DICTIONARY_PATH_PRE + getPackageName()
                + JapaneseHelper.JA_DICTIONARY_KANWA_PATH_SUFFIX);
        if (destFile.exists()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isCopySuccess = false;
                // 由于raw下只能读写1M以内文件，所以先将字典分为4部分
                int[] jaDicts = new int[] {
                        R.raw.kanwadict_1,
                        R.raw.kanwadict_2,
                        R.raw.kanwadict_3,
                        R.raw.kanwadict_4};
                try {
                  OutputStream os = openFileOutput("kanwadict", Context.MODE_PRIVATE);
                  byte[] buffer = new byte[1024];
                  InputStream is;
                  int readLen = 0;
                  for (int dataResId : jaDicts) {
                      is = getResources().openRawResource(dataResId);
                      while ((readLen=is.read(buffer))!=-1) {
                          os.write(buffer, 0, readLen);
                      }
                      os.flush();
                      is.close();
                  }
                  os.close();
                  isCopySuccess = true;
              } catch (FileNotFoundException exception) {
                  exception.printStackTrace();
              } catch (IOException exception) {
                  exception.printStackTrace();
              } finally {
                  // 拷贝失败，则删除目标文件，待下次重新拷贝
                  if (!isCopySuccess) {
                      destFile.delete();
                  }
              }
            }
        }).start();
    }

    /**
     * 将res/raw/下的kanwadict_x字典拷贝合并为/data/data/co.onemeter.oneapp/files/itaiji文件
     */
    private void initJapaneseItaijiDictionary() {
        // 判断是否已经拷贝完成
        final File destFile = new File(JapaneseHelper.JA_DICTIONARY_PATH_PRE + getPackageName()
                + JapaneseHelper.JA_DICTIONARY_ITAIJI_PATH_SUFFIX);
        if (destFile.exists()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isCopySuccess = false;
                try {
                  OutputStream os = openFileOutput("itaiji", Context.MODE_PRIVATE);
                  byte[] buffer = new byte[1024];
                  InputStream is;
                  int readLen = 0;
                  is = getResources().openRawResource(R.raw.itaijidict);
                  while ((readLen=is.read(buffer))!=-1) {
                      os.write(buffer, 0, readLen);
                  }
                  
                  os.flush();
                  is.close();
                  os.close();
                  isCopySuccess = true;
              } catch (FileNotFoundException exception) {
                  exception.printStackTrace();
              } catch (IOException exception) {
                  exception.printStackTrace();
              } finally {
                  // 拷贝失败，则删除目标文件，待下次重新拷贝
                  if (!isCopySuccess) {
                      destFile.delete();
                  }
              }
            }
        }).start();
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
