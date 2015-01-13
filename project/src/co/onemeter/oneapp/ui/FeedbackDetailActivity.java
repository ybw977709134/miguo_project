package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.MyUrlSpanHelper;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.TimerTextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jianxingdong
 * Date: 13-5-15
 * Time: PM3:55
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackDetailActivity extends Activity implements View.OnClickListener{

    private static final String IMAGE_CACHE_DIR = "image";
    public static final String EXTRA_REPLY_TO_MOMENT_ID = "reply_to_moment_id";
    public static final String EXTRA_REPLY_TO_REVIEW = "reply_to_review_id";
    /** 输出状态发生了变化的 moment id */
    public static final String EXTRA_CHANGED_MOMENT_ID = "changed_moment_id";
    /** 输出被删除了的 moment id */
    public static final String EXTRA_DELETED_MOMENT_ID = "deleted_moment_id";

    private ImageButton btnTitleBack;

    private ImageView imgPhoto;
    private TextView txtName;
    private TextView txtTime;
    private TextView txtDate;
    private TextView txtContent;

    private LinearLayout micLayout;
    private ImageView btnPlay;
    private ProgressBar progressBar;
    private TimerTextView micTimer;
    private TextView txtLoc; 

    private SpannedTextView txtLikeName;

    private MomentWebServerIF mMomentWeb;
    private Database dbHelper;
    private ImageResizer mImageResizer;
    private MessageBox mMsgBox;

    private Moment moment;
    private WFile voiceFile;
    private ArrayList<WFile> photoFiles;

    private final static int MSG_ID_MOMENT_DELETE_WITH_DELAY_FINISH=1;
    private Handler mHandler=new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_MOMENT_DELETE_WITH_DELAY_FINISH:
                    mMsgBox.dismissWait();
                    finish();
                    break;
                default:
                    break;
            }
        }
    } ;

    private MediaPlayerWraper mediaPlayerWraper;
    
    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);

        imgPhoto = (ImageView) findViewById(R.id.img_photo);
        txtContent = (TextView) findViewById(R.id.txt_content);
        txtName = (TextView) findViewById(R.id.txt_name);
        txtTime = (TextView) findViewById(R.id.txt_time);
        txtDate = (TextView) findViewById(R.id.txt_date);
        micLayout = (LinearLayout) findViewById(R.id.mic_layout);
        btnPlay = (ImageView) findViewById(R.id.btn_play);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        micTimer = (TimerTextView) findViewById(R.id.mic_timer);
        txtLoc = (TextView) findViewById(R.id.txt_loc);
        txtLikeName = (SpannedTextView) findViewById(R.id.txt_like_names);
        
        findViewById(R.id.reviewLayout).setVisibility(View.GONE);
        
        btnTitleBack.setOnClickListener(this);

    }


    private void showVoiceFile(WFile file) {
        if (file == null) {
            micLayout.setVisibility(View.GONE);
        } else {
            micLayout.setVisibility(View.VISIBLE);
            setViewForVoice(file);
        }
    }

    private void getVoiceFileFromServer(final WFile file) {
        final String path = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());
        btnPlay.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
            boolean ok;

            @Override
            protected Void doInBackground(Void... voids) {
                WowTalkWebServerIF.getInstance(FeedbackDetailActivity.this).fGetFileFromServer(file.fileid,
                        GlobalSetting.S3_MOMENT_FILE_DIR,
                        new NetworkIFDelegate() {
                            @Override
                            public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                ok = true;
                            }

                            @Override
                            public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                ok = false;
                            }

                            @Override
                            public void setProgress(int i, int i2) {
                                publishProgress(i2);
                            }
                        }, 0, path, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                btnPlay.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                setViewForVoice(file);
                startPlayingVoice(path, file);
            }

            @Override
            protected void onProgressUpdate(Integer... params) {
                if (progressBar != null) {
                    progressBar.setProgress(params[0]);
                }
            }
        });
    }

    private void setViewForVoice(final WFile file) {
        btnPlay.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        final String localPath = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());

        if(localPath.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            btnPlay.setImageResource(R.drawable.timeline_player_stop);
            mediaPlayerWraper.setPlayingTimeTV(micTimer,true);
        } else {
            btnPlay.setImageResource(R.drawable.timeline_player_play);
            micTimer.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
        }
        if (new File(localPath).exists()) {
           micLayout.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   startPlayingVoice(localPath,file);
               }
           });
        } else {
            micLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getVoiceFileFromServer(file);
                }
            });
        }
    }

    private void startPlayingVoice(String localPath,final WFile file) {
        if(!localPath.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(micTimer,true);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    btnPlay.setImageResource(R.drawable.timeline_player_play);
                }

                @Override
                public void onPlayBegin(String path) {
                    btnPlay.setImageResource(R.drawable.timeline_player_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    btnPlay.setImageResource(R.drawable.timeline_player_play);
                }
            });
            mediaPlayerWraper.triggerPlayer(localPath,file.duration);
        } else {
            mediaPlayerWraper.triggerPlayer(localPath,file.duration);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    private void confirmDeleteMoment() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.contacts_local_delete)
                .setMessage(R.string.delete_moment)
                .setPositiveButton(R.string.msg_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int whichButton) {
                                doDeleteMoment();
                            }
                        })
                .setNegativeButton(R.string.msg_cancel, null)
                .show();
    }

    private void doDeleteMoment() {
        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mMomentWeb.fDeleteMoment(moment.id);
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (ErrorCode.OK == errno) {
                    mHandler.sendEmptyMessageDelayed(MSG_ID_MOMENT_DELETE_WITH_DELAY_FINISH, 1000);
                    dbHelper.deleteMoment(moment.id);
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_DELETED_MOMENT_ID, moment.id));
                } else {
                    mMsgBox.toast(R.string.operation_failed);
                    mMsgBox.dismissWait();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_detail);
        int isShow = getIntent().getIntExtra("isowner", 0);
        
        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mediaPlayerWraper= new MediaPlayerWraper(this);

        mMsgBox = new MessageBox(this);
        dbHelper = Database.open(this);
        mMomentWeb = MomentWebServerIF.getInstance(this);

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        mImageResizer.setLoadingImage(R.drawable.feed_default_pic);
        mImageResizer.addImageCache(cacheParams);

        moment = getIntent().getParcelableExtra("moment");
        if(null == moment) {
            finish();
            return;
        }
        initView();
        setupContent(moment);

        if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(moment.tag)
                || Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageResizer.setExitTasksEarly(false);
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageResizer.setPauseWork(false);
        mImageResizer.setExitTasksEarly(true);
        mImageResizer.flushCache();

        mImageResizer.clearCacheInMem();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mImageResizer.closeCache();
        mediaPlayerWraper.stop();
    }

    private void setupContent(final Moment moment) {
        if(null == moment) {
            return;
        }
        txtName.setText(TextUtils.isEmpty(moment.owner.alias) ? moment.owner.nickName : moment.owner.alias);
        long time = Long.valueOf(moment.timestamp * 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        String strDate = format.format(time);
        format = new SimpleDateFormat("HH:mm");
        String strTime = format.format(time);
        txtDate.setText(strDate);
        txtTime.setText(strTime); 

        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, moment.owner, true);
        if (moment.latitude == 0 && moment.longitude == 0) { // XXX compare double with int?!
            txtLoc.setVisibility(View.GONE);
            txtLoc.setOnClickListener(null);
        } else {
            txtLoc.setVisibility(View.VISIBLE);
            txtLoc.setText(moment.place);
            txtLoc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View paramView) {
					MessageDetailAdapter.viewLocationInfo(FeedbackDetailActivity.this,moment.latitude,moment.longitude,moment.place);					
				}
			});
        }

        if (moment.text == null || moment.text.equals("")) {
            txtContent.setVisibility(View.GONE);
        } else {
            txtContent.setVisibility(View.VISIBLE);
            txtContent.setText(moment.text);
            final MyUrlSpanHelper spanHelper = new MyUrlSpanHelper(txtContent);
            txtContent.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    spanHelper.onLongClicked();
                    if (moment != null && !Utils.isNullOrEmpty(moment.text)) {
                    }
                    return false;
                }
            });
        }

        photoFiles = new ArrayList<WFile>();
        if (moment != null && moment.multimedias != null && !moment.multimedias.isEmpty()) {
            for (WFile file : moment.multimedias) {
                if (file.getExt().equals("aac") || file.getExt().equals("m4a") || file.getExt().equals("3gpp")) {
                    voiceFile = file;
                } else {
                    photoFiles.add(file);
                }
            }
        }


        showVoiceFile(voiceFile);
    }

    /**
     * 跳转到好友
     * @param context
     * @param moment
     */
    public static void launch(Context context, Moment moment) {
        Intent intent = new Intent(context, FeedbackDetailActivity.class);
        intent.putExtra("moment", moment);
        context.startActivity(intent);
    }
    
    /**
     * 跳转到自己
     * @param context
     * @param moment
     */
    public static void launchForOwner(Context context, Moment moment) {
        Intent intent = new Intent(context, FeedbackDetailActivity.class);
        intent.putExtra("moment", moment);
        intent.putExtra("isowner", 1);//给自己多传一个标志值
        context.startActivity(intent);
    }

}
