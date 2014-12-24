package co.onemeter.oneapp.ui;

import com.androidquery.AQuery;

import co.onemeter.oneapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class LessonInfoEditActivity extends Activity implements OnClickListener {

	public static final int TAG_CLASS_INFO = 0;
	public static final int TAG_LES_TABLE = 1;
	
	public static final String TERM = "term";
	public static final String GRADE = "grade";
	public static final String SUBJECT = "subject";
	public static final String DATE = "date";
	public static final String TIME = "time";
	public static final String PLACE = "place";
	
	private int tag;
	
	private EditText dtTerm;
	private EditText dtGrade;
	private EditText dtSubject;
	private EditText dtDate;
	private EditText dtTime;
	private EditText dtPlace;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lessoninfo_edit);
		
		initView();
	}
	
	private void initView(){
		Intent intent = getIntent();
		tag = intent.getIntExtra("tag", 0);
		
		dtTerm = (EditText) findViewById(R.id.ed_lesinfo_term);
		dtGrade = (EditText) findViewById(R.id.ed_lesinfo_grade);
		dtSubject = (EditText) findViewById(R.id.ed_lesinfo_subject);
		dtDate = (EditText) findViewById(R.id.ed_lesinfo_date);
		dtTime = (EditText) findViewById(R.id.ed_lesinfo_time);
		dtPlace = (EditText) findViewById(R.id.ed_lesinfo_place);
		
		AQuery q = new AQuery(this);
		q.find(R.id.cancel).clicked(this);
		
		if(tag == TAG_LES_TABLE){
			findViewById(R.id.lay_info_edit).setVisibility(View.GONE);
			findViewById(R.id.lay_les_edit).setVisibility(View.VISIBLE);
			q.find(R.id.title).text(getString(R.string.class_coursetable_info));
		}else{
			q.find(R.id.title).text(getString(R.string.class_info));
			findViewById(R.id.lay_info_edit).setVisibility(View.VISIBLE);
			findViewById(R.id.lay_les_edit).setVisibility(View.GONE);
			dtTerm.setText(intent.getStringExtra(TERM));
			dtGrade.setText(intent.getStringExtra(GRADE));
			dtSubject.setText(intent.getStringExtra(SUBJECT));
			dtDate.setText(intent.getStringExtra(DATE));
			dtTime.setText(intent.getStringExtra(TIME));
			dtPlace.setText(intent.getStringExtra(PLACE));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;

		default:
			break;
		}
	}
}
