package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wowtalk.api.PrefUtil;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 14-2-11
 * Time: 下午1:42
 * To change this template use File | Settings | File Templates.
 */
public class SysNoticeSetting extends Activity implements View.OnClickListener{
    private ImageButton titleBackBtn;
    private ImageView ivSysNoticeEnable;
    private ImageView ivMusic;
    private ImageView ivVibrate;
    private ImageView ivNewMessage;
    private ImageView ivTelephone;
    private RelativeLayout rlOthers;
    private TextView tvOtherSevice;

    private PrefUtil mPrefUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setContentView(R.layout.sys_notice_layout);

        mPrefUtil = PrefUtil.getInstance(this);

        initView();
    }

    private void initView() {
        titleBackBtn=(ImageButton) findViewById(R.id.title_back);
        ivSysNoticeEnable=(ImageView) findViewById(R.id.iv_sys_notice_enable);
        ivMusic=(ImageView) findViewById(R.id.iv_sys_notice_music);
        ivVibrate=(ImageView) findViewById(R.id.iv_sys_notice_vibrate);
        ivNewMessage=(ImageView) findViewById(R.id.iv_sys_notice_new_message);
        ivTelephone=(ImageView) findViewById(R.id.iv_sys_notice_telephone);
        rlOthers=(RelativeLayout) findViewById(R.id.other_service_layout);
        tvOtherSevice=(TextView) findViewById(R.id.tv_other_service_set);

        titleBackBtn.setOnClickListener(this);
        ivSysNoticeEnable.setOnClickListener(this);

        updateSysNoticeStatus();
    }

    public static void setOnItemStatus(ImageView iv2set,boolean isSet) {
        if(null != iv2set) {
            if(isSet) {
                iv2set.setImageResource(R.drawable.switch_on_little);
            } else {
                iv2set.setImageResource(R.drawable.switch_off_little);
            }
        }
    }

    private void updateSysNoticeStatus() {
        if(mPrefUtil.isSysNoticeEnabled()) {
            ivSysNoticeEnable.setImageResource(R.drawable.switch_on_little);
            setOnItemStatus(ivMusic,mPrefUtil.isSysNoticeMusicOn());
            setOnItemStatus(ivVibrate,mPrefUtil.isSysNoticeVibrateOn());
            setOnItemStatus(ivNewMessage,mPrefUtil.isSysNoticeNewMessageOn());
            setOnItemStatus(ivTelephone,mPrefUtil.isSysNoticeTelephoneOn());

            ivMusic.setOnClickListener(this);
            ivVibrate.setOnClickListener(this);
            ivNewMessage.setOnClickListener(this);
            ivTelephone.setOnClickListener(this);
            rlOthers.setOnClickListener(this);
            tvOtherSevice.setTextColor(getResources().getColor(R.color.black));
        } else {
            ivSysNoticeEnable.setImageResource(R.drawable.switch_off_little);
            ivMusic.setImageResource(R.drawable.switch_off_little);
            ivVibrate.setImageResource(R.drawable.switch_off_little);
            ivNewMessage.setImageResource(R.drawable.switch_off_little);
            ivTelephone.setImageResource(R.drawable.switch_off_little);

            ivMusic.setOnClickListener(null);
            ivVibrate.setOnClickListener(null);
            ivNewMessage.setOnClickListener(null);
            ivTelephone.setOnClickListener(null);
            rlOthers.setOnClickListener(null);
            tvOtherSevice.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_sys_notice_music:
                mPrefUtil.setSysNoticeMusicOn(!mPrefUtil.isSysNoticeMusicOn());
                updateSysNoticeStatus();
                break;
            case R.id.iv_sys_notice_vibrate:
                mPrefUtil.setSysNoticeVibrateOn(!mPrefUtil.isSysNoticeVibrateOn());
                updateSysNoticeStatus();
                break;
            case R.id.iv_sys_notice_new_message:
                mPrefUtil.setSysNoticeNewMessageOn(!mPrefUtil.isSysNoticeNewMessageOn());
                updateSysNoticeStatus();
                break;
            case R.id.iv_sys_notice_telephone:
                mPrefUtil.setSysNoticeTelephoneOn(!mPrefUtil.isSysNoticeTelephoneOn());
                updateSysNoticeStatus();
                break;
            case R.id.other_service_layout:
                Intent intent = new Intent();
                intent.setClass(this, SysNoticeForOthersSetting.class);
                startActivity(intent);
                break;
            case R.id.iv_sys_notice_enable:
                mPrefUtil.setSysNoticeEnabled(!mPrefUtil.isSysNoticeEnabled());
                updateSysNoticeStatus();
                break;
            case R.id.title_back:
                finish();
                break;
            default:
                Log.e("unknown handle for id "+view.getId());
                break;
        }
    }
}
