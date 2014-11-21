package co.onemeter.oneapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import co.onemeter.oneapp.ui.IncallActivity;
import co.onemeter.oneapp.ui.LoginInvitedActivity;
import co.onemeter.oneapp.ui.StartActivity;
import com.bugsense.trace.BugSenseHandler;
import org.wowtalk.NetworkManager;
import org.wowtalk.api.JapaneseHelper;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.ui.GlobalValue;

import java.io.*;

public class YuanquActivity extends Activity {
	protected void onCreate(Bundle savedInstanceState) {

        // setup BugSenseHandler before setContentView
        BugSenseHandler.initAndStartSession(this, GlobalValue.BUGSENSE_APIKEY);

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.yuanqu);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Handler hdl = new Handler();
		hdl.postDelayed(new splashHandler(), 500);

        initJapaneseItaijiDictionary();
        initJapaneseKanwaDictionary();
        JapaneseHelper.setJapaneseDict(this);

        fixVoipState();

        // run test
//        Test.testOptGroup(this);
//        Test.testOptBuddy(this);
//        Test.testGetPendingBuddyRequests(this);
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
				intent.setClass(YuanquActivity.this, StartActivity.class);
			} else {
			    GlobalValue.IS_BOOT_FROM_LOGIN = true;
				intent.setClass(YuanquActivity.this, LoginInvitedActivity.class);
			}
			startActivity(intent);
			
			YuanquActivity.this.finish();
		}		
	}	
}
