package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.CreateSurveyOptionsLeftDeleteAdapter;
import co.onemeter.oneapp.adapter.CreateSurveyOptionsRightContentAdapter;
import co.onemeter.oneapp.utils.LocationHelper;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;
import co.onemeter.utils.AsyncTaskExecutor;
import junit.framework.Assert;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-26
 * Time: 下午2:05
 * To change this template use File | Settings | File Templates.
 */
public class CreateNormalMomentWithTagActivity extends Activity implements View.OnClickListener, InputBoardManager.ChangeToOtherAppsListener{

    public static final int MOMENTS_WORDS_OVER = 600;

    /**
     * 输入 Moment 对象（可选），或在
     * 发布成功后，在 Activity Result 中输出 Moment 对象。
     */
    public static final String EXTRA_MOMENT = "moment";

    private int tagType;

    private TextView tvShareRange;

    private MediaInputHelper mediaHelper;
    private MessageBox mMsgBox;
    private Database mDb;

    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA=1;
    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY=2;
    private final static int ACTIVITY_REQ_ID_SHARE_RANGE_SELECT=3;
    private static final int ACTIVITY_REQ_ID_INPUT_VIDEO = 4;

    public final static int PHOTO_THUMBNAIL_WIDTH = 180;
    public final static int PHOTO_THUMBNAIL_HEIGHT = 120;

    private ArrayList<CreateMomentActivity.WMediaFile> listPhotoOrVideo;

    private static CreateNormalMomentWithTagActivity instance;

    private LinearLayout addedImgLayout;

    private Moment moment;
    private File mLastVoiceFile;

    private HorizontalScrollView hsvImgList;
    private boolean isCapturingVoice=false;
    private ImageView ivCaptureInnerInd;
    private TextView  tvCaptureInnerInd;

    private ImageView ivReadyCaptureVoicePlay;
    private TextView  tvReadyCaptureVoiceTimeLength;

    private ImageView ivPickLocationImgInd;
    private TextView  tvPickLocationTxtInd;

    private MediaRecorder mMediaRecorder;
//    private MediaPlayer mPlayer;
    private TimeElapseReportRunnable timeElapseReportForCaptureVoiceRunnable;
//    private TimeElapseReportRunnable playingVoiceRunnable;

    private LocationHelper locationHelper;
    private boolean isGettingLocation=false;

    private EditText etMomentMsgContent;

    private TextView tvSurveyNoOptionsInd;

    private ArrayList<String> surveyOptions;
    private RelativeLayout rlSurveyOptionsLayout;
    private ListView lvSurveyOptionsLeftDelete;
    private ListView lvSurveyOptionsContent;
    private CreateSurveyOptionsLeftDeleteAdapter optionDeleteAdapter;
    private CreateSurveyOptionsRightContentAdapter optionContentAdapter;

    private Button btnCreateSurveyOption;
    private ImageButton ibCanSurveyMultiSelect;

    private RelativeLayout rlSurveyVoteDeadLine;
    private TextView tvSurveyVoteDeadLine;
//    private String surveyDeadLineTime;

    private MediaPlayerWraper mediaPlayerWraper;
//    private TimeElapseReportRunnable rightBtnStatusRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_moment_with_tag_layout);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initData(savedInstanceState);

        initView(savedInstanceState);

