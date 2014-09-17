package co.onemeter.oneapp.ui;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PrefUtil prefUtil = PrefUtil.getInstance(context);
		int setupStep = prefUtil.getSetupStep();
		
		if (setupStep != 2)
			return;
		
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
	}

}
