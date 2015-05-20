package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.TimerTextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created with Eclipse.
 * User: zz
 * Date: 19-5-15
 */
public class LessonHomeworkActivity extends Activity implements OnClickListener{
	private ImageButton title_back;
	private TextView tv_del;
	private TextView txt_content;
	private TableLayout imageTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_homework);
        
        initView();
    }

	private void initView() {
		title_back = (ImageButton) findViewById(R.id.title_back);
		tv_del = (TextView) findViewById(R.id.tv_del);
		txt_content = (TextView) findViewById(R.id.txt_content);
		imageTable = (TableLayout) findViewById(R.id.imageTable);
		
		title_back.setOnClickListener(this);
		tv_del.setOnClickListener(this);
	}

	private void getLessonHomework(){
		
	}
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.title_back:
			onBackPressed();
			break;
		case R.id.tv_del:
			break;
		default:
			break;
		}
	}

}