        //注销掉对发送按钮的服务监听
//        rightBtnStatusRunnable=new TimeElapseReportRunnable();
//        rightBtnStatusRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
//            @Override
//            public void reportElapse(final long elapsed) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        checkRightBtnStatus();
//                    }
//                });
//            }
//        });
//        new Thread(rightBtnStatusRunnable).start();
    }

    // 初始化 Moment 对象
    private void initData(Bundle savedInstanceState) {
        // source #1, Activity argument
        moment = getIntent().getParcelableExtra(EXTRA_MOMENT);

        if (savedInstanceState != null) {
            if (moment == null)
                moment = savedInstanceState.getParcelable(EXTRA_MOMENT);
            mediaHelper = savedInstanceState.getParcelable("media_helper");
            listPhotoOrVideo = savedInstanceState.getParcelableArrayList("list_photo");
            surveyOptions = savedInstanceState.getStringArrayList("survey_options");

            String lastVoiceFilePath = savedInstanceState.getString("last_voice_file");
            if (!TextUtils.isEmpty(lastVoiceFilePath)) {
                File aFile = new File(lastVoiceFilePath);
                if (null != aFile && aFile.exists()) {
                    mLastVoiceFile = aFile;
                }
            }
        }

        if (moment == null)
            moment = new Moment();

        if (mediaHelper == null)
            mediaHelper = new MediaInputHelper(this);

        if (listPhotoOrVideo == null)
            listPhotoOrVideo = new ArrayList<>();

        if (surveyOptions == null)
            surveyOptions = new ArrayList<>();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlayingVoice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationHelper.setOnLocationGotListener(null);
        locationHelper.stop();
        recycleStoredBitmapDrawable();

//        if(null != rightBtnStatusRunnable) {
//            rightBtnStatusRunnable.stop();
//        }

        if(null != mMediaRecorder) {
            stopRecording();
        }

        Log.w("create moment destroyed");
    }

    private final static int MIN_OPTIONS_NUM_FOR_SURVEY=2;
    private boolean isSurveyValid() {
        int validSurveyNumber=0;
        for(String opt : surveyOptions) {
            if(!TextUtils.isEmpty(opt)) {
                ++validSurveyNumber;
            }
        }
        if(TimelineActivity.TAG_SURVEY_IDX==tagType && validSurveyNumber<MIN_OPTIONS_NUM_FOR_SURVEY) {
            return false;
        }

        return true;
    }

    private boolean isContentValid() {
        if ((Utils.isNullOrEmpty(etMomentMsgContent.getText().toString())
                && (listPhotoOrVideo == null || listPhotoOrVideo.isEmpty())
                && (mLastVoiceFile == null || !mLastVoiceFile.exists())
                && (0==moment.latitude && 0 == moment.longitude)) ||
             (null != mMediaRecorder && voiceDuration<MOMENT_VOICE_MIN_LEN_IN_MS)) {
            return false;
        }

        return true;
    }
    
    private boolean isGetingLocation()
    {
    	return isGettingLocation;
    }

    private void checkRightBtnStatus() {
        View rightConfirm= findViewById(R.id.title_moment_send);

        if (isContentValid() && isSurveyValid() && !isGetingLocation()) {
            rightConfirm.setEnabled(true);
        } else {
            rightConfirm.setEnabled(false);
        }
    }

    private void updateLocation(double latitude,double longitude,String strAddress) {
        moment.place = strAddress;
        moment.latitude = latitude;
        moment.longitude = longitude;

        tvPickLocationTxtInd.setText(strAddress);
        ivPickLocationImgInd.setImageResource(R.drawable.timeline_location_a);
        findViewById(R.id.pick_location_layout).setBackgroundResource(R.drawable.text_field);
        findViewById(R.id.pick_location_delete).setVisibility(View.VISIBLE);

        isGettingLocation=false;
    }
  
    LocationHelper.OnLocationGotListener mOnLocationGotListener = new LocationHelper.OnLocationGotListener() {
        @Override
        public void onLocationGot(Location location, String strAddress) {
            try {
                updateLocation(location.getLatitude(),location.getLongitude(),strAddress);
                locationHelper.setOnLocationGotListener(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            moment.place = strAddress;
//            if (null != location) {
//                moment.latitude = location.getLatitude();
//                moment.longitude = location.getLongitude();
//            }
//
//            tvPickLocationTxtInd.setText(String.format(getString(R.string.event_place),strAddress));
//            ivPickLocationImgInd.setImageResource(R.drawable.timeline_location_a);
//            findViewById(R.id.pick_location_layout).setBackgroundResource(R.drawable.text_field);
//            findViewById(R.id.pick_location_delete).setVisibility(View.VISIBLE);
//
//            isGettingLocation=false;
        }

        @Override
        public void onNoLocationGot() {
//            Location tLocation=new Location("ttt");
//            tLocation.setLongitude(10);
//            tLocation.setLatitude(20);
//            onLocationGot(tLocation,"ttt");
            moment.place = null;
            moment.latitude = moment.longitude = 0;
            mMsgBox.toast(R.string.moments_create_obtain_location_failed);

            tvPickLocationTxtInd.setText(R.string.place);
            isGettingLocation=false;
            locationHelper.setOnLocationGotListener(null);
        }
    };
    
    private void initView(Bundle savedInstanceState) {
        instance=this;

        mediaPlayerWraper=new MediaPlayerWraper(this);

        locationHelper = new LocationHelper(this);

        etMomentMsgContent=(EditText) findViewById(R.id.edt_moment_content);
        addedImgLayout=(LinearLayout) findViewById(R.id.added_images_layout);
        hsvImgList=(HorizontalScrollView) findViewById(R.id.hsv_img_list);

        mMsgBox = new MessageBox(this);
        mDb = new Database(this);

        tagType=getIntent().getIntExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_TAG_ID,TimelineActivity.TAG_NOTICE_IDX);
        switch (tagType) {
        case TimelineActivity.TAG_NOTICE_IDX:
            etMomentMsgContent.setHint(R.string.moments_compose_hint_status);
            break;
        case TimelineActivity.TAG_QA_IDX:
            etMomentMsgContent.setHint(R.string.moments_compose_hint_qa);
            break;
        case TimelineActivity.TAG_SURVEY_IDX:
            etMomentMsgContent.setHint(R.string.moments_compose_hint_survey);
            break;
        case TimelineActivity.TAG_STUDY_IDX:
            etMomentMsgContent.setHint(R.string.moments_compose_hint_report);
            break;
        case TimelineActivity.TAG_VIDEO_IDX:
        	etMomentMsgContent.setHint(R.string.moments_compose_hint);
        	((TextView)findViewById(R.id.trigger_add_img_txt_desc)).setText(R.string.moments_compose_gallery_video);
        	findViewById(R.id.capture_voice_framelayout).setVisibility(View.GONE);
        	break;
        default:
            etMomentMsgContent.setHint(R.string.moments_compose_hint);
            break;
        }

        tvShareRange=(TextView) findViewById(R.id.tv_share_range);

        TextView tvTitle=(TextView) findViewById(R.id.title_txt);
        String tagName=TimelineActivity.getSelectedTagLocalDesc(this, tagType);
        tvTitle.setText(String.format(getString(R.string.new_moment_title),tagName));

        findViewById(R.id.title_back).setOnClickListener(this);
        findViewById(R.id.title_moment_send).setOnClickListener(this);
        findViewById(R.id.share_range_layout).setOnClickListener(this);

        findViewById(R.id.trigger_add_img_layout).setOnClickListener(this);


        ivCaptureInnerInd=(ImageView) findViewById(R.id.capture_inner_img_ind);
        tvCaptureInnerInd=(TextView) findViewById(R.id.capture_inner_txt_ind);

        ivReadyCaptureVoicePlay=(ImageView) findViewById(R.id.ready_captured_voice_play);
        tvReadyCaptureVoiceTimeLength=(TextView) findViewById(R.id.ready_captured_voice_time_length);

        ivPickLocationImgInd = (ImageView) findViewById(R.id.pick_location_img_ind);
        tvPickLocationTxtInd = (TextView) findViewById(R.id.pick_location_txt_ind);

        rlSurveyVoteDeadLine = (RelativeLayout) findViewById(R.id.survey_vote_dead_line_layout);
        tvSurveyVoteDeadLine = (TextView) findViewById(R.id.tv_survey_vote_dead_line);

//        Calendar calendar=Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_MONTH,1);
//        surveyDeadLineTime=Database.chatMessage_dateToUTCString(calendar.getTime());

        updateSurveyDeadLine();

        rlSurveyVoteDeadLine.setOnClickListener(this);

        findViewById(R.id.capture_voice_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_inner_left_layout).setOnClickListener(this);
        findViewById(R.id.ready_captured_voice_delete).setOnClickListener(this);

        findViewById(R.id.pick_location_layout).setOnClickListener(this);
        findViewById(R.id.pick_location_delete).setOnClickListener(this);

        if (mLastVoiceFile != null)
            updateGotVoice();

        if(null != moment) {
            try {
                if(!TextUtils.isEmpty(moment.text)) {
                    etMomentMsgContent.setText(moment.text);
                }
                if(moment.visibility() == Moment.VISIBVILITY_LIMITED) {
                    tvShareRange.setText(R.string.share_range_private);
                } else {
                    tvShareRange.setText(R.string.share_range_public);
                }

                if (moment.multimedias != null && !moment.multimedias.isEmpty()) {
                    listPhotoOrVideo = new ArrayList<>(moment.multimedias.size());
                    for (WFile f : moment.multimedias) {
                        if (f.isImageByExt() || f.isVideoByExt()) {
                            listPhotoOrVideo.add(new CreateMomentActivity.WMediaFile(f));
                        }
                    }
                } else if (listPhotoOrVideo == null) {
                    listPhotoOrVideo = new ArrayList<>();
                }

                if (moment.surveyOptions != null && !moment.surveyOptions.isEmpty()) {
                    surveyOptions = new ArrayList<>(moment.surveyOptions.size());
                    for (Moment.SurveyOption surveyOption : moment.surveyOptions) {
                        surveyOptions.add(surveyOption.optionDesc);
                    }
                } else {
                    surveyOptions = new ArrayList<>();
                }

                if(!TextUtils.isEmpty(moment.place)) {
                    updateLocation(moment.latitude, moment.longitude, moment.place);
                }

                notifyFileChanged(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(TimelineActivity.TAG_NOTICE_IDX == tagType || tagType == TimelineActivity.TAG_VIDEO_IDX) {
            findViewById(R.id.location_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.location_layout).setVisibility(View.GONE);
        }
        //if survey,set survey visible
        if(TimelineActivity.TAG_SURVEY_IDX == tagType) {
            findViewById(R.id.survey_layout).setVisibility(View.VISIBLE);
//            findViewById(R.id.location_layout).setVisibility(View.GONE);

            tvSurveyNoOptionsInd=(TextView) findViewById(R.id.tv_survey_no_options_ind);

            rlSurveyOptionsLayout=(RelativeLayout) findViewById(R.id.survey_options_layout);
            lvSurveyOptionsLeftDelete=(ListView) findViewById(R.id.survey_options_left_delete);
            lvSurveyOptionsContent=(ListView) findViewById(R.id.survey_options_right_content);

            btnCreateSurveyOption=(Button) findViewById(R.id.btn_create_option);
            btnCreateSurveyOption.setOnClickListener(this);

            ibCanSurveyMultiSelect=(ImageButton) findViewById(R.id.survey_can_multi_select);
            ibCanSurveyMultiSelect.setOnClickListener(this);

            updateSurveyMultiSelectState();

            if(surveyOptions.size() <= 0) {
                //add empty survey if none
                for(int i=0; i<MIN_OPTIONS_NUM_FOR_SURVEY; ++i) {
                    addASurveyOption();
                }
            } else {
                updateSurveyOptionsState();
            }

        }
    }

    private void updateSurveyMultiSelectState() {
        ibCanSurveyMultiSelect.setBackgroundResource(moment.isSurveyAllowMultiSelect ? R.drawable.switch_on_little : R.drawable.switch_off_little);
    }

    private void updateSurveyOptionsState() {
        if(surveyOptions.size() > 0) {
            tvSurveyNoOptionsInd.setVisibility(View.GONE);
            rlSurveyOptionsLayout.setVisibility(View.VISIBLE);

            //set list of left delete
            if(null == optionDeleteAdapter) {
                optionDeleteAdapter=new CreateSurveyOptionsLeftDeleteAdapter(this,new CreateSurveyOptionsLeftDeleteAdapter.OnOptionDeleteListener() {
                    @Override
                    public void onDelete(final int position) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                surveyOptions.remove(position);
                                updateSurveyOptionsState();
                            }
                        });
                    }
                });
            }
            optionDeleteAdapter.setCount(surveyOptions.size());
            lvSurveyOptionsLeftDelete.setAdapter(optionDeleteAdapter);
            ListHeightUtil.setListHeight(lvSurveyOptionsLeftDelete);

            //set list of right content
            if(null == optionContentAdapter) {
                optionContentAdapter=new CreateSurveyOptionsRightContentAdapter(this);
            }
            optionContentAdapter.setOptionsContent(surveyOptions);
            lvSurveyOptionsContent.setAdapter(optionContentAdapter);
            ListHeightUtil.setListHeight(lvSurveyOptionsContent);
        } else {
            tvSurveyNoOptionsInd.setVisibility(View.VISIBLE);
            tvSurveyNoOptionsInd.setText(String.format(getString(R.string.survey_option_at_least_2),MIN_OPTIONS_NUM_FOR_SURVEY));
            rlSurveyOptionsLayout.setVisibility(View.GONE);
        }
    }

    private void addASurveyOption() {
        surveyOptions.add("");
        updateSurveyOptionsState();
    }

