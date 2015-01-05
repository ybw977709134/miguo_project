package co.onemeter.oneapp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkVoipIF;

public class PhoneStateChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        int setupStep = PrefUtil.getInstance(context).getSetupStep();
		
		if (setupStep == 2) {
			final String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (extraState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
				fAdjustWowTalkEngineForGSMCall(context, true);
			} else if (extraState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
				if (WowTalkVoipIF.fIsWowTalkServiceReady()) {
					fAdjustWowTalkEngineForGSMCall(context, true);
				}
			} else if (extraState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				fAdjustWowTalkEngineForGSMCall(context, false);
				
				if (GlobalValue._weStartANormalPhoneCall) {
					GlobalValue._weStartANormalPhoneCall = false;
					Intent startWowTalkIntent = new Intent(context, StartActivity.class);
					startWowTalkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(startWowTalkIntent);
				}
			}
		}
	}
	
	private void fAdjustWowTalkEngineForGSMCall(Context context, boolean gsm_io_happen) {
		if (!WowTalkVoipIF.fIsWowTalkServiceReady()) {
            AppStatusService.getOfflineMessages(context);
			WowTalkVoipIF.getInstance(context).fStartWowTalkService();
            WowTalkVoipIF.fDisableRingingInSDK(true);
			return;
		}
		WowTalkVoipIF.fGSMStateChanged(gsm_io_happen);
		WowTalkVoipIF.fPauseResumeCall(gsm_io_happen);
	}

}
