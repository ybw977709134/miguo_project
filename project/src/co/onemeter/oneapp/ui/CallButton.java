package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.WowTalkVoipIF;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

public class CallButton extends Button implements OnClickListener{
	
	private OnClickListener _externalClickLister;
	private Context context;

	public void setExternalClickListener(OnClickListener c) {
		_externalClickLister = c;
	}
	
	public CallButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		setOnClickListener(this);
	}
	
	public void onClick(View v) {
		try {
			if (!WowTalkVoipIF.fAcceptCallIfIncomingPending()) {
				String address = getText().toString();
				if (address == null)
					return;
				Log.e("CallAddress=" + address);
				final String globalPhoneNumber = WowTalkWebServerIF.getInstance(context).fTranslatePhoneNumberToGlobalPhoneNumber(address);
				final String encriptedglobalNumber = WowTalkWebServerIF.fGetEncriptedPhoneNumber(globalPhoneNumber);
				Database dbHelper = new Database(context);
				
				Buddy buddy = dbHelper.buddyWithPhoneNumber(encriptedglobalNumber);
				
				if (buddy != null) {
					CallMainActivity.startNewOutGoingCall(context, buddy.userID, globalPhoneNumber, false);
				} else {
					new Thread() {
						public void run() {
							ArrayList<String> list = new ArrayList<String>();
							list.add(globalPhoneNumber);
							WowTalkWebServerIF.getInstance(context).fScanPhoneNumbersForBuddy(list);
						}
					}.start();
					//GlobaleValue._weStartNormalPhoneCall = true;
					Intent intent = new Intent(Intent.ACTION_CALL, 
							Uri.parse("tel:" + address));
					context.startActivity(intent);
				}
			}
		} catch (Exception e) {
			WowTalkVoipIF.fTerminateCall(false);
			onWrongDestinationAddressAware();
		}
		
		if (_externalClickLister != null) {
			_externalClickLister.onClick(v);
		}
	}
	
	protected void onWrongDestinationAddressAware() {
        MessageBox msgbox = new MessageBox(context);
        msgbox.show(null, "无法呼叫地址");
	}
}