//    private void showAddSurveyOptionPopup() {
//        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View contentView= lf.inflate(R.layout.survey_add_option_layout, null);
//
//        final PopupWindow popWindow = Utils.getFixedPopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//
//        final EditText tvASurveyOptionContent=(EditText)contentView.findViewById(R.id.a_survey_option_content);
//        Button btnASurveyOptionOk=(Button)contentView.findViewById(R.id.a_survey_option_ok);
//        Button btnASurveyOptionCancel=(Button)contentView.findViewById(R.id.a_survey_option_cancel);
//
//        contentView.findViewById(R.id.root_layout).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                popWindow.dismiss();
//            }
//        });
//
//        btnASurveyOptionOk.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String option=tvASurveyOptionContent.getText().toString();
//
//                if(TextUtils.isEmpty(option)) {
//                    mMsgBox.toast(String.format(getString(R.string.inputsimpletext_empty),getString(R.string.survey_option)));
//                } else {
//                    surveyOptions.add(option);
//                    updateSurveyOptionsState();
//
//                    popWindow.dismiss();
//                }
//            }
//        });
//        btnASurveyOptionCancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                popWindow.dismiss();
//            }
//        });
//
//        popWindow.setFocusable(true);
//        popWindow.setTouchable(true);
//        popWindow.setOutsideTouchable(true);
//        popWindow.setBackgroundDrawable(new BitmapDrawable());
//
//        popWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//
//        popWindow.showAtLocation(btnCreateSurveyOption, Gravity.CENTER, 0, 0);
//        popWindow.update();
//    }

    private void updateSurveyDeadLine() {
        if(moment.surveyDeadLine == Moment.SURVEY_DEADLINE_NO_LIMIT_VALUE) {
            Date date = new Date(moment.surveyDeadLine);

            SimpleDateFormat dateFormat= new SimpleDateFormat(getResources().getString(R.string.msg_date_format_with_year));
            tvSurveyVoteDeadLine.setText(dateFormat.format(date));
        } else {
            tvSurveyVoteDeadLine.setText(R.string.survey_dead_line_no_limit);
        }
    }

    private void showPickTimeLayout() {
        DatePickerDialog.OnDateSetListener dateListener =
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker,int year, int month, int dayOfMonth) {
                        Calendar curCalendar=new GregorianCalendar();

                        // 使用所选日期的23:59:59作为截止时间
                        Calendar endCalendar=new GregorianCalendar(year, month, dayOfMonth);
                        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                        endCalendar.set(Calendar.MINUTE, 59);
                        endCalendar.set(Calendar.SECOND, 59);
                        if(curCalendar.after(endCalendar)) {
                            mMsgBox.toast(R.string.survey_dead_line_should_after_today);
                        } else {
                            endCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                            moment.surveyDeadLine = endCalendar.getTimeInMillis() / 1000;
                            updateSurveyDeadLine();
                        }
                        rlSurveyVoteDeadLine.setEnabled(true);
                    }
                };

        Calendar calendar = Calendar.getInstance();
        if (moment.surveyDeadLine == Moment.SURVEY_DEADLINE_NO_LIMIT_VALUE) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.setTimeInMillis(moment.surveyDeadLine * 1000);
        }
        DatePickerDialog dialog = new DatePickerDialog(this,
                dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateGotVoice() {
        isCapturingVoice=false;

        //restore capture layout,as when voice file deleted, status correct
        ivCaptureInnerInd.setImageResource(R.drawable.timeline_record);
        tvCaptureInnerInd.setText(R.string.capture_voice_click_record);
        findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.bkg_e6e6e6);

        //switch to recored state
        findViewById(R.id.capture_voice_layout).setVisibility(View.GONE);
        findViewById(R.id.ready_captured_voice_layout).setVisibility(View.VISIBLE);

        addVoice2moment();

        setVoiceDuration();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.survey_vote_dead_line_layout:
            	rlSurveyVoteDeadLine.setEnabled(false);
                showPickTimeLayout(); //在方法执行后  rlSurveyVoteDeadLine.setEnabled(true);                                                            
                break;
            case R.id.btn_create_option:
//                showAddSurveyOptionPopup();
                addASurveyOption();
                break;
            case R.id.survey_can_multi_select:
                moment.isSurveyAllowMultiSelect = !moment.isSurveyAllowMultiSelect;
                updateSurveyMultiSelectState();
                break;
            case R.id.pick_location_delete:
                moment.place = null;
                moment.latitude = moment.longitude = 0;

                findViewById(R.id.pick_location_delete).setVisibility(View.GONE);
                tvPickLocationTxtInd.setText(R.string.place);
                findViewById(R.id.pick_location_layout).setBackgroundResource(R.drawable.bkg_e6e6e6);
                ivPickLocationImgInd.setImageResource(R.drawable.timeline_location);
                break;
            case R.id.pick_location_layout:
                if(!isGettingLocation && TextUtils.isEmpty(moment.place)) {
                	tvPickLocationTxtInd.setText(R.string.getting_location_info);
                    isGettingLocation=true;
                    locationHelper.setOnLocationGotListener(mOnLocationGotListener);
                    locationHelper.getLocationWithAMap(true);
                }else
                {
                	if(TextUtils.isEmpty(moment.place))
                		tvPickLocationTxtInd.setText(R.string.place);
                	isGettingLocation = false;
                	locationHelper.setOnLocationGotListener(null);
                	locationHelper.stop();
                }
                break;
            case R.id.ready_captured_voice_delete:
                stopPlayingVoice();

                findViewById(R.id.capture_voice_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.ready_captured_voice_layout).setVisibility(View.GONE);

                removeVoiceFromMoment();
                break;
            case R.id.ready_captured_voice_inner_left_layout:
                tryPlayOrStopVoice();
                break;
            case R.id.capture_voice_layout:
            	ImageButton imageButton = (ImageButton) findViewById(R.id.title_moment_send);
                if(!isCapturingVoice) {
                    if(startRecording()) {
                        isCapturingVoice=true;

                        ivCaptureInnerInd.setImageResource(R.drawable.timeline_record_a);
                        findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.text_field);
                        //正在录音，不能发布
                        imageButton.setImageResource(R.drawable.nav_confirm_p);
                        imageButton.setEnabled(false);
                        
                    }
                } else {
                    stopRecording();
                    updateGotVoice();
                    //结束了录音，可以发布
                    imageButton.setImageResource(R.drawable.nav_confirm);
                    imageButton.setEnabled(true);
//                    isCapturingVoice=false;
//
//                    //restore capture layout,as when voice file deleted, status correct
//                    ivCaptureInnerInd.setImageResource(R.drawable.timeline_record);
//                    tvCaptureInnerInd.setText(R.string.capture_voice_click_record);
//                    findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.bkg_e6e6e6);
//                    stopRecording();

                    //switch to recored state
//                    findViewById(R.id.capture_voice_layout).setVisibility(View.GONE);
//                    findViewById(R.id.ready_captured_voice_layout).setVisibility(View.VISIBLE);
//
//                    addVoice2moment();
//
//                    setVoiceDuration();
                }
                break;
            case R.id.title_back:
            	//发布动态退出时的提示
            	Builder builderBack = new AlertDialog.Builder(CreateNormalMomentWithTagActivity.this);
            	builderBack.setTitle("提示");
            	builderBack.setMessage("未完成发布，是否退出");
            	builderBack.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						releaseMediaFiles();
		                finish();
						
					}
				});
            	builderBack.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
    			
            	builderBack.create().show();
    			
