package co.onemeter.oneapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wowtalk.api.Connect2;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;
import co.onemeter.oneapp.R;

/**
 * 处理 Web 请求时账号验证失败的事件。
 *
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-13
 * Time: AM10:19
 * To change this template use File | Settings | File Templates.
 */
public class AuthFailureReceiver extends BroadcastReceiver {

    private static boolean sIsHandling;

    public void onReceive(Context context, Intent intent) {
        Log.e("auth failure receiver received");

        if (sIsHandling || !PrefUtil.getInstance(context).isLogined()) {
            return;
        }
        sIsHandling = true;

        PrefUtil.getInstance(context).logoutAccount();
        ManageAccountsActivity.deleteDatasInDB(context);
        Database database = new Database(context);
        database.close();
        // 变换Database,Connect2的标志位(用户判断是否切换了用户)
        Database.sFlagIndex++;
        Connect2.sFlagIndex++;

        // auth failure, don't need to logout.
//        WowTalkWebServerIF mWeb = WowTalkWebServerIF.getInstance(context);
//        mWeb.fLogout();
        WowTalkVoipIF.getInstance(context).fStopWowTalkService();
        Intent loginIntent = new Intent(context, LoginActivity.class);
        // android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity
        // context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        loginIntent.putExtra(LoginActivity.EXTRA_PROMPT,
                context.getString(R.string.account_web_api_auth_failure));
        context.startActivity(loginIntent);

        if(null != StartActivity.instance()) {
            StartActivity.instance().finish();
        }
        sIsHandling = false;
    }
}

