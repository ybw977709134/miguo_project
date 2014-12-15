package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
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

    private int tagType;

    private TextView tvShareRange;

    private MediaInputHelper mediaHelper;
    private MessageBox mMsgBox;
    private Database mDb;

    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA=1;
    private final static int ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY=2;
    private final static int ACTIVITY_REQ_ID_SHARE_RANGE_SELECT=3;

    private ArrayList<CreateMomentActivity.WMediaFile> listPhoto;

    private static CreateNormalMomentWithTagActivity instance;

    private LinearLayout addedImgLayout;

    private Moment moment = new Moment();
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
    private boolean isSuveyMultiSelectable;
    private ImageButton ibCanSurveyMultiSelect;

    private RelativeLayout rlSurveyVoteDeadLine;
    private TextView tvSurveyVoteDeadLine;
//    private String surveyDeadLineTime;
    private Calendar deadLineCalendar;
    public final static String SURVEY_DEADLINE_NO_LIMIT_VALUE="-1";

    private MediaPlayerWraper mediaPlayerWraper;
    private TimeElapseReportRunnable rightBtnStatusRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_moment_with_tag_layout);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView(savedInstanceState);

        rightBtnStatusRunnable=new TimeElapseReportRunnable();
        rightBtnStatusRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
            @Override
            public void reportElapse(final long elapsed) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkRightBtnStatus();
                    }
                });
            }
        });
        new Thread(rightBtnStatusRunnable).start();
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

        if(null != rightBtnStatusRunnable) {
            rightBtnStatusRunnable.stop();
        }

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
                && (listPhoto == null || listPhoto.isEmpty())
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

        etMomentMsgContent=(EditText) findViewById(R.id.moment_content);
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


        mediaHelper = new MediaInputHelper(this);
        listPhoto = new ArrayList<CreateMomentActivity.WMediaFile>();

        surveyOptions=new ArrayList<String>();
        isSuveyMultiSelectable=false;

        if(null != savedInstanceState) {
            try {
                String savedMsg=savedInstanceState.getString("moment_msg_content");
                String savedRange=savedInstanceState.getString("share_range");
                if(!TextUtils.isEmpty(savedMsg)) {
                    etMomentMsgContent.setText(savedMsg);
                }
                if(!TextUtils.isEmpty(savedRange)) {
                    tvShareRange.setText(savedRange);
                }
                mediaHelper=savedInstanceState.getParcelable("media_helper");
                listPhoto=savedInstanceState.getParcelableArrayList("list_photo");
                surveyOptions=savedInstanceState.getStringArrayList("survey_options");
                isSuveyMultiSelectable=savedInstanceState.getBoolean("survey_multi_selectable");

                long surveyEndTimeInM=savedInstanceState.getLong("survey_end_time");
                if(0 != surveyEndTimeInM) {
                    deadLineCalendar=new GregorianCalendar();
                    deadLineCalendar.setTimeInMillis(surveyEndTimeInM);
                    updateSurveyDeadLine();
                }

                moment.limitedDepartmentList = savedInstanceState.getStringArrayList("share_range_limited_dep_list");
                if(null == moment.limitedDepartmentList) {
                    moment.limitedDepartmentList=new ArrayList<String>();
                }
                String lastVoiceFilePath=savedInstanceState.getString("last_voice_file");
                if(!TextUtils.isEmpty(lastVoiceFilePath)) {
                    File aFile=new File(lastVoiceFilePath);
                    if(null != aFile && aFile.exists()) {
                        mLastVoiceFile=aFile;
                        updateGotVoice();
                    }
                }

                moment.latitude = savedInstanceState.getDouble("location_latitude");
                moment.longitude = savedInstanceState.getDouble("location_longitude");

                String strAddress=savedInstanceState.getString("location_address");
                if(!TextUtils.isEmpty(strAddress)) {
                    moment.place = strAddress;
                    updateLocation(moment.latitude,moment.longitude,strAddress);
                }

                notifyFileChanged(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(TimelineActivity.TAG_NOTICE_IDX == tagType) {
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
        ibCanSurveyMultiSelect.setBackgroundResource(isSuveyMultiSelectable ? R.drawable.switch_on_little : R.drawable.switch_off_little);
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
        if(null != deadLineCalendar) {
            Date date = deadLineCalendar.getTime();

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
//                            surveyDeadLineTime=Database.chatMessage_dateToUTCString(endCalendar.getTime());
                            deadLineCalendar=endCalendar;
                            updateSurveyDeadLine();
                        }
                    }
                };

        Calendar calendar=Calendar.getInstance();
        if (null == deadLineCalendar) {
            calendar.add(Calendar.DAY_OF_MONTH,1);
        } else {
            calendar.setTime(deadLineCalendar.getTime());
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
                showPickTimeLayout();
                break;
            case R.id.btn_create_option:
//                showAddSurveyOptionPopup();
                addASurveyOption();
                break;
            case R.id.survey_can_multi_select:
                isSuveyMultiSelectable=!isSuveyMultiSelectable;
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
                if(!isCapturingVoice) {
                    if(startRecording()) {
                        isCapturingVoice=true;

                        ivCaptureInnerInd.setImageResource(R.drawable.timeline_record_a);
                        findViewById(R.id.capture_voice_layout).setBackgroundResource(R.drawable.text_field);
                    }
                } else {
                    stopRecording();
                    updateGotVoice();
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
                releaseMediaFiles();
                finish();
                break;
            case R.id.title_moment_send:
                createMoment();
                break;
            case R.id.share_range_layout:
//                showShareRangeSelector();
                Intent intent = new Intent(this,ShareRangeSelectActivity.class);
                intent.putStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS,moment.limitedDepartmentList);
                startActivityForResult(intent, ACTIVITY_REQ_ID_SHARE_RANGE_SELECT);
                break;
            case R.id.trigger_add_img_layout:
                showPickImgSelector();
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
//        new AsyncTask<Void, Void, Boolean>() {
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
//        }.execute((Void) null);
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
//        new AsyncTask<Void, Void, Boolean>() {
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
//        }.execute((Void) null);
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
            mLastVoiceFile = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_VOICE, ".m4a");
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
                if(null != listPhoto) {
                    for(CreateMomentActivity.WMediaFile aWPhoto : listPhoto) {
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
                        if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
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
                        if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
                            mMsgBox.toast(String.format(CreateNormalMomentWithTagActivity.this.getString(R.string.settings_account_moment_take_photos_oom), CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
                            return;
                        }
                        int i = 0;
                        for (CreateMomentActivity.WMediaFile photo : listPhoto) {
                            if (!photo.isFromGallery) {
                                i++;
                            }
                        }
                        Intent intent = new Intent(CreateNormalMomentWithTagActivity.this, SelectPhotoActivity.class);
                        intent.putExtra("num", CreateMomentActivity.TOTAL_PHOTO_ALLOWED - i);
                        ThemeHelper.putExtraCurrThemeResId(intent, CreateNormalMomentWithTagActivity.this);
                        ArrayList<String> listPath = new ArrayList<String>();
                        for (CreateMomentActivity.WMediaFile photo : listPhoto) {
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

//        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View contentView= lf.inflate(R.layout.pick_img_selector_layout, null);
//
//        final PopupWindow pickImgOptionPopWindow = Utils.getFixedPopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//
//        View pickFromGalleryView=contentView.findViewById(R.id.pick_img_from_gallery);
//        View pickFromCameraView=contentView.findViewById(R.id.pick_img_from_camera);
//
//        contentView.findViewById(R.id.root_layout).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                pickImgOptionPopWindow.dismiss();
//            }
//        });
//
//        pickFromGalleryView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                pickImgOptionPopWindow.dismiss();
//
//                if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
//                    mMsgBox.toast(String.format(CreateNormalMomentWithTagActivity.this.getString(R.string.settings_account_moment_take_photos_oom), CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
//                    return;
//                }
//                int i = 0;
//                for (CreateMomentActivity.WPhoto photo : listPhoto) {
//                    if (!photo.isFromGallery) {
//                        i++;
//                    }
//                }
//                Intent intent = new Intent(CreateNormalMomentWithTagActivity.this, SelectPhotoActivity.class);
//                intent.putExtra("num", CreateMomentActivity.TOTAL_PHOTO_ALLOWED - i);
//                ArrayList<String> listPath = new ArrayList<String>();
//                for (CreateMomentActivity.WPhoto photo : listPhoto) {
//                    if (photo.isFromGallery) {
//                        listPath.add(photo.galleryPath);
//                    }
//                }
//                intent.putStringArrayListExtra("list", listPath);
//                startActivityForResult(intent, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_GALLERY);
//            }
//        });
//
//        pickFromCameraView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                pickImgOptionPopWindow.dismiss();
//
//                if (listPhoto.size() >= CreateMomentActivity.TOTAL_PHOTO_ALLOWED) {
//                    mMsgBox.toast(String.format(CreateNormalMomentWithTagActivity.this.getString(R.string.settings_account_moment_take_photos_oom), CreateMomentActivity.TOTAL_PHOTO_ALLOWED));
//                    return;
//                }
//                mediaHelper.takePhoto(CreateNormalMomentWithTagActivity.this, ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA);
//            }
//        });
//
//        pickImgOptionPopWindow.setFocusable(true);
//        pickImgOptionPopWindow.setTouchable(true);
//        pickImgOptionPopWindow.setOutsideTouchable(true);
//        pickImgOptionPopWindow.setBackgroundDrawable(new BitmapDrawable());
//
////        pickImgOptionPopWindow.setWidth((int)getResources().getDimension(R.dimen.create_moment_option_tags_width));
//        pickImgOptionPopWindow.showAtLocation(findViewById(R.id.share_range_layout), Gravity.CENTER, 0, 0);
//        pickImgOptionPopWindow.update();
    }

//    private void showShareRangeSelector() {
//        LayoutInflater lf = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View contentView= lf.inflate(R.layout.share_range_selector_layout, null);
//
//        final PopupWindow shareOptionPopWindow = Utils.getFixedPopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//
//        View selectPublicView=contentView.findViewById(R.id.share_range_selector_public);
//        View selectPrivateView=contentView.findViewById(R.id.share_range_selector_private);
//
//        contentView.findViewById(R.id.root_layout).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                shareOptionPopWindow.dismiss();
//            }
//        });
//
//        selectPublicView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                shareOptionPopWindow.dismiss();
//                tvShareRange.setText(R.string.share_range_public);
//            }
//        });
//
//        selectPrivateView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                shareOptionPopWindow.dismiss();
//                tvShareRange.setText(R.string.share_range_private);
//            }
//        });
//
//        shareOptionPopWindow.setFocusable(true);
//        shareOptionPopWindow.setTouchable(true);
//        shareOptionPopWindow.setOutsideTouchable(true);
//        shareOptionPopWindow.setBackgroundDrawable(new BitmapDrawable());
//
////        shareOptionPopWindow.setWidth((int)getResources().getDimension(R.dimen.create_moment_option_tags_width));
//        shareOptionPopWindow.showAtLocation(findViewById(R.id.share_range_layout), Gravity.CENTER, 0, 0);
//        shareOptionPopWindow.update();
//    }

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
            moment.id = CreateMomentActivity.ALIAS_ID_PREFIX+System.currentTimeMillis()+(++MOMENT_ALIAS_ID_INC);
            moment.timestamp = getIntent().getLongExtra(CreateMomentActivity.EXTRA_KEY_MOMENT_MAX_TIMESTAMP,0)+1;
            Log.w("local moment timestamp set to "+moment.timestamp);
            if(TimelineActivity.TAG_SURVEY_IDX==tagType) {
                if(surveyOptions.size() < MIN_OPTIONS_NUM_FOR_SURVEY) {
//                    mMsgBox.toast(String.format(getString(R.string.inputsimpletext_empty),getString(R.string.survey_option)));
                    mMsgBox.toast(String.format(getString(R.string.survey_option_at_least_2),MIN_OPTIONS_NUM_FOR_SURVEY));
                    return;
                } else {
                    moment.isSurveyAllowMultiSelect=isSuveyMultiSelectable;
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
                    if(null == deadLineCalendar) {
                        moment.surveyDeadLine=SURVEY_DEADLINE_NO_LIMIT_VALUE;
                    } else {
                        deadLineCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                        moment.surveyDeadLine=Long.toString(deadLineCalendar.getTimeInMillis()/1000);
                    }
                }
            }

            moment.tag=TimelineActivity.getSelectedTagServerDesc(CreateNormalMomentWithTagActivity.this,tagType,isSuveyMultiSelectable);

            //share range is used local now
            if(getString(R.string.share_range_public).equals(tvShareRange.getText())) {
                moment.shareRange=Moment.SERVER_SHARE_RANGE_PUBLIC;
            } else {
                moment.shareRange=Moment.SERVER_SHARE_RANGE_LIMITED;
            }

            //store local moment
            mMsgBox.showWait();
            new AsyncTask<Void, Integer, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    String uid = PrefUtil.getInstance(CreateNormalMomentWithTagActivity.this).getUid();

                    if (null == moment.owner)
                        moment.owner = new Buddy();
                    moment.owner.userID = uid;
                    moment.likedByMe = false;
                    mDb.storeMoment(moment,null);
                    Log.e("moment media count "+moment.multimedias.size());
                    for (WFile f : moment.multimedias) {
                        mDb.storeMultimedia(moment, f);
                    }

                    Intent data = new Intent();
                    setResult(RESULT_OK, data);

                    //upload to server
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int errno=ErrorCode.UNKNOWN;
                            if(TimelineActivity.TAG_SURVEY_IDX == tagType) {
                                errno = WowMomentWebServerIF.getInstance(CreateNormalMomentWithTagActivity.this).fAddMomentForSurvey(moment);
                            } else if (TimelineActivity.TAG_VIDEO_IDX == tagType) {
                                //not implemented now
                                throw new IllegalArgumentException();
                            } else {
                            	//android.util.Log.i("-->>>", moment.place);
                                errno = WowMomentWebServerIF.getInstance(CreateNormalMomentWithTagActivity.this).fAddMoment(moment);
                            }
                            if (errno == ErrorCode.OK) {
                                Intent intent = new Intent(CreateNormalMomentWithTagActivity.this, DownloadingAndUploadingService.class);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_ACTION,
                                        DownloadingAndUploadingService.ACTION_UPLOAD_MOMENT_FILE);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_MOMENT_ID, moment.id);
                                intent.putExtra(DownloadingAndUploadingService.EXTRA_WFILES, moment.multimedias);
                                startService(intent);
                            }
                        }
                    }).start();
                    return 0;
                }

                @Override
                protected void onPostExecute(Integer errno) {
                    mMsgBox.dismissWait();
                    finish();
                   
                    
                }
            }.execute((Void)null);
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

    private void copyFileForMomentMultimedia(WFile aFile) {
        String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                aFile.fileid, aFile.getExt());
        FileUtils.copyFile(aFile.localPath, destFilePath);

        // make thumbnail
        if (null != aFile.thumb_fileid) {
            Bitmap thumb = BmpUtils.decodeFile(aFile.localPath, 200, 200, true);
            aFile.localThumbnailPath = PhotoDisplayHelper.makeLocalFilePath(
                    aFile.thumb_fileid, aFile.getExt());
            boolean saved = false;
            OutputStream os = null;
            try {
                os = new FileOutputStream(aFile.localThumbnailPath);
                saved = thumb.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX format should be same with main file?
                os.close();

                BmpUtils.recycleABitmap(thumb);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!saved) {
                aFile.thumb_fileid = aFile.localThumbnailPath = null;
            }
        }
    }

    private void addPhoto2moment(CreateMomentActivity.WMediaFile aPhoto) {
        for(WFile aFile : moment.multimedias) {
            if(aFile.localPath.equals(aPhoto.localPath)) {
                Log.w("duplicate photo add 2 momet, omit");
                return;
            }
        }
        String ext = FileUtils.getExt(aPhoto.localPath);
        String fakeFileId = String.valueOf(Math.random());
        WFile f = new WFile(ext, fakeFileId, null, aPhoto.localPath);
        f.thumb_fileid = String.valueOf(Math.random());
        f.remoteDir = GlobalSetting.S3_MOMENT_FILE_DIR;
        moment.multimedias.add(f);

        copyFileForMomentMultimedia(f);
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

            copyFileForMomentMultimedia(f);
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
            final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
            final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
            imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(listPhoto.size() - 1).localPath));
            bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

            addPhoto2moment(listPhoto.get(listPhoto.size() - 1));
            listPhoto.get(listPhoto.size() - 1).relativeView=view;

            ImageButton imgDelete = (ImageButton) view.findViewById(R.id.btn_delete);
            imgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAImage(view);
                }
            });