//                releaseMediaFiles();
//                finish();
                break;
                
            case R.id.title_moment_send:
//            	Builder builder = new AlertDialog.Builder(CreateNormalMomentWithTagActivity.this);
            	if (!isContentValid()) {
            		Toast.makeText(CreateNormalMomentWithTagActivity.this, "你还没有填写任何信息", Toast.LENGTH_LONG).show();
            	} else {
      	
//            		if (TextUtils.isEmpty(etMomentMsgContent.getText().toString())) {
//            			Toast.makeText(CreateNormalMomentWithTagActivity.this, "内容不能为空", Toast.LENGTH_LONG).show();
//            		}
////            		else if (listPhotoOrVideo == null || listPhotoOrVideo.isEmpty()) {
////            			Toast.makeText(CreateNormalMomentWithTagActivity.this, "请添加图片", Toast.LENGTH_LONG).show();
////            			
////            		} else if (mLastVoiceFile == null || !mLastVoiceFile.exists()) {
////            			Toast.makeText(CreateNormalMomentWithTagActivity.this, "请录音", Toast.LENGTH_LONG).show();
////            		} 
//            		else {
//            			
//            			if (listPhotoOrVideo == null || listPhotoOrVideo.isEmpty()) {
//                			builder.setTitle("你还没有添加图片");
//                			
//                		} else if (mLastVoiceFile == null || !mLastVoiceFile.exists()) {
//                			builder.setTitle("你还没有录音");
//                		} else {
//                			builder.setTitle("信息填写完整了吗？");
//                		}
//            			
////            			builder.setTitle("提交前请确认信息是否完整");
//            			builder.setMessage("你确定要提交吗？");
//            			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//							
//							@Override
//							public void onClick(DialogInterface arg0, int arg1) {
//								createMoment();
//								
//							}
//						});
//            			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//							
//							@Override
//							public void onClick(DialogInterface arg0, int arg1) {
//							}
//						});
//            			builder.create().show();
//            			
//            		}
            		
            		if (tagType == 6) {//如果选择发布视频，那么就必须要添加视频才能发布
            			if (listPhotoOrVideo == null || listPhotoOrVideo.isEmpty()) {
            				Toast.makeText(CreateNormalMomentWithTagActivity.this, "请添加视频", Toast.LENGTH_LONG).show();
            			} else {
            				createMoment();
            			}
            		} else {
            			//只要填写了任何信息都可以发布
            			createMoment();
            		}
            		
            		//只要填写了任何信息都可以发布
            	//	createMoment();
            		
            	}     	
