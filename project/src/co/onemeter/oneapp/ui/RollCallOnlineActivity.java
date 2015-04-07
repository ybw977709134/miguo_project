package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.LessonPerformance;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * Created by jacky on 15-4-3.
 */
public class RollCallOnlineActivity extends Activity implements View.OnClickListener {
	
    private String lesson_name;
    private int lessonId;
    private String classId;

    private ListView listView;

    private List<Map<String, Object>> classstudents = new ArrayList<Map<String,Object>>();
    private ArrayList<LessonPerformance> performancesToPost = new ArrayList<>();

    private LessonWebServerIF signWebServer;
    private MessageBox msgbox;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_call);

        Intent data = getIntent();
        classId = data.getStringExtra("classId");
        lessonId = data.getIntExtra("lessonId", -1);
        lesson_name = data.getStringExtra("lesson_name");

        msgbox = new MessageBox(RollCallOnlineActivity.this);
        signWebServer = LessonWebServerIF.getInstance(RollCallOnlineActivity.this);
        initView();

        getClassStudentInfo();
    }

    
    private void getClassStudentInfo(){

        msgbox.showWait();
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, List<Map<String, Object>>>() {

            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                List<Map<String, Object>> reslut = signWebServer.getClassStudents(classId);
                return reslut;
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
                msgbox.dismissWait();
                if (result != null) {
                    classstudents.clear();
                    classstudents.addAll(result);
                    listView.setAdapter(new StuRollCallAdapter(classstudents));

                    //ScrollView嵌套ListView需要计算listview内容高度
                    ListViewUtils.setListViewHeightBasedOnChildren(listView);
                }

            }

        });
    }

    void initView(){
        findViewById(R.id.roll_call_ok).setOnClickListener(this);
        findViewById(R.id.btn_all_signin).setOnClickListener(this);
        listView = (ListView) findViewById(R.id.listView_roll_call);

        TextView txt_name = (TextView) findViewById(R.id.title_back);
        txt_name.setOnClickListener(this);
        txt_name.setText(lesson_name);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_back://返回
                finish();
                break;
            case R.id.roll_call_ok://确认
                int stuSize = classstudents.size();
                if(stuSize == 0){
                    return ;
                }
                if(performancesToPost.size() > 0 && performancesToPost != null){
                    postRollCallByPropertyValue();
                }
                break;
            case R.id.btn_all_signin://全部签到
                if(classstudents.size() > 0){
                    setAllSignIn();
                }
                break;
        }
    }

    private void setAllSignIn(){
        int size = classstudents.size();
        for (int i = 0;i < size;i ++){
            int first = listView.getFirstVisiblePosition();
            int index = i - first;
            if(index >= 0){
                View view = listView.getChildAt(index);
                StuRollCallAdapter.ViewHolder holder = (StuRollCallAdapter.ViewHolder) view.getTag();
                holder.radio2 = (RadioButton) view.findViewById(R.id.radio2);
                holder.radio2.setChecked(true);
                //performancesToPost.get(i).property_value = 2;
            }

        }
    }

    private void postRollCallByPropertyValue(){
        msgbox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = ErrorCode.BAD_RESPONSE;
                ArrayList<LessonPerformance> performances_value0 = new ArrayList<LessonPerformance>();
                ArrayList<LessonPerformance> performances_value1 = new ArrayList<LessonPerformance>();
                ArrayList<LessonPerformance> performances_value2 = new ArrayList<LessonPerformance>();
                for(LessonPerformance performance : performancesToPost){
                    switch (performance.property_value){
                        case 0:
                            performances_value0.add(performance);
                            break;
                        case 1:
                            performances_value1.add(performance);
                            break;
                        case 2:
                            performances_value2.add(performance);
                            break;
                    }
                }
                if(!performances_value0.isEmpty()){
                    resultCode = signWebServer.addOrModifyStudentsRollcall(performances_value0);
                }
                if(!performances_value1.isEmpty()){
                    resultCode = signWebServer.addOrModifyStudentsRollcall(performances_value1);
                }
                if(!performances_value2.isEmpty()){
                    resultCode = signWebServer.addOrModifyStudentsRollcall(performances_value2);
                }
                return resultCode;
            }

            protected void onPostExecute(Integer result) {
                msgbox.dismissWait();
                if (ErrorCode.OK == result) {
                    Toast.makeText(RollCallOnlineActivity.this, R.string.class_submit_success, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RollCallOnlineActivity.this,R.string.class_submit_failed,Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private class StuRollCallAdapter extends BaseAdapter{

        private LayoutInflater inflater;
        private final int performence_property_id = 10;
        private List<Map<String, Object>> classstudents;

        public StuRollCallAdapter(List<Map<String, Object>> classstudents){
            inflater = LayoutInflater.from(RollCallOnlineActivity.this);
            this.classstudents = classstudents;
        }

        @Override
        public int getCount() {
            if(classstudents == null || classstudents.isEmpty()){
                return 0;
            }
            return classstudents.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return classstudents.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.listitem_lesson_status,parent,false);
                holder.tv_name = (TextView) convertView.findViewById(R.id.item_les_perform_tv);
                holder.rg_per = (RadioGroup) convertView.findViewById(R.id.item_les_radioGroup);
                holder.radio0 = (RadioButton) convertView.findViewById(R.id.radio0);
                holder.radio1 = (RadioButton) convertView.findViewById(R.id.radio1);
                holder.radio2 = (RadioButton) convertView.findViewById(R.id.radio2);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            holder.tv_name.setText(TextUtils.isEmpty(classstudents.get(position).get("student_alias").toString()) 
            		? classstudents.get(position).get("student_username").toString()
            		: classstudents.get(position).get("student_alias").toString());
            
            holder.radio2.setChecked(true);
            
            final LessonPerformance performance = new LessonPerformance();
            performance.property_value = 2;
            performance.lesson_id = lessonId;
            performance.student_id = classstudents.get(position).get("student_id").toString();
            performance.property_id = performence_property_id;
            
            holder.rg_per.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.radio0:
                            performance.property_value = 0;
                            break;
                        case R.id.radio1:
                            performance.property_value = 1;
                            break;
                        case R.id.radio2:
                            performance.property_value = 2;
                            break;
                        default:
                            performance.property_value = 2;
                            break;
                    }
                    performancesToPost.remove(position);
                    performancesToPost.add(position,performance);
                }
            });
            performancesToPost.add(performance);
            return convertView;
        }

        class ViewHolder{
            TextView tv_name;
            RadioGroup rg_per;
            RadioButton radio0;
            RadioButton radio1;
            RadioButton radio2;
        }
    }
    
    
    @Override
    protected void onDestroy() {
    	if (classstudents != null) {
    		classstudents =null;
    	}
    	if (signWebServer != null) {
    		signWebServer = null;
    	}
    	super.onDestroy();
    }
}
