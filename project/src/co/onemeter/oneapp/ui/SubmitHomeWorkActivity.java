package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.GetLessonHomework;
import org.wowtalk.api.HomeWorkResult;
import org.wowtalk.api.LessonWebServerIF;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.HomeWorkAdapter;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * Created by hutianfeng on 2015/5/29
 */
public class SubmitHomeWorkActivity extends Activity implements View.OnClickListener {

    //返回
    private ImageButton title_back;
    private TextView textView_homework_back;


    private PullToRefreshListView listView_submit;
    private ListView listView_fresh;

    private GetLessonHomework getLessonHomework;
    private HomeWorkAdapter adapter;
    private ImageResizer imageResizer;
    private List<HomeWorkResult> stuResultList;

    private int lessonId;//需要传进来

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_home_work);

        initView();
    }

    /**
     * 初始化数据
     */
    private void initView () {
        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_homework_back = (TextView) findViewById(R.id.textView_homework_back);

        listView_submit = (PullToRefreshListView) findViewById(R.id.listView_submit);
        listView_fresh = listView_submit.getRefreshableView();
        listView_fresh.addFooterView(buildHeader());//为listview的后面添加button

        imageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        if (adapter == null) {
            adapter = new HomeWorkAdapter(this,stuResultList,imageResizer);
        }

        listView_fresh.setAdapter(adapter);

        listView_submit.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                fillListView();
                listView_submit.onRefreshComplete();
            }
        });

        listView_submit.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
            @Override
            public void onLastItemVisible() {
                listView_submit.onRefreshComplete();
            }
        });

        title_back.setOnClickListener(this);
        textView_homework_back.setOnClickListener(this);

    }


    private void fillListView(){
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int status = LessonWebServerIF.getInstance(SubmitHomeWorkActivity.this).getLessonHomeWork(lessonId, getLessonHomework);
                return status;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == 1) {//成功
//                    stuResultList = getLessonHomework.stuResultList;
                    stuResultList.clear();
                    stuResultList.addAll(getLessonHomework.stuResultList);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SubmitHomeWorkActivity.this,"网络请求失败",Toast.LENGTH_SHORT).show();
                }

            }

        });
    }


    private View buildHeader() {
        Button btn=new Button(this);


        if (PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
            btn.setText("去评分!");
        } else {
            btn.setText("修改作业");
        }

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (PrefUtil.getInstance(SubmitHomeWorkActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
                    //跳转到老师评分
                    Intent intent = new Intent(SubmitHomeWorkActivity.this,HomeWorkEvaluate.class);
                    //传一些参数
                    startActivity(intent);
                } else {
                   //跳转到学生修改页面
                }

            }
        });

        return(btn);
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
}