//                createMoment();
                break;
            case R.id.share_range_layout:
//                showShareRangeSelector();
                Intent intent = new Intent(this,ShareRangeSelectActivity.class);
                intent.putStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS,moment.limitedDepartmentList);
                startActivityForResult(intent, ACTIVITY_REQ_ID_SHARE_RANGE_SELECT);
                break;
            case R.id.trigger_add_img_layout:
            	if(tagType == TimelineActivity.TAG_VIDEO_IDX){
                    mediaHelper.inputVideo(this, ACTIVITY_REQ_ID_INPUT_VIDEO);
            	}else{
            		showPickImgSelector();
            	}
                break;
            default:
                break;
        }
    }

    private void stopPlayingVoice() {
        mediaPlayerWraper.stop();

//        if(null != playingVoiceRunnable) {
//            playingVoiceRunnable.stop();
//        }

//        if(null != mPlayer) {
//            tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper.makeMyTimeDisplayFromMS(mPlayer.getDuration()));
//        } else {
//            tvReadyCaptureVoiceTimeLength.setText("00:00");
//        }

//        if (null != mPlayer) {
//            mPlayer.release();
//            mPlayer=null;
//        }

//        ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_player_play);
    }

    private int MOMENT_VOICE_MIN_LEN_IN_MS=1000;
    private void setVoiceDuration() {
        if(voiceDuration > MOMENT_VOICE_MIN_LEN_IN_MS) {
            tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper.makeMyTimeDisplayFromMS(voiceDuration));
        } else {
            mMsgBox.toast(R.string.msg_voice_too_short);
            findViewById(R.id.ready_captured_voice_delete).performClick();
        }
//        final MediaPlayer parparedPlayer = new MediaPlayer();
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Boolean>() {
//            @Override
//            protected Boolean doInBackground(Void... params) {
//                boolean success=false;
//                try {
//                    parparedPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
//                    parparedPlayer.prepare();
//
//                    success=true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//
//                    parparedPlayer.release();
//                }
//                return success;
//            }
//
//            @Override
//            protected void onPostExecute(Boolean errno) {
//                if(errno) {
//                    voiceDuration=parparedPlayer.getDuration();
//
//                    tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper.makeMyTimeDisplayFromMS(voiceDuration));
//                    parparedPlayer.release();
//                } else {
//                    tvReadyCaptureVoiceTimeLength.setText("00:00");
//                }
//            }
//        });
    }

    private void tryPlayOrStopVoice() {
        if(!mLastVoiceFile.getAbsolutePath().equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(tvReadyCaptureVoiceTimeLength,false);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_play);
                }

                @Override
                public void onPlayBegin(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_play);
                }
            });
            mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(),voiceDuration);
        } else {
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(mLastVoiceFile.getAbsolutePath(),voiceDuration);
        }

