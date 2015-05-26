package co.onemeter.oneapp.ui.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import co.onemeter.oneapp.R;

public class HomeWorkEvaluate extends Activity implements View.OnClickListener {
    private ImageButton title_back;//返回
    private TextView textView_homework_back;//返回
    private TextView textView_homework_send;//发送

    RatingBar ratingBar_confirm;//完整性
    RatingBar ratingBar_timely;//及时性
    RatingBar ratingBar_exact;//准确性

    private Button btn_comment;//评语模板
    private EditText editText_comment;//评语


    //星星评分对应的数值
    private float confirmNum;
    private float timelyNum;
    private float exactNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_work_evaluate);
    }


    private void initView() {
        title_back = (ImageButton) findViewById(R.id.title_back);
        textView_homework_back = (TextView) findViewById(R.id.textView_homework_back);
        textView_homework_send = (TextView) findViewById(R.id.textView_homework_send);

        ratingBar_confirm = (RatingBar) findViewById(R.id.ratingBar_confirm);
        ratingBar_timely = (RatingBar) findViewById(R.id.ratingBar_timely);
        ratingBar_exact = (RatingBar) findViewById(R.id.ratingBar_exact);

        btn_comment = (Button) findViewById(R.id.btn_comment);
        editText_comment = (EditText) findViewById(R.id.editText_comment);


        //为各个控件添加监听事件
        title_back.setOnClickListener(this);
        textView_homework_back.setOnClickListener(this);
        textView_homework_send.setOnClickListener(this);
        ratingBar_confirm.setOnRatingBarChangeListener(new RatingBarListener());
        ratingBar_timely.setOnRatingBarChangeListener(new RatingBarListener());
        ratingBar_exact.setOnRatingBarChangeListener(new RatingBarListener());

        btn_comment.setOnClickListener(this);

    }

    private class RatingBarListener implements RatingBar.OnRatingBarChangeListener{

        @Override
        public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
            switch (ratingBar.getId()) {
                case R.id.ratingBar_confirm://完整性
                    confirmNum = rating;
                    break;

                case R.id.ratingBar_timely://及时性
                    timelyNum = rating;
                    break;

                case R.id.ratingBar_exact://准确性
                    exactNum = rating;
                    break;
            }

        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back://返回
            case R.id.textView_homework_back:
                finish();
                break;


            case R.id.textView_homework_send://发送

                break;

            case R.id.btn_comment://获得模板

                break;

        }
    }
}
