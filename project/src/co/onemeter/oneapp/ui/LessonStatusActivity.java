package co.onemeter.oneapp.ui;

import java.util.List;

import org.wowtalk.api.LessonPerformance;
import org.wowtalk.api.WowLessonWebServerIF;

import com.androidquery.AQuery;

import co.onemeter.oneapp.Constants;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * 上课状态页面。
 * Created by yl on 23/12/2014.
 */
public class LessonStatusActivity extends Activity implements OnClickListener{

	public static final String FALG = "isTeacher";
	
	private int lessonId;
	private boolean isTeacher;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lesson_status);
		
		initView();
	}
	
	private void initView(){
		String[] preforms = getResources().getStringArray(R.array.lesson_performance_names);
		AQuery q = new AQuery(this);
		q.find(R.id.lv_les_status).adapter(new LessonStatusAdapter(preforms, null));
		q.find(R.id.title_back).clicked(this);
		
		Button btn = (Button) findViewById(R.id.btn_parent_confirm);
		btn.setOnClickListener(this);
		
		Intent intent = getIntent();
		lessonId = intent.getIntExtra(Constants.LESSONID,0);
		isTeacher = intent.getBooleanExtra(FALG, false);
		if(isTeacher){
			btn.setText(R.string.login_retrieve_password_btn);
		}
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_parent_confirm:
			submitStuPerformance();
			break;
		case R.id.title_back:
			finish();
			break;
		default:
			break;
		}
	}
	
	private void submitStuPerformance(){
		new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return WowLessonWebServerIF.getInstance(LessonStatusActivity.this).addOrModifyLessonPerformance(null);
			}
			
		}.execute((Void)null);
	}
	
	class LessonStatusAdapter extends BaseAdapter{
		private String[] pers;
		private List<LessonPerformance> lesPers;
		
		public LessonStatusAdapter(String[] pers,List<LessonPerformance> lesPers){
			this.lesPers = lesPers;
			this.pers = pers;
		}
		
		@Override
		public int getCount() {
			return pers.length;
		}

		@Override
		public Object getItem(int position) {
			return pers[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(convertView == null){
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.listitem_lesson_status, parent, false);
				holder.tv_per = (TextView) convertView.findViewById(R.id.item_les_perform_tv);
				holder.rg_per = (RadioGroup) convertView.findViewById(R.id.item_les_radioGroup);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.tv_per.setText(pers[position]);
			if(!Utils.isAccoTeacher(LessonStatusActivity.this)){
				holder.rg_per.setEnabled(false);
				
			}
			return convertView;
		}
		
		class ViewHolder{
			TextView tv_per;
			RadioGroup rg_per;
		}
		
	}

}
