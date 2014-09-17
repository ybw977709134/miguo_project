package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.wowtalk.api.PrefUtil;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 14-2-11
 * Time: 下午3:34
 * To change this template use File | Settings | File Templates.
 */
public class SysNoticeForOthersSetting extends Activity implements View.OnClickListener {
    private PrefUtil mPrefUtil;
    private ImageButton titleBackBtn;
    private ImageView ivSysNoticeAllMoment;
    private ImageView ivSysNoticeFrequentContactsMoment;
    private ImageView ivSysNoticeTimeline;
    private ImageView ivSysNoticeQa;
    private ImageView ivSysNoticeShare;
    private ImageView ivSysNoticeVote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setContentView(R.layout.sys_notice_others_layout);

        mPrefUtil = PrefUtil.getInstance(this);

        initView();
    }

    private void initView() {
        titleBackBtn=(ImageButton) findViewById(R.id.title_back);

        ivSysNoticeAllMoment=(ImageView) findViewById(R.id.iv_sys_notice_all_moment);
        ivSysNoticeFrequentContactsMoment=(ImageView) findViewById(R.id.iv_sys_notice_frequent_contact_moment);
        ivSysNoticeTimeline=(ImageView) findViewById(R.id.iv_sys_notice_timeline);
        ivSysNoticeQa=(ImageView) findViewById(R.id.iv_sys_notice_qa);
        ivSysNoticeShare=(ImageView) findViewById(R.id.iv_sys_notice_share);
        ivSysNoticeVote=(ImageView) findViewById(R.id.iv_sys_notice_vote);

        titleBackBtn.setOnClickListener(this);
        ivSysNoticeAllMoment.setOnClickListener(this);
        ivSysNoticeFrequentContactsMoment.setOnClickListener(this);


        updateStatus();
    }

    private void updateStatus() {
        boolean allMomentOn=mPrefUtil.isSysNoticeAllMomentOn();
        boolean frequentContactsMomentOn=mPrefUtil.isSysNoticeFrequentContactsMomentOn();
        SysNoticeSetting.setOnItemStatus(ivSysNoticeAllMoment,allMomentOn);
        SysNoticeSetting.setOnItemStatus(ivSysNoticeFrequentContactsMoment,frequentContactsMomentOn);

        if(allMomentOn || frequentContactsMomentOn) {
            SysNoticeSetting.setOnItemStatus(ivSysNoticeTimeline,mPrefUtil.isSysNoticeMomentTimelineOn());
            SysNoticeSetting.setOnItemStatus(ivSysNoticeQa,mPrefUtil.isSysNoticeMomentQAOn());
            SysNoticeSetting.setOnItemStatus(ivSysNoticeShare,mPrefUtil.isSysNoticeMomentShareOn());
            SysNoticeSetting.setOnItemStatus(ivSysNoticeVote,mPrefUtil.isSysNoticeMomentVoteOn());

            ivSysNoticeTimeline.setOnClickListener(this);
            ivSysNoticeQa.setOnClickListener(this);
            ivSysNoticeShare.setOnClickListener(this);
            ivSysNoticeVote.setOnClickListener(this);

            if(allMomentOn) {
                ivSysNoticeFrequentContactsMoment.setOnClickListener(null);
            } else {
                ivSysNoticeFrequentContactsMoment.setOnClickListener(this);
            }
        } else {
            ivSysNoticeTimeline.setImageResource(R.drawable.switch_off_little);
            ivSysNoticeQa.setImageResource(R.drawable.switch_off_little);
            ivSysNoticeShare.setImageResource(R.drawable.switch_off_little);
            ivSysNoticeVote.setImageResource(R.drawable.switch_off_little);

            ivSysNoticeTimeline.setOnClickListener(null);
            ivSysNoticeQa.setOnClickListener(null);
            ivSysNoticeShare.setOnClickListener(null);
            ivSysNoticeVote.setOnClickListener(null);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_sys_notice_all_moment:
                mPrefUtil.setSysNoticeAllMomentOn(!mPrefUtil.isSysNoticeAllMomentOn());
                if(mPrefUtil.isSysNoticeAllMomentOn()) {
                    mPrefUtil.setSysNoticeFrequentContactsMomentOn(true);
                }
                updateStatus();
                break;
            case R.id.iv_sys_notice_frequent_contact_moment:
                mPrefUtil.setSysNoticeFrequentContactsMomentOn(!mPrefUtil.isSysNoticeFrequentContactsMomentOn());
                updateStatus();
                break;
            case R.id.iv_sys_notice_timeline:
                mPrefUtil.setSysNoticeMomentTimelineOn(!mPrefUtil.isSysNoticeMomentTimelineOn());
                updateStatus();
                break;
            case R.id.iv_sys_notice_qa:
                mPrefUtil.setSysNoticeMomentQAOn(!mPrefUtil.isSysNoticeMomentQAOn());
                updateStatus();
                break;
            case R.id.iv_sys_notice_share:
                mPrefUtil.setSysNoticeMomentShareOn(!mPrefUtil.isSysNoticeMomentShareOn());
                updateStatus();
                break;
            case R.id.iv_sys_notice_vote:
                mPrefUtil.setSysNoticeMomentVoteOn(!mPrefUtil.isSysNoticeMomentVoteOn());
                updateStatus();
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