//        if(null != mPlayer) {
//            if(mPlayer.isPlaying()) {
//                stopPlayingVoice();
//            }
//            return;
//        }
//        mPlayer = new MediaPlayer();
//
//        ivReadyCaptureVoicePlay.setImageResource(R.drawable.timeline_player_stop);
//
//        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Boolean>() {
//            @Override
//            protected Boolean doInBackground(Void... params) {
//                boolean success=false;
//                try {
//                    mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
//                    mPlayer.prepare();
//                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                        @Override
//                        public void onCompletion(MediaPlayer mediaPlayer) {
//                            stopPlayingVoice();
//                        }
//                    });
//                    success=true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                return success;
//            }
//
//            @Override
//            protected void onPostExecute(Boolean errno) {
//                if(errno) {
//                    mPlayer.start();
//
//                    playingVoiceRunnable=new TimeElapseReportRunnable();
//                    playingVoiceRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
//                        @Override
//                        public void reportElapse(final long elapsed) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if(null != mPlayer && mPlayer.isPlaying()) {
//                                        tvReadyCaptureVoiceTimeLength.setText(MediaPlayerWraper.makeMyTimeDisplayFromMS(mPlayer.getCurrentPosition()));
//                                    }
//                                }
//                            });
//                        }
//                    });
//                    new Thread(playingVoiceRunnable).start();
//                } else {
//                    mMsgBox.toast(R.string.operation_failed);
//                }
//            }
//        });
    }

    private void stopRecording() {
        if(null != timeElapseReportForCaptureVoiceRunnable) {
            timeElapseReportForCaptureVoiceRunnable.stop();
        }
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder = null;
        }
    }

    private boolean startRecording() {
        boolean ret=false;

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        } else {
            mMediaRecorder.reset();
        }

        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioSamplingRate(16000);
            mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(
                    MediaInputHelper.MEDIA_TYPE_VOICE, "." + ChatMessage.SEND_AUDIO_EXT);
            mMediaRecorder.setOutputFile(mLastVoiceFile.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();

            timeElapseReportForCaptureVoiceRunnable=new TimeElapseReportRunnable();
            timeElapseReportForCaptureVoiceRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
                @Override
                public void reportElapse(final long elapsed) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isCapturingVoice) {
                                String myFormatTime=MediaPlayerWraper.makeMyTimeDisplayFromMS(elapsed);
                                tvCaptureInnerInd.setText(String.format(getString(R.string.capture_voice_click_stop),myFormatTime));
                                //如果录音的时间超过120秒//将停止录音
                                if (elapsed >= 120000 ) {
                                	stopRecording();
                                    updateGotVoice();
                                    Toast.makeText(CreateNormalMomentWithTagActivity.this, "录音时间已经超过了120秒", Toast.LENGTH_SHORT).show();
                                } 
                            }
                        }
                    });
                }
            });
            new Thread(timeElapseReportForCaptureVoiceRunnable).start();
            ret=true;
        } catch (Exception e) {
            e.printStackTrace();
            stopRecording();

            if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
                mLastVoiceFile.delete();
            }
            mLastVoiceFile = null;
            mMsgBox.toast(R.string.media_record_not_avaliable);
        }

        return ret;
    }

    private void releaseMediaFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(null != listPhotoOrVideo) {
                    for(CreateMomentActivity.WMediaFile aWPhoto : listPhotoOrVideo) {
                        deleteWPhotoFile(aWPhoto);
                    }
                }

                if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
                    Database.deleteAFile(mLastVoiceFile.getAbsolutePath());
                }
            }
        }).start();
    }

    private void hideIME() {
        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etMomentMsgContent.getWindowToken(), 0);
    }

    private void showPickImgSelector() {
        hideIME();

        final BottomButtonBoard bottomBoard=new BottomButtonBoard(this, getWindow().getDecorView());
        bottomBoard.add(getString(R.string.image_take_photo), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (listPhotoOrVideo.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
                            mMsgBox.toast(String.format(CreateNormalMomentWithTagActivity.this.getString(R.string.settings_account_moment_take_photos_oom), CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
                            return;
                        }
                        mediaHelper.takePhoto(CreateNormalMomentWithTagActivity.this, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA);
                    }
                });
        bottomBoard.add(getString(R.string.image_pick_from_local), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (listPhotoOrVideo.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
                            mMsgBox.toast(String.format(CreateNormalMomentWithTagActivity.this.getString(R.string.settings_account_moment_take_photos_oom), CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
                            return;
                        }
                        int i = 0;
                        for (CreateMomentActivity.WMediaFile photo : listPhotoOrVideo) {
                            if (!photo.isFromGallery) {
                                i++;
                            }
                        }
                        Intent intent = new Intent(CreateNormalMomentWithTagActivity.this, SelectPhotoActivity.class);
                        intent.putExtra("num", CreateMomentActivity.TOTAL_PHOTO_ALLOWED - i);
                        ThemeHelper.putExtraCurrThemeResId(intent, CreateNormalMomentWithTagActivity.this);
                        ArrayList<String> listPath = new ArrayList<String>();
                        for (CreateMomentActivity.WMediaFile photo : listPhotoOrVideo) {
                            if (photo.isFromGallery) {
                                listPath.add(photo.galleryPath);
                            }
                        }
                        intent.putStringArrayListExtra("list", listPath);
                        startActivityForResult(intent, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY);
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();
    }

    private static long MOMENT_ALIAS_ID_INC=0;
    private void createMoment() {
        String content = etMomentMsgContent.getText().toString();
        if (content.length() > MOMENTS_WORDS_OVER) {
            mMsgBox.show(null, getString(R.string.moments_words_over_failed));
            return;
        }
        stopPlayingVoice();
        moment.text = content;
        if (!isContentValid()) {
            mMsgBox.show(null, getString(
                    R.string.settings_account_moment_text_cannot_be_empty));
        } else {
            //alias id and timestamp,timestamp should be the largest
            //will be updated when returned by server
            moment.id = Moment.ID_PLACEHOLDER_PREFIX + System.currentTimeMillis()+(++MOMENT_ALIAS_ID_INC);
            moment.timestamp = getIntent().getLongExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP,0)+1;
            Log.w("local moment timestamp set to "+moment.timestamp);
            if(TimelineActivity.TAG_SURVEY_IDX==tagType) {
                if(surveyOptions.size() < MIN_OPTIONS_NUM_FOR_SURVEY) {
//                    mMsgBox.toast(String.format(getString(R.string.inputsimpletext_empty),getString(R.string.survey_option)));
                    mMsgBox.toast(String.format(getString(R.string.survey_option_at_least_2),MIN_OPTIONS_NUM_FOR_SURVEY));
                    return;
                } else {
                    // 判断投票内容是否为空
                    // 判断投票的选项是否有一样的
                    Set<String> tempOptions = new HashSet<String>();
                    for (String surveyOption : surveyOptions) {
                        if (TextUtils.isEmpty(surveyOption.trim())) {
                            mMsgBox.toast(R.string.survey_option_content_empty);
                            return;
                        }
                        tempOptions.add(surveyOption.trim());
                    }
                    if (tempOptions.size() != surveyOptions.size()) {
                        mMsgBox.toast(R.string.survey_options_are_same);
                        return;
                    }
                    moment.surveyOptions.clear();
                    for(int i=0; i<surveyOptions.size(); ++i) {
                        Moment.SurveyOption aOption=new Moment.SurveyOption();
                        aOption.momentId=moment.id;
                        aOption.optionId=""+i;//this is alias
                        aOption.optionDesc=surveyOptions.get(i);
                        aOption.votedNum=0;
                        aOption.isVoted=false;
                        moment.surveyOptions.add(aOption);
                    }
                }
            }

            moment.tag=TimelineActivity.getSelectedTagServerDesc(
                    CreateNormalMomentWithTagActivity.this, tagType, moment.isSurveyAllowMultiSelect);

            //store local moment
            mMsgBox.showWait();
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    String uid = PrefUtil.getInstance(CreateNormalMomentWithTagActivity.this).getUid();

                    if (null == moment.owner) {
                        moment.owner = new Buddy();
                        moment.owner.nickName = PrefUtil.getInstance(
                                CreateNormalMomentWithTagActivity.this).getMyNickName();
                    }
                    moment.owner.userID = uid;
                    moment.likedByMe = false;
                    mDb.storeMoment(moment, null);
                    Log.e("moment media count " + moment.multimedias.size());
                    for (WFile f : moment.multimedias) {
                        mDb.storeMultimedia(moment, f);
                    }

                    // 把缩略图从临时路径复制到标准路径，以便展示
                    for (WFile file : moment.multimedias) {
                        if (!TextUtils.isEmpty(file.localThumbnailPath) &&
                                new File(file.localThumbnailPath).exists()) {
                            String dst = PhotoDisplayHelper.makeLocalFilePath(file.thumb_fileid, file.getExt());
                            if (!TextUtils.equals(file.localThumbnailPath, dst)) {
                                FileUtils.copyFile(file.localThumbnailPath, dst);
                            }
                        }
                    }

                    Intent data = new Intent();
                    data.putExtra(EXTRA_MOMENT, moment);
                    setResult(RESULT_OK, data);

                    //upload to server
                    Intent intent = new Intent(CreateNormalMomentWithTagActivity.this, PublishMomentService.class);
                    intent.putExtra(PublishMomentService.EXTRA_MOMENT, moment);
                    intent.putExtra(PublishMomentService.EXTRA_IS_SURVEY, TimelineActivity.TAG_SURVEY_IDX == tagType);
                    startService(intent);
                    return 0;
                }

                @Override
                protected void onPostExecute(Integer errno) {
                    mMsgBox.dismissWait();
                    finish();
                }
            });
        }
    }

    private LinkedList<BitmapDrawable> bmpDrawableList=new LinkedList<BitmapDrawable>();
    private void recycleStoredBitmapDrawable() {
        for(BitmapDrawable aBmpDrawable : bmpDrawableList) {
            if(null != aBmpDrawable) {
                Bitmap bmp=aBmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);
            }
        }
        bmpDrawableList.clear();
    }

    private void copyFileForMomentMultimedia(WFile aFile, boolean isPhoto) {
        String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                aFile.fileid, aFile.getExt());
        FileUtils.copyFile(aFile.localPath, destFilePath);

        // make thumbnail
        if (null != aFile.thumb_fileid && aFile.localThumbnailPath == null) {
            boolean saved = false;
            Bitmap thumb = null;
            if (isPhoto) {
                thumb = BmpUtils.decodeFile(aFile.localPath, 200, 200, true);
            } else {
                thumb = BitmapFactory.decodeResource(getResources(), R.drawable.chat_icon_video);
            }
            if (thumb != null) {
                aFile.localThumbnailPath = PhotoDisplayHelper.makeLocalFilePath(
                        aFile.thumb_fileid, aFile.getExt());
                OutputStream os = null;
                try {
                    os = new FileOutputStream(aFile.localThumbnailPath);
                    saved = thumb.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX format should be same with main file?
                    os.close();

                    BmpUtils.recycleABitmap(thumb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!saved) {
                aFile.thumb_fileid = aFile.localThumbnailPath = null;
            }
        }
    }

    private void addMedia2moment(CreateMomentActivity.WMediaFile file) {
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(file.localPath)) {
                Log.w("duplicate photo add 2 momet, omit");
                return;
            }
        }
        if (TextUtils.isEmpty(file.getExt()))
            file.setExt(FileUtils.getExt(file.localPath));
        if (TextUtils.isEmpty(file.fileid))
            file.fileid = String.valueOf(Math.random());
        if (TextUtils.isEmpty(file.thumb_fileid))
            file.thumb_fileid = String.valueOf(Math.random());
        file.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
        moment.multimedias.add(file);

        copyFileForMomentMultimedia(file, file.isPhoto);
    }

    private void removePhotoFromMoment(CreateMomentActivity.WMediaFile aPhoto) {
        WFile file2remove=null;
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(aPhoto.localPath)) {
                file2remove=aFile;
                break;
            }
        }

        if(null != file2remove) {
            moment.multimedias.remove(file2remove);
        }
    }

    private int voiceDuration;
    private void addVoice2moment() {
        if (mLastVoiceFile != null && mLastVoiceFile.exists()) {
            String localFilename = mLastVoiceFile.getAbsolutePath();
            String ext = FileUtils.getExt(localFilename);
            String fakeFileId = String.valueOf(Math.random());
            MediaPlayer mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mLastVoiceFile.getAbsolutePath());
                mPlayer.prepare();
                voiceDuration = mPlayer.getDuration();
            } catch (IOException e) {
                voiceDuration = 0;
                e.printStackTrace();
            } finally {
                mPlayer.release();
                mPlayer = null;
            }
            WFile f = new WFile(ext, fakeFileId, voiceDuration/1000, localFilename);
            f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
            moment.multimedias.add(f);

            copyFileForMomentMultimedia(f, false);
        }
    }

    private void removeVoiceFromMoment() {
        WFile file2remove=null;
        String localFilename = mLastVoiceFile.getAbsolutePath();
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(localFilename)) {
                file2remove=aFile;
                break;
            }
        }

        if(null != file2remove) {
            moment.multimedias.remove(file2remove);
        }
        voiceDuration=0;

        mLastVoiceFile.delete();
        mLastVoiceFile=null;
    }

    private void recycleAView(View view) {
        ImageView aView=(ImageView) view.findViewById(R.id.img_photo);
        if(null != aView && null != aView.getDrawable()) {
            try {
                BitmapDrawable bmpDrawable=(BitmapDrawable)aView.getDrawable();
                Bitmap bmp=bmpDrawable.getBitmap();
                BmpUtils.recycleABitmap(bmp);

                deleteWPhotoFile((CreateMomentActivity.WMediaFile)view.getTag());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteWPhotoFile(CreateMomentActivity.WMediaFile aWPhoto) {
        if(null != aWPhoto) {
            Database.deleteAFile(aWPhoto.localPath);
        }
    }

    private void notifyFileChanged(boolean isAdded) {
        updateTriggerAddImgDescTxtStatus();

        if (isAdded) {
            Assert.assertTrue(!listPhotoOrVideo.isEmpty());
            addMedia2moment(listPhotoOrVideo.get(listPhotoOrVideo.size() - 1));

            final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
            final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
                    listPhotoOrVideo.get(listPhotoOrVideo.size() - 1).localThumbnailPath));
            bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

            listPhotoOrVideo.get(listPhotoOrVideo.size() - 1).relativeView=view;

            View imgDelete = view.findViewById(R.id.btn_delete);
            imgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAImage(view);
                }
            });
