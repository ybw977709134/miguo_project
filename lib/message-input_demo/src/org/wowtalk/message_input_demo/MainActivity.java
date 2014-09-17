package org.wowtalk.message_input_demo;

import org.wowtalk.ui.msg.InputBoardManager;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.ViewGroup;

public class MainActivity extends Activity {

	InputBoardManager mInputMgr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mInputMgr = new InputBoardManager(this, (ViewGroup)this.findViewById(android.R.id.content), null);
		mInputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
