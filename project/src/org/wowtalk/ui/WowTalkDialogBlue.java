package org.wowtalk.ui;


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;


public class WowTalkDialogBlue  {
	private static Dialog dialogWowTalkBlue;
	private static LayoutInflater mInflater;
	private static int screenH = 0;
	
	@SuppressWarnings("deprecation")
	public static void showWowTalkDialog(Context context, String strTitle,String[] strArrayChoice,OnItemClickListener onItemClickListener){
		if(dialogWowTalkBlue!=null){
			dialogWowTalkBlue.dismiss();
			dialogWowTalkBlue=null;
		}
		
		
		if(mInflater==null){
			mInflater=LayoutInflater.from(context);
		}
		
		if(screenH == 0) {
			screenH = context.getResources().getDisplayMetrics().heightPixels;
		}
		
		//initialization
		final View viewWowTalkDialogBlue = mInflater.inflate(R.layout.wowtalk_dialog_blue, null);
		TextView txt_title= (TextView) viewWowTalkDialogBlue.findViewById(R.id.txt_title);
		ListView lv_choices = (ListView) viewWowTalkDialogBlue.findViewById(R.id.lv_choices);
		int bgcolor = R.color.blue;
		viewWowTalkDialogBlue.setBackgroundColor(bgcolor);
		lv_choices.setDivider(new ColorDrawable(bgcolor));
		lv_choices.setCacheColorHint(bgcolor);
		dialogWowTalkBlue = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);

		
		//custom title
		if(strTitle==null){
			txt_title.setVisibility(View.GONE);
		}
		else{
			txt_title.setVisibility(View.VISIBLE);
			txt_title.setText(strTitle);
		}
		
		//custom choices
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, 
				R.layout.wowtalk_dialog_listitem,
				strArrayChoice);
		lv_choices.setAdapter(adapter);
		
	
		//custom listener
		lv_choices.setOnItemClickListener(onItemClickListener);
		
		

		dialogWowTalkBlue.setCanceledOnTouchOutside(true);
		dialogWowTalkBlue.setContentView(viewWowTalkDialogBlue);
		//dialogWowTalkBlue.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		dialogWowTalkBlue.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		WindowManager.LayoutParams lp=dialogWowTalkBlue.getWindow().getAttributes(); 
		lp.dimAmount = 0.5f;
		lp.alpha = 0.9f;
		if(strArrayChoice.length>5){
			lp.height = screenH /2;
		}
		else {
			lp.height =  LayoutParams.WRAP_CONTENT;
		}
		lp.width = LayoutParams.FILL_PARENT;
		dialogWowTalkBlue.getWindow().setAttributes(lp);
		
		
		//show dialog
		dialogWowTalkBlue.show();
		
	}
	
	public static void  dismissWowTalkDialog() {
		if(dialogWowTalkBlue!=null){
			dialogWowTalkBlue.dismiss();
			dialogWowTalkBlue=null;
		}
	}
	
		
}
