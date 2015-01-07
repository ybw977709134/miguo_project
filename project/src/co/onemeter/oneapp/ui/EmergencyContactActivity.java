package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.LocationHelper;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-8-26
 * Time: 下午4:33
 * To change this template use File | Settings | File Templates.
 */
public class EmergencyContactActivity extends Activity implements View.OnClickListener{
    private final static int EMERGENCY_STATUS_LEVEL_SAFE=0x01;
    private final static int EMERGENCY_STATUS_LEVEL_NEED_HELP=0x02;
    private final static int EMERGENCY_STATUS_LEVEL_SAFE_AT_HOME=0x04;
    private final static int EMERGENCY_STATUS_LEVEL_AT_EVACUATION_AREA=0x08;

    private MessageBox mMsgBox;
    private LocationHelper locationHelper;

    private Location myLocation;
    private String   myAddress;

    private WowTalkWebServerIF mWeb;

    private ImageView ivOptionSelectIndSafe;
    private ImageView ivOptionSelectIndNeedHelp;
    private ImageView ivOptionSelectIndSafeAtHome;
    private ImageView ivOptionSelectIndAtEvacuationArea;

    private ArrayList<Integer> selectedOptionList=new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emergency_contact_layout);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mWeb = WowTalkWebServerIF.getInstance(this);

        mMsgBox = new MessageBox(this);

        locationHelper = new LocationHelper(this);
        locationHelper.setOnLocationGotListener(new LocationHelper.OnLocationGotListener() {
            @Override
            public void onLocationGot(Location location, String strAddress) {
                Log.i("location with ["+location.getLatitude()+","+location.getLongitude()+"]:"+strAddress);
                myLocation=location;
                myAddress=strAddress;

//                TextView tvLocationTitle=(TextView)findViewById(R.id.tv_location_title);
                TextView tvLocation=(TextView)findViewById(R.id.location_indicator);

                findViewById(R.id.location_layout).setVisibility(View.GONE);

//                tvLocationTitle.setText(getString(R.string.input_location)+":");
                tvLocation.setText(getString(R.string.latitude)+": "+location.getLatitude()+"\n"+
                        getString(R.string.Longitude)+": "+location.getLongitude());
            }

            @Override
            public void onNoLocationGot() {
                Log.w("no location got");
                myLocation=null;
                myAddress=null;

                findViewById(R.id.location_layout).setVisibility(View.GONE);
            }
        });

        setView();
    }

    @Override
    public void onResume() {
        super.onResume();

        locationHelper.getLocationWithAMap(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        locationHelper.stop();
    }

    private void setView() {
        findViewById(R.id.title_back).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);

        ivOptionSelectIndSafe=(ImageView) findViewById(R.id.iv_ind_emergency_option_ok);
        ivOptionSelectIndNeedHelp=(ImageView) findViewById(R.id.iv_ind_emergency_option_need_help);
        ivOptionSelectIndSafeAtHome=(ImageView) findViewById(R.id.iv_ind_emergency_option_safe_at_home);
        ivOptionSelectIndAtEvacuationArea=(ImageView) findViewById(R.id.iv_ind_emergency_option_at_evacuation_area);

        findViewById(R.id.emergency_option_ok).setOnClickListener(this);
        findViewById(R.id.emergency_option_need_help).setOnClickListener(this);
        findViewById(R.id.emergency_option_safe_at_home).setOnClickListener(this);
        findViewById(R.id.emergency_option_at_evacuation_area).setOnClickListener(this);

        findViewById(R.id.emergency_option_need_help).performClick();
    }

    private void optionClickHandle(int optionIdx) {
        if(selectedOptionList.contains(optionIdx)) {
            selectedOptionList.remove(Integer.valueOf(optionIdx));
        } else {
            selectedOptionList.add(optionIdx);
        }

        switch (optionIdx) {
            case EMERGENCY_STATUS_LEVEL_SAFE:
                if(selectedOptionList.contains(optionIdx)) {
                    ivOptionSelectIndSafe.setImageResource(R.drawable.timeline_checked);
                } else {
                    ivOptionSelectIndSafe.setImageResource(R.drawable.timeline_unchecked);
                }
                break;
            case EMERGENCY_STATUS_LEVEL_NEED_HELP:
                if(selectedOptionList.contains(optionIdx)) {
                    ivOptionSelectIndNeedHelp.setImageResource(R.drawable.timeline_checked);
                } else {
                    ivOptionSelectIndNeedHelp.setImageResource(R.drawable.timeline_unchecked);
                }
                break;
            case EMERGENCY_STATUS_LEVEL_SAFE_AT_HOME:
                if(selectedOptionList.contains(optionIdx)) {
                    ivOptionSelectIndSafeAtHome.setImageResource(R.drawable.timeline_checked);
                } else {
                    ivOptionSelectIndSafeAtHome.setImageResource(R.drawable.timeline_unchecked);
                }
                break;
            case EMERGENCY_STATUS_LEVEL_AT_EVACUATION_AREA:
                if(selectedOptionList.contains(optionIdx)) {
                    ivOptionSelectIndAtEvacuationArea.setImageResource(R.drawable.timeline_checked);
                } else {
                    ivOptionSelectIndAtEvacuationArea.setImageResource(R.drawable.timeline_unchecked);
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.btn_send:
                sendEmergencyStatus();
                break;
            case R.id.emergency_option_ok:
                optionClickHandle(EMERGENCY_STATUS_LEVEL_SAFE);
                break;
            case R.id.emergency_option_need_help:
                optionClickHandle(EMERGENCY_STATUS_LEVEL_NEED_HELP);
                break;
            case R.id.emergency_option_safe_at_home:
                optionClickHandle(EMERGENCY_STATUS_LEVEL_SAFE_AT_HOME);
                break;
            case R.id.emergency_option_at_evacuation_area:
                optionClickHandle(EMERGENCY_STATUS_LEVEL_AT_EVACUATION_AREA);
                break;
        }
    }

    private int getEmergencyLevel() {
        int level=0;

        for(int option : selectedOptionList) {
            level |= option;
        }

        if(0 == level) {
            level |= EMERGENCY_STATUS_LEVEL_NEED_HELP;
        }

        return level;
    }

    private void sendEmergencyStatus() {
        mMsgBox.showWait();

        final int emergencyLevel=getEmergencyLevel();

        EditText emergencyDesc=(EditText) findViewById(R.id.emergency_detail_msg);
        final String detailMsg=emergencyDesc.getText().toString();

        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Log.w("emergency send with level "+emergencyLevel+" ,msg "+detailMsg);
                String companyId = PrefUtil.getInstance(EmergencyContactActivity.this).getCompanyId();
                int ret;
                if(null == myLocation) {
                    ret=mWeb.sendEmergencyMsg(companyId,emergencyLevel,detailMsg,0,0);
                } else {
                    ret=mWeb.sendEmergencyMsg(companyId,emergencyLevel,detailMsg,myLocation.getLatitude(),myLocation.getLongitude());
                }

                return ret;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                mMsgBox.dismissWait();
                if(0 != errno) {
                    mMsgBox.toast(R.string.emergency_send_fail);
                } else {
                    if((emergencyLevel & EMERGENCY_STATUS_LEVEL_NEED_HELP) != 0) {
                        mMsgBox.toast(R.string.emergency_send_success_with_help);
                    } else {
                        mMsgBox.toast(R.string.emergency_send_success_with_no_help);
                    }
                }
            }
        }.execute((Void)null);
    }
}