//            LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//            viewLayoutParams.setMargins(0,0,DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
            view.setTag(listPhotoOrVideo.get(listPhotoOrVideo.size() - 1));
            addedImgLayout.addView(view, 0);
//            ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//            params.width += imgPhoto.getLayoutParams().width;
//            addedImgLayout.setLayoutParams(params);
        } else {
            addedImgLayout.removeAllViews();
            recycleStoredBitmapDrawable();
            for(CreateMomentActivity.WMediaFile aPhoto : listPhotoOrVideo) {
                removePhotoFromMoment(aPhoto);
            }
//            moment.multimedias.clear();

            int fileNum = listPhotoOrVideo.size();
            for (int i = 0; i < fileNum; i++) {
                addMedia2moment(listPhotoOrVideo.get(i));

                final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
                final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
                imgPhoto.setImageDrawable(new BitmapDrawable(getResources(),
                        listPhotoOrVideo.get(i).localThumbnailPath));
                bmpDrawableList.add((BitmapDrawable) imgPhoto.getDrawable());

                listPhotoOrVideo.get(i).relativeView=view;

                View imgDelete = view.findViewById(R.id.btn_delete);
                imgDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAImage(view);
                    }
                });
//                LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//                viewLayoutParams.setMargins(0, 0, DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
                view.setTag(listPhotoOrVideo.get(i));
                addedImgLayout.addView(view,0);
