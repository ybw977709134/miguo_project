package co.onemeter.oneapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;

public class IncomeCallIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        int setupStep = PrefUtil.getInstance(context).getSetupStep();

		if (setupStep == 2) {
			if (WowTalkVoipIF.fGetMainActivity() == null) {
				WowTalkVoipIF.fSetMainActivity(CallMainActivity.class);
			}
			if (WowTalkVoipIF.fGetChatMessageDelegate() == null) {
				
			}
			if (!WowTalkVoipIF.fIsWowTalkServiceReady()) {
                AppStatusService.getOfflineMessages(context);
				WowTalkVoipIF.getInstance(context).fStartWowTalkService();
                WowTalkVoipIF.fDisableRingingInSDK(true);
			}
			
			context.startActivity(new Intent().setClass(context, WowTalkVoipIF.fGetMainActivity()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}

	}

}
