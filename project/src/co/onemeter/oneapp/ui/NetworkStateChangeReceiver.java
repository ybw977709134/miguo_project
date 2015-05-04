package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.Intent;
import org.wowtalk.NetworkManager;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;

import java.util.ArrayList;

public class NetworkStateChangeReceiver extends NetworkManager {

    @Override
    public void onReceive(final Context context, Intent intent) {

        super.onReceive(context, intent);

        if (!isConnected) {
            return;
        }
        Log.e("NetworkStateChangeReceiver,", "Network changed to be connected...");

        AppStatusService.getOfflineMessages(context);

        int setupStep = PrefUtil.getInstance(context).getSetupStep();
        if (setupStep == 2) {
            try {
                if (WowTalkVoipIF.fGetMainActivity() == null) {
                    WowTalkVoipIF.fSetMainActivity(CallMainActivity.class);
                }
                if (WowTalkVoipIF.fGetChatMessageDelegate() == null) {
                }
                if (!WowTalkVoipIF.fIsWowTalkServiceReady()) {
                    WowTalkVoipIF.getInstance(context).fStartWowTalkService();
                    WowTalkVoipIF.fDisableRingingInSDK(true);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(NetworkManager.isConnected && WowTalkVoipIF.fHasLoginToServer()){
                        Database dbHelper = new Database(context);
                        ArrayList<ChatMessage> unsentReceiptList=dbHelper.fetchAllUnsentReceipt();
                        for(int i=0;i<unsentReceiptList.size();i++){
                            ChatMessage msg = unsentReceiptList.get(i);
                            WowTalkVoipIF.getInstance(context).fResendUnsentReceipt(msg);
                        }
                    }
                }
            }).start();

        }
    }
}