//                ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//                params.width += imgPhoto.getLayoutParams().width;
//                addedImgLayout.setLayoutParams(params);
            }
        }
    }

    private void deleteAImage(View view) {
        CreateMomentActivity.WMediaFile path = (CreateMomentActivity.WMediaFile) view.getTag();
        listPhotoOrVideo.remove(path);

        removePhotoFromMoment(path);

        recycleAView(view);
        ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
        bmpDrawableList.remove(imgPhoto.getDrawable());

        recycleAView(view);
        notifyFileChanged(false);
    }

    private void updateTriggerAddImgDescTxtStatus() {
        TextView tvDesc=(TextView) findViewById(R.id.trigger_add_img_txt_desc);
        if(listPhotoOrVideo.size() > 0) {
            tvDesc.setVisibility(View.GONE);
            hsvImgList.setBackgroundResource(R.drawable.table_white);
        } else {
            tvDesc.setVisibility(View.VISIBLE);
            hsvImgList.setBackgroundResource(R.drawable.bkg_e6e6e6);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_REQ_ID_SHARE_RANGE_SELECT:
                if (resultCode == RESULT_OK) {
                    int shareRangeTag = data.getIntExtra(
                            ShareRangeSelectActivity.SHARE_RANGE_TAG,
                            Moment.VISIBILITY_ALL);
                    if(Moment.VISIBILITY_ALL == shareRangeTag) {
                        tvShareRange.setText(R.string.share_range_public);
                        moment.limitedDepartmentList.clear();
                    } else {
                        tvShareRange.setText(R.string.share_range_private);

                        ArrayList<String> deps = data.getStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS);
                        moment.limitedDepartmentList=deps;
                    }
                }
                break;
            case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> listPath = data.getStringArrayListExtra("list");
                    ArrayList<CreateMomentActivity.WMediaFile> photo2add = new ArrayList<CreateMomentActivity.WMediaFile>();
                    ArrayList<CreateMomentActivity.WMediaFile> photo2del = new ArrayList<CreateMomentActivity.WMediaFile>();
                    for (int i = 0; i < listPhotoOrVideo.size(); i++) {
                        boolean needAdd=false;
                        if (!listPhotoOrVideo.get(i).isFromGallery) {
                            needAdd=true;
                        } else {
                            if(listPath.contains(listPhotoOrVideo.get(i).galleryPath)) {
                                listPath.remove(listPhotoOrVideo.get(i).galleryPath);
                                needAdd=true;
                            } else {
                                //not contained,delete this
                                photo2del.add(listPhotoOrVideo.get(i));
                            }
                        }

                        if(needAdd) {
                            photo2add.add(listPhotoOrVideo.get(i));
                        }
                    }

                    for(CreateMomentActivity.WMediaFile aPhoto : photo2del) {
                        deleteAImage(aPhoto.relativeView);
                    }
                    listPhotoOrVideo = photo2add;

                    mMsgBox.showWait();
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<ArrayList<String>, Void, Void>() {
                        //                        boolean firstAdd=true;
                        @Override
                        protected Void doInBackground(ArrayList<String>... params) {
                            for (String path : params[0]) {
                                CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(true);
                                Bitmap bmp = BmpUtils.decodeFile(path, CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT);
                                File file = MediaInputHelper.makeOutputMediaFile(
                                        MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg");
                                try {
                                    OutputStream os = new FileOutputStream(file);
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
                                    os.close();

                                    BmpUtils.recycleABitmap(bmp);

                                    photo.localPath = file.getAbsolutePath();
                                    photo.galleryPath = path;
                                    photo.isFromGallery = true;
                                    listPhotoOrVideo.add(photo);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }

                                publishProgress((Void) null);
                            }
                            return null;
                        }

                        @Override
                        protected void onProgressUpdate(Void... errno) {
                            instance.notifyFileChanged(true);
                        }

                        @Override
                        protected void onPostExecute(Void errno) {
                            mMsgBox.dismissWait();
                            instance.notifyFileChanged(false);
                        }
                    }, listPath);
                }
                break;
            case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA:
                if (resultCode == RESULT_OK) {
                    AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Intent, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Intent... params) {
                            String[] path = new String[2];
                            boolean handleImageRet = mediaHelper.handleImageResult(CreateNormalMomentWithTagActivity.this, params[0],
                                    CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT,
                                    0, 0,
                                    path);
                            if (handleImageRet) {
                                Log.i("handle result ok,path[0]=" + path[0]);
                            } else {
                                Log.e("handle image error");
                            }
                            CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile(true);
                            photo.localPath = path[0];
                            photo.isFromGallery = false;
                            listPhotoOrVideo.add(photo);
                            return handleImageRet;
                        }

                        @Override
                        protected void onPostExecute(Boolean status) {
                            if (status) {
                                Assert.assertTrue(!listPhotoOrVideo.isEmpty());
                                instance.notifyFileChanged(true);
                            }
                        }
                    }, data);
                }
                break;
            case ACTIVITY_REQ_ID_INPUT_VIDEO:
                if (resultCode == RESULT_OK && null != mediaHelper) {
                    String[] videoPath = new String[2];
                    if(mediaHelper.handleVideoResult(
                            this,
                            data,
                            CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT,
                            PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                            videoPath)) {
                        CreateMomentActivity.WMediaFile videoFile = new CreateMomentActivity.WMediaFile(false);
                        videoFile.localPath = videoPath[0];
                        videoFile.localThumbnailPath = videoPath[1];
                        listPhotoOrVideo.add(videoFile);
                        notifyFileChanged(true);
                    }
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        moment.text = etMomentMsgContent.getText().toString();

        outState.putParcelable(EXTRA_MOMENT, moment);
        outState.putParcelable("media_helper", mediaHelper);
        outState.putParcelableArrayList("list_photo", listPhotoOrVideo);
        outState.putStringArrayList("survey_options", surveyOptions);

        if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
            outState.putString("last_voice_file",mLastVoiceFile.getAbsolutePath());
        }
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }
}