//            LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//            viewLayoutParams.setMargins(0,0,DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
            view.setTag(listPhoto.get(listPhoto.size() - 1));
            addedImgLayout.addView(view, 0);
//            ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//            params.width += imgPhoto.getLayoutParams().width;
//            addedImgLayout.setLayoutParams(params);
        } else {
            addedImgLayout.removeAllViews();
            recycleStoredBitmapDrawable();
            for(CreateMomentActivity.WMediaFile aPhoto : listPhoto) {
                removePhotoFromMoment(aPhoto);
            }
//            moment.multimedias.clear();

            int fileNum = listPhoto.size();
            for (int i = 0; i < fileNum; i++) {
                final View view = LayoutInflater.from(this).inflate(R.layout.listitem_moment_image, addedImgLayout, false);
                final ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
                imgPhoto.setImageDrawable(new BitmapDrawable(getResources(), listPhoto.get(i).localPath));
                bmpDrawableList.add((BitmapDrawable)imgPhoto.getDrawable());

                addPhoto2moment(listPhoto.get(i));
                listPhoto.get(i).relativeView=view;

                ImageButton imgDelete = (ImageButton) view.findViewById(R.id.btn_delete);
                imgDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAImage(view);
                    }
                });
//                LinearLayout.LayoutParams viewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
//                viewLayoutParams.setMargins(0, 0, DensityUtil.dip2px(CreateNormalMomentWithTagActivity.this, 10), 0);
                view.setTag(listPhoto.get(i));
                addedImgLayout.addView(view,0);
