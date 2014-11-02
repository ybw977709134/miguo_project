package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WEvent;
import org.wowtalk.api.WowEventWebServerIF;

public class CreateEventActivity extends Activity implements OnClickListener {
    public static final String EXTRA_PAGE_TITLE = "page_title";
    public static final String EXTRA_EVENT_CATEGORY = "event_cat";

    private static final int REQ_INPUT_TITLE = 123;
    private static final int REQ_INPUT_CATEGORY = 124;
    private static final int REQ_INPUT_TIME = 125;
    private static final int REQ_INPUT_PLACE = 126;
    private static final int REQ_INPUT_DESC = 127;

    private WEvent wevent = new WEvent();

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

    private void initView(Bundle bundle) {
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

        if (bundle != null) {
            setTitle(bundle.getString(EXTRA_PAGE_TITLE));
        } else {
            setTitle("");
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        new AQuery(this).find(R.id.title_text).text(title);
    }

    private void createEvent() {
        updateData();
        new AsyncTask<WEvent, Void, Integer>() {
            Context context;

            @Override
            protected Integer doInBackground(WEvent... wEvents) {
                context = CreateEventActivity.this;
                WowEventWebServerIF web = WowEventWebServerIF.getInstance(context);
                return web.fAdd(wEvents[0]);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    finish();
                } else {
                    Toast.makeText(context, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(wevent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Bundle b = savedInstanceState == null ? getIntent().getExtras() : savedInstanceState;
        if (b != null) {
            wevent.category = b.getString(EXTRA_EVENT_CATEGORY);
        }

        initView(b);
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
                createEvent();
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
            case R.id.layout_title: {
                startActivityForResult(
                        new Intent(this, InputPlainTextActivity.class)
                                .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_title)),
                        REQ_INPUT_TITLE);
                break;
            }
            case R.id.layout_loc:
                startActivityForResult(
                        new Intent(this, InputPlainTextActivity.class)
                                .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.event_loc)),
                        REQ_INPUT_PLACE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(resultCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQ_INPUT_TITLE:
                wevent.title = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
            case REQ_INPUT_DESC:
                wevent.description = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
            case REQ_INPUT_PLACE:
                wevent.address = data.getStringExtra(InputPlainTextActivity.EXTRA_VALUE);
                updateUI();
                break;
        }
    }

    private void updateUI() {
        if (wevent == null)
            return;

        txtTitle.setText(wevent.title);
        txtLoc.setText(wevent.description);
        txtLoc.setText(wevent.address);
    }

    private void updateData() {
        if (wevent == null)
            return;

        AQuery q = new AQuery(this);
        wevent.description = q.find(R.id.edt_content).getText().toString();
    }
}
