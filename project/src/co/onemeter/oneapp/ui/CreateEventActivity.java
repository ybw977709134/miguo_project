package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.Utils;
import co.onemeter.oneapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CreateEventActivity extends Activity implements OnClickListener {
    private boolean isAllDay = false;
    private boolean allowReview = false;
    private boolean needToUpdateMulti = false;

    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;

    private TextView txtTitle;
    private TextView txtLoc;
    private TextView txtStartTime;
    private TextView txtEndTime;
    private TextView txtRepeat;
    private TextView txtOpenLevel;
    private TextView txtTargetUser;
    private TextView txtCapacity;
    private TextView txtCoins;
    private TextView txtAddPhoto;
    private TextView txtAddAudio;

    private ImageView imgAllDay;
    private ImageView imgAllowReview;
    private ImageView imgNeedToUpdateMulti;

    private EditText edtContent;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);

        txtTitle = (TextView) findViewById(R.id.txt_title);
        txtLoc = (TextView) findViewById(R.id.txt_loc);
        txtStartTime = (TextView) findViewById(R.id.txt_start_time);
        txtEndTime = (TextView) findViewById(R.id.txt_end_time);
        txtRepeat = (TextView) findViewById(R.id.txt_repeat);
        txtOpenLevel = (TextView) findViewById(R.id.txt_open_level);
        txtTargetUser = (TextView) findViewById(R.id.txt_targetuser);
        txtCapacity = (TextView) findViewById(R.id.txt_capacity);
        txtCoins = (TextView) findViewById(R.id.txt_coins);
        txtAddPhoto = (TextView) findViewById(R.id.txt_addphoto);
        txtAddAudio = (TextView) findViewById(R.id.txt_addaudio);


        imgAllDay = (ImageView) findViewById(R.id.img_allday);
        imgAllowReview = (ImageView) findViewById(R.id.img_allowreview);
        imgNeedToUpdateMulti = (ImageView) findViewById(R.id.img_needtoupdatemulti);

        btnTitleBack.setOnClickListener(this);
        btnTitleConfirm.setOnClickListener(this);

        imgAllDay.setOnClickListener(this);
        imgAllowReview.setOnClickListener(this);
        imgNeedToUpdateMulti.setOnClickListener(this);

        findViewById(R.id.layout_title).setOnClickListener(this);
        findViewById(R.id.layout_loc).setOnClickListener(this);
        findViewById(R.id.layout_starttime).setOnClickListener(this);
        findViewById(R.id.layout_endtime).setOnClickListener(this);
        findViewById(R.id.layout_repeat).setOnClickListener(this);
        findViewById(R.id.layout_openlevel).setOnClickListener(this);
        findViewById(R.id.layout_targetuser).setOnClickListener(this);
        findViewById(R.id.layout_capacity).setOnClickListener(this);
        findViewById(R.id.layout_coins).setOnClickListener(this);
        findViewById(R.id.layout_addphoto).setOnClickListener(this);
        findViewById(R.id.layout_addaudio).setOnClickListener(this);
    }

    private void createEvent() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.title_confirm:
                break;
            case R.id.img_allday:
                isAllDay = !isAllDay;
                imgAllDay.setImageResource(isAllDay ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.img_allowreview:
                allowReview = !allowReview;
                imgAllowReview.setImageResource(allowReview ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.img_needtoupdatemulti:
                needToUpdateMulti = !needToUpdateMulti;
                imgNeedToUpdateMulti.setImageResource(needToUpdateMulti ? R.drawable.switch_on : R.drawable.switch_off);
                break;
            case R.id.layout_title:
                break;
            case R.id.layout_loc:
                break;
            case R.id.layout_starttime:
                break;
            case R.id.layout_endtime:
                break;
            case R.id.layout_repeat:
                break;
            case R.id.layout_openlevel:
                break;
            case R.id.layout_targetuser:
                break;
            case R.id.layout_capacity:
                break;
            case R.id.layout_coins:
                break;
            case R.id.layout_addphoto:
                break;
            case R.id.layout_addaudio:
                break;
            default:
                break;
        }
    }
}