//                ViewGroup.LayoutParams params = addedImgLayout.getLayoutParams();
//                params.width += imgPhoto.getLayoutParams().width;
//                addedImgLayout.setLayoutParams(params);
            }
        }
    }

    private void deleteAImage(View view) {
        CreateMomentActivity.WMediaFile path = (CreateMomentActivity.WMediaFile) view.getTag();
        listPhoto.remove(path);

        removePhotoFromMoment(path);

        recycleAView(view);
        ImageView imgPhoto = (ImageView) view.findViewById(R.id.img_photo);
        bmpDrawableList.remove(imgPhoto.getDrawable());

        recycleAView(view);
        notifyFileChanged(false);
    }

    private void updateTriggerAddImgDescTxtStatus() {
        TextView tvDesc=(TextView) findViewById(R.id.trigger_add_img_txt_desc);
        if(listPhoto.size() > 0) {
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
                    String shareRangeTag=data.getStringExtra(ShareRangeSelectActivity.SHARE_RANGE_TAG);
                    if(Moment.SERVER_SHARE_RANGE_PUBLIC.equals(shareRangeTag)) {
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
                    for (int i = 0; i < listPhoto.size(); i++) {
                        boolean needAdd=false;
                        if (!listPhoto.get(i).isFromGallery) {
                            needAdd=true;
                        } else {
                            if(listPath.contains(listPhoto.get(i).galleryPath)) {
                                listPath.remove(listPhoto.get(i).galleryPath);
                                needAdd=true;
                            } else {
                                //not contained,delete this
                                photo2del.add(listPhoto.get(i));
                            }
                        }

                        if(needAdd) {
                            photo2add.add(listPhoto.get(i));
                        }
                    }

                    for(CreateMomentActivity.WMediaFile aPhoto : photo2del) {
                        deleteAImage(aPhoto.relativeView);
                    }
                    listPhoto = photo2add;

                    mMsgBox.showWait();
                    new AsyncTask<ArrayList<String>, Void, Void>() {
//                        boolean firstAdd=true;
                        @Override
                        protected Void doInBackground(ArrayList<String>... params) {
                            for (String path : params[0]) {
                                CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile();
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
                                    listPhoto.add(photo);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                publishProgress((Void)null);
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
                    }.execute(listPath);
                }
                break;
            case ACTIVITY_REQ_ID_PICK_PHOTO_FROM_CAMERA:
                if (resultCode == RESULT_OK) {
                    new AsyncTask<Intent, Void, Void>() {
                        @Override
                        protected Void doInBackground(Intent... params) {
                            String[] path = new String[2];
                            boolean handleImageRet=mediaHelper.handleImageResult(CreateNormalMomentWithTagActivity.this, params[0],
                                    CreateMomentActivity.PHOTO_SEND_WIDTH, CreateMomentActivity.PHOTO_SEND_HEIGHT,
                                    0, 0,
                                    path);
                            if(handleImageRet) {
                                Log.i("handle result ok,path[0]="+path[0]);
                            } else {
                                Log.e("handle image error");
                            }
                            CreateMomentActivity.WMediaFile photo = new CreateMomentActivity.WMediaFile();
                            photo.localPath = path[0];
                            photo.isFromGallery = false;
                            listPhoto.add(photo);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void errno) {
                            instance.notifyFileChanged(true);
                        }
                    }.execute(data);
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("moment_msg_content", etMomentMsgContent.getText().toString());
        outState.putString("share_range", tvShareRange.getText().toString());
        outState.putStringArrayList("share_range_limited_dep_list",moment.limitedDepartmentList);

        outState.putParcelable("media_helper", mediaHelper);
        outState.putParcelableArrayList("list_photo", listPhoto);
        outState.putStringArrayList("survey_options",surveyOptions);
        outState.putBoolean("survey_multi_selectable",isSuveyMultiSelectable);
        if(null != deadLineCalendar) {
            outState.putLong("survey_end_time",deadLineCalendar.getTimeInMillis());
        } else {
            outState.putLong("survey_end_time",0);
        }

        if(null != mLastVoiceFile && mLastVoiceFile.exists()) {
            outState.putString("last_voice_file",mLastVoiceFile.getAbsolutePath());
        }

        if(!TextUtils.isEmpty(moment.place)) {
            outState.putString("location_address",moment.place);
            if(0 != moment.latitude || 0 != moment.longitude) {
                outState.putDouble("location_latitude",moment.latitude);
                outState.putDouble("location_longitude",moment.longitude);
            }
        } else {
            outState.putString("location_address","");
            outState.putDouble("location_latitude",0);
            outState.putDouble("location_longitude",0);
        }
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }
}
