package co.onemeter.oneapp.ui;

import java.util.List;

import org.wowtalk.api.Classroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.ClassroomActivity.ClassroomAdapter.ViewHodler;

public class HomeWorkTemplate extends Activity implements View.OnClickListener {

    private ImageButton title_back;
    private TextView textView_homework_back;
    private ListView listView;
    private String[] template;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_work_template);
        template = getResources().getStringArray(R.array.commentsTemplate);
        initView();
    }


    private void initView () {
        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_homework_back = (TextView) findViewById(R.id.textView_homework_back);
        listView = (ListView) findViewById(R.id.listView);

        //绑定数据源
//        listView.setAdapter(new ArrayAdapter(HomeWorkTemplate.this,android.R.layout.simple_list_item_1,template));
        listView.setAdapter(new HomeworkTemplateAdapter(template));

        title_back.setOnClickListener(this);
        textView_homework_back.setOnClickListener(this);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("template",template[position]);
                setResult(RESULT_OK,intent);
                HomeWorkTemplate.this.finish();
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
            case R.id.textView_homework_back:
                finish();
                break;

        }
    }
    
    class HomeworkTemplateAdapter extends BaseAdapter{

    	private String[] template;
		
		public HomeworkTemplateAdapter(String[] template){
			this.template = template;
		}
		@Override
		public int getCount() {
			return template.length;
		}

		@Override
		public Object getItem(int position) {
			return template[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_homework_template, parent, false);
				holder.home_work_template = (TextView) convertView.findViewById(R.id.home_work_template);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			holder.home_work_template.setText(template[position]);
			return convertView;
		}
		class ViewHodler{
			TextView home_work_template;
		}		
		
	}
}
