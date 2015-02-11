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
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import co.onemeter.oneapp.utils.LocationHelper;
import co.onemeter.oneapp.utils.MyUrlSpanHelper;
import co.onemeter.utils.AsyncTaskExecutor;

import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.Stamp;
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
public class MomentDetailActivity extends Activity implements View.OnClickListener,
        MomentAdapter.MomentActionHandler, InputBoardManager.InputResultHandler, InputBoardManager.ChangeToOtherAppsListener {

    private static final String IMAGE_CACHE_DIR = "image";
    public static final String EXTRA_REPLY_TO_MOMENT_ID = "reply_to_moment_id";
    public static final String EXTRA_REPLY_TO_REVIEW = "reply_to_review_id";
    /** 输出状态发生了变化的 moment id */
    public static final String EXTRA_CHANGED_MOMENT_ID = "changed_moment_id";
    /** 输出被删除了的 moment id */
    public static final String EXTRA_DELETED_MOMENT_ID = "deleted_moment_id";

    private ImageButton btnTitleBack;

    private ImageView imgPhoto;
    private ImageView imageView_tag_tea;
    private TextView txtName;
    private TextView txtTime;
    private TextView txtDate;
    private TextView txtContent;
    private TableLayout imageTable;
    private ImageButton momentOp;

    private LinearLayout micLayout;
    private ImageView btnPlay;
    private ProgressBar progressBar;
    private TimerTextView micTimer;
    private TextView txtLoc; 

    private LinearLayout reviewLayout;
    private SpannedTextView txtLikeName;
    private ImageView reviewDivider;
    private LinearLayout commentLayout;
    private Button btnLike;
//    private Button btnReview;
    private LinearLayout reviewButtons;
    private LinearLayout layoutLike;
    private LinearLayout layoutComment;
    private LinearLayout layoutAnswer;
//    private ImageView ivMomentFavorite;
    
  //详情页中的赞数和评论数
    private LinearLayout layout_detailcomment_review;
    private TextView textView_commentdetail_like;//赞
    private TextView textView_commentdetail_comment;//评论

    private WowTalkWebServerIF mWeb;
    private PrefUtil mPrefUtil;
    private MomentWebServerIF mMomentWeb;
    private Database dbHelper;
    private ImageResizer mImageResizer;
//    private MediaPlayer mPlayer;
    private InputBoardManager mInputMgr;
    private MessageBox mMsgBox;

    private Moment moment;
    private WFile voiceFile;
    private ArrayList<WFile> photoFiles;
    private BottomButtonBoard mMenu;

    private ArrayList<String> surveyMomentSelectedItem=new ArrayList<String>();

    private TimelineActivity.OnMomentReviewDeleteListener onMomentReviewDeleteListener;

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

//    private boolean isMomentMine() {
//        return mWeb.fGetMyUserIDFromLocal().equals(moment.owner.userID);
//    }

    private MediaPlayerWraper mediaPlayerWraper;
    

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);

        imgPhoto = (ImageView) findViewById(R.id.img_photo);
        imageView_tag_tea = (ImageView) findViewById(R.id.imageView_tag_tea);
        
        //根据moment获得帐号的类型
        if (moment.owner.getAccountType() == 2) {//此时的moment不为空//老师
        	imageView_tag_tea.setVisibility(View.VISIBLE);//显示标记
        } else {
        	imageView_tag_tea.setVisibility(View.GONE);
        }
        
        txtContent = (TextView) findViewById(R.id.txt_content);
        txtName = (TextView) findViewById(R.id.txt_name);
        txtTime = (TextView) findViewById(R.id.txt_time);
        txtDate = (TextView) findViewById(R.id.txt_date);
        imageTable = (TableLayout) findViewById(R.id.imageTable);
        micLayout = (LinearLayout) findViewById(R.id.mic_layout);
        btnPlay = (ImageView) findViewById(R.id.btn_play);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        micTimer = (TimerTextView) findViewById(R.id.mic_timer);
        txtLoc = (TextView) findViewById(R.id.txt_loc);
        reviewLayout = (LinearLayout) findViewById(R.id.reviewLayout);
        txtLikeName = (SpannedTextView) findViewById(R.id.txt_like_names);
        reviewDivider = (ImageView) findViewById(R.id.reviewDivider);
        commentLayout = (LinearLayout) findViewById(R.id.layout_review);
        btnLike = (Button) findViewById(R.id.btn_like);
//        btnReview = (Button) findViewById(R.id.btn_comment);
        reviewButtons = (LinearLayout) findViewById(R.id.review_btn);
        layoutLike = (LinearLayout) findViewById(R.id.layout_like);
        layoutComment = (LinearLayout) findViewById(R.id.layout_comment);
        layoutAnswer = (LinearLayout) findViewById(R.id.layout_answer);
//        ivMomentFavorite=(ImageView) findViewById(R.id.moment_favorite);
        
        //赞和评论的布局
        layout_detailcomment_review = (LinearLayout) findViewById(R.id.layout_detailcomment_review);
        layout_detailcomment_review.setVisibility(View.VISIBLE);//显示布局
        textView_commentdetail_like = (TextView) findViewById(R.id.textView_momentdetail_like);//赞
        textView_commentdetail_comment = (TextView) findViewById(R.id.textView_momentdetail_comment);//评论

        findViewById(R.id.moment_op_btns_layout_whole).setVisibility(View.GONE);
        
        momentOp=(ImageButton) findViewById(R.id.moment_op);
        
        
      if (getIntent().getIntExtra("isowner", 0) == 1) {//自己
      	momentOp.setVisibility(View.VISIBLE);
      } else {//好友
      	momentOp.setVisibility(View.GONE);
      }


//        boolean isMineMoment=mWeb.fGetMyUserIDFromLocal().equals(moment.owner.userID);
//        Log.i("moment detail mine? "+isMineMoment);

//        	ImageButton momentOp=(ImageButton) findViewById(R.id.moment_op);
//        ImageView ivMomentOpLeftDiv=(ImageView) findViewById(R.id.moment_op_left_div);
//        if(isMomentMine()) {
//            momentOp.setVisibility(View.VISIBLE);
//            ivMomentOpLeftDiv.setVisibility(View.VISIBLE); 
//        }

        if(!TextUtils.isEmpty(moment.tag) && moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_QA)) {
//            layoutLike.setVisibility(View.GONE);
            layoutComment.setVisibility(View.GONE);
            layoutAnswer.setVisibility(View.VISIBLE);
        }
        if(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(moment.tag) || Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) {
//            layoutLike.setVisibility(View.GONE);
            layoutComment.setVisibility(View.VISIBLE);
            layoutAnswer.setVisibility(View.GONE);
        }

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutLike.performClick();
            }
        });
        layoutLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLikeMoment_async(moment);
            }
        });

        findViewById(R.id.btn_comment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutComment.performClick();
            }
        });
        layoutComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replyToMoment(-1, moment, null, false);
            }
        });

        layoutComment.performClick();

        findViewById(R.id.btn_answer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutAnswer.performClick();
            }
        });
        layoutAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutComment.performClick();
            }
        });

        momentOp.setOnClickListener(this);
        btnTitleBack.setOnClickListener(this);
        imgPhoto.setOnClickListener(this);

//        MomentAdapter.setMomentFavoriteStatus(moment,ivMomentFavorite);
//        findViewById(R.id.moment_favorite_layout).setOnClickListener(this);

//        findViewById(R.id.new_moment_op_layout).setVisibility(View.GONE);
        reviewButtons.setVisibility(View.VISIBLE);
    }

    private void doLikeMoment_async(final Moment moment) {
        if (moment == null)
            return;
        if(moment.likedByMe) {
            Review likeReview=null;
            String mMyUid = mPrefUtil.getUid();
            for(Review aReview : moment.reviews) {
                if(Review.TYPE_LIKE == aReview.type && aReview.uid.equals(mMyUid)) {
                    likeReview=aReview;
                    break;
                }
            }
                      
            if(null != likeReview) {
                TimelineActivity.deleteMomentReview(this,moment.id,likeReview,onMomentReviewDeleteListener);
                moment.reviews.remove(likeReview);
                dbHelper.storeMoment(moment, moment.id);
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));
                
            } else {
                Log.e("delete like review null");
            }
            return;
        }
        final String momentId = moment.id;
        final Review r = new Review();
        r.id= "0";
        r.type = Review.TYPE_LIKE;
        r.uid = mPrefUtil.getUid();
        r.nickname = mPrefUtil.getMyNickName();
        r.read = true;

        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mMomentWeb.fReviewMoment(momentId, Review.TYPE_LIKE, null, null, r);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (ErrorCode.OK == result) {
                    moment.likedByMe = true;
                    moment.reviews.add(r);
                    addReviewToList(r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setLayoutForTimelineMoment(moment, getLikeBtnClickListener(momentId));
                        mInputMgr.setInputText("");
                    }

                    dbHelper.storeMoment(moment, moment.id);
                    //实现点赞异步刷新
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, momentId));
                } else {
                    mMsgBox.toast(R.string.msg_operation_failed);
                }
            }
        });
    }

    private void doReviewMoment_async(final Moment moment,
                                final String replyToReviewId,
                                final String strComment) {
        if (moment == null) return;
        final String moment_id = moment.id;
        final Review r = new Review();
        r.id = "0";
        r.uid = mPrefUtil.getUid();
        r.nickname = mPrefUtil.getMyNickName();
        r.type = Review.TYPE_TEXT;
        r.text = strComment;
        r.replyToReviewId = replyToReviewId;
        r.read = true;

        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    return mMomentWeb.fReviewMoment(moment_id, Review.TYPE_TEXT,
                            strComment, replyToReviewId, r);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    addReviewToList(r);

                    // clear inputted text
                    if (mInputMgr != null) {
                        mInputMgr.setInputText("");
                    }

                    moment.reviews.add(r);
                    dbHelper.storeMoment(moment, moment.id);
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));
                }
            }
        });
    }

    private void addReviewToList(Review r) {
        moment.reviews.add(moment.reviews.size(), r);
//        setupContent(moment);//去掉此方法的调用，否则在详情页中点赞，多一次刷新
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
                WowTalkWebServerIF.getInstance(MomentDetailActivity.this).fGetFileFromServer(file.fileid,
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
//        if (!(mPlayer != null && mPlayer.isPlaying())) {
//            micTimer.setText(String.format("%01d:%02d", file.duration / 60, file.duration % 60));
//            micTimer.setMaxElapse(file.duration);
//        }
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
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(localPath,file.duration);
        }

//        if (mPlayer != null && mPlayer.isPlaying()) {
//            mPlayer.seekTo(mPlayer.getDuration());
////            micTimer.stop();
////            micTimer.setMaxTime();
//////            micTimer.setText(String.format("%02d:%02d", (mPlayer.getDuration() / 1000) / 60,
//////                    (mPlayer.getDuration() / 1000) % 60));
////            btnPlay.setImageResource(R.drawable.timeline_player_play);
////            mPlayer.stop();
////            mPlayer.release();
////            mPlayer = null;
//        } else {
//            mPlayer = new MediaPlayer();
//            btnPlay.setImageResource(R.drawable.timeline_player_stop);
//            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, Void>() {
//                @Override
//                protected Void doInBackground(String... params) {
//                    try {
//                        mPlayer.setDataSource(params[0]);
//                        mPlayer.prepare();
//                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                            @Override
//                            public void onCompletion(MediaPlayer mp) {
//                                micTimer.stop();
//                                micTimer.setMaxTime();
//                                micTimer.setText(String.format("%01d:%02d", file.duration / 60, file.duration % 60));
////                        micTimer.setText(String.format("%02d:%02d", (mPlayer.getDuration() / 1000) / 60,
////                                (mPlayer.getDuration() / 1000) % 60));
//                                mPlayer.release();
//                                mPlayer = null;
//                                btnPlay.setImageResource(R.drawable.timeline_player_play);
//                            }
//                        });
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        mPlayer.release();
//                        mPlayer = null;
//                    }
//
//                    return null;
//                }
//
//                @Override
//                protected void onPostExecute(Void errno) {
//                    if(null != mPlayer) {
//                        mPlayer.start();
//                        micTimer.reset();
//                        micTimer.start();
//                    } else {
//                        if(null != mMsgBox) {
//                            mMsgBox.toast(R.string.operation_failed);
//                        }
//                        btnPlay.setImageResource(R.drawable.timeline_player_play);
//                    }
//                }
//            }, localPath);
//
////            try {
////                mPlayer.setDataSource(localPath);
////                mPlayer.prepare();
////                mPlayer.start();
////                micTimer.reset();
////                micTimer.start();
////                btnPlay.setBackgroundResource(R.drawable.stop_btn);
////                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
////                    @Override
////                    public void onCompletion(MediaPlayer mp) {
////                        micTimer.stop();
////                        micTimer.setMaxTime();
//////                        micTimer.setText(String.format("%02d:%02d", (mPlayer.getDuration() / 1000) / 60,
//////                                (mPlayer.getDuration() / 1000) % 60));
////                        mPlayer.release();
////                        mPlayer = null;
////                        btnPlay.setBackgroundResource(R.drawable.play_btn);
////                    }
////                });
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.moment_op:
                showMomentOpBottomDialog();
//                confirmDeleteMoment();
                break;
            case R.id.img_photo:
                TimelineActivity.launch(this, moment.owner.userID, TextUtils.isEmpty(moment.owner.alias)?moment.owner.nickName:moment.owner.alias);
                break;
//            case R.id.moment_favorite_layout:
//                MomentAdapter.triggerMomentFavorite(this,moment,ivMomentFavorite);
//                break;
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

    private void showMomentOpBottomDialog() {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this,
                findViewById(R.id.moment_op));
//        bottomBoard.add(moment.isFavorite?getString(R.string.un_favorite_moment):getString(R.string.favorite_moment),
//                BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                MomentAdapter.triggerMomentFavorite(MomentDetailActivity.this,moment,ivMomentFavorite);
//                bottomBoard.dismiss();
//            }
//        });
        String myUid = mPrefUtil.getUid();
        if(!TextUtils.isEmpty(myUid) && myUid.equals(moment.owner.userID)) {
            //moment is mine
//            bottomBoard.add(getString(R.string.view_share_range),
//                    BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Intent intent = new Intent(MomentDetailActivity.this,ShareRangeSelectActivity.class);
//                    intent.putStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS,moment.limitedDepartmentList);
//                    intent.putExtra(ShareRangeSelectActivity.SHOWN_ONLY,true);
//                    startActivity(intent);
//                    bottomBoard.dismiss();
//                }
//            });

            bottomBoard.add(getString(R.string.delete_moment),
                    BottomButtonBoard.BUTTON_RED, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmDeleteMoment();
                    bottomBoard.dismiss();
                }
            });
        }

        bottomBoard.addCancelBtn(getString(R.string.close));
        bottomBoard.show();
    }

    protected void onMessageTextClicked(final String message) {

        if (mInputMgr != null)
            mInputMgr.setSoftKeyboardVisibility(false);

        if (mMenu == null)
            mMenu = new BottomButtonBoard(this, findViewById(android.R.id.content));
        else
            mMenu.clearView();

        TextOperationHelper.fillMenu(this, mMenu,null, message, null, null, true);

        mMenu.show();
    }

    private IDBTableChangeListener momentObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(null == moment) {
                        finish();
                    } else {
                        moment=dbHelper.fetchMoment(moment.id);
                        setupContent(moment);

                        // 以便动态列表页面重新刷新评论数
                        setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHANGED_MOMENT_ID, moment.id));
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moment_detail);   

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        onMomentReviewDeleteListener=new TimelineActivity.OnMomentReviewDeleteListener() {
            @Override
            public void onMomentDelete(String momentId, Review review) {
                momentObserver.onDBTableChanged(Database.TBL_MOMENT);

                if (mInputMgr != null) {
                    mInputMgr.setLayoutForTimelineMoment(moment,new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            doLikeMoment_async(moment);
                        }
                    });
                }
            }
        };
        

//        mInputMgr = new InputBoardManager(this, (ViewGroup) findViewById(R.id.input_board_holder), this, this);
//        if (mInputMgr.setupViews()) {
//        	mInputMgr.mTxtContent.setFocusable(true);
//            mInputMgr.mTxtContent.setFocusableInTouchMode(true);
//            mInputMgr.mTxtContent.requestFocus();
//        }  
//        Handler hanlder = new Handler();
//        hanlder.postDelayed(new Runnable() {
//			
//			@Override
//			public void run() {
//				InputMethodManager imm = (InputMethodManager)mInputMgr.mTxtContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);  
//		        imm.showSoftInput(mInputMgr.mTxtContent, 0);
//			}
//		}, 200);
//        

        mediaPlayerWraper= new MediaPlayerWraper(this);

        mMsgBox = new MessageBox(this);
        dbHelper = Database.open(this);
        mWeb = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mMomentWeb = MomentWebServerIF.getInstance(this);

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));
        mImageResizer.setLoadingImage(R.drawable.feed_default_pic);
        mImageResizer.addImageCache(cacheParams);

        moment = getIntent().getParcelableExtra("moment");
        if(null == moment) {
            //as moment is created with alias_id
            //if MomentDetailActivity is started and onCreated has not been called,but moment_id has changed from server
            //moment fetch with old alias_id with be null
            finish();
            return;
        }
        initView();
        setupContent(moment);

        Database.addDBTableChangeListener(Database.TBL_MOMENT,momentObserver);
        Database.addDBTableChangeListener(Database.TBL_MOMENT_REVIEWS,momentObserver);

        dbHelper.setReviewsRead(moment);

        // 我们不知道是否以及何时提交投票，索性假设用户一定提交了投票，从而返回后上级页面总是刷新。
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

        if (dbHelper != null)
            dbHelper.setReviewsRead(moment);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mImageResizer.closeCache();
        Database.removeDBTableChangeListener(momentObserver);

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
            //txtLoc.setText(moment.place);
            LocationHelper.fetchAddressFromLatitudeAndLongitude(this, moment.latitude, moment.longitude, txtLoc);
            txtLoc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View paramView) {
					MessageDetailAdapter.viewLocationInfo(MomentDetailActivity.this,moment.latitude,moment.longitude,moment.place);					
				}
			});
        }

        if (moment.text == null || moment.text.equals("")) {
            txtContent.setVisibility(View.GONE);
        } else {
            txtContent.setVisibility(View.VISIBLE);
            txtContent.setText(moment.text);
//            MomentAdapter.setStringAsURLIfAvaliable(txtContent,moment.text,false);
            final MyUrlSpanHelper spanHelper = new MyUrlSpanHelper(txtContent);
            txtContent.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    spanHelper.onLongClicked();
                    if (moment != null && !Utils.isNullOrEmpty(moment.text)) {
                        onMessageTextClicked(moment.text);
                    }
                    return false;
                }
            });
        }

        photoFiles = new ArrayList<WFile>();
        if (moment != null && moment.multimedias != null && !moment.multimedias.isEmpty()) {
            for (WFile file : moment.multimedias) {
                if (file.isAudioByExt()) {
                    voiceFile = file;
                } else {
                    photoFiles.add(file);
                }
            }
        }


        showVoiceFile(voiceFile);
        MomentAdapter.setImageLayout(this, mImageResizer, photoFiles, imageTable);
        int nlikers = MomentAdapter.setViewForLikeReview(this, txtLikeName, moment.reviews);
        int nReviews = MomentAdapter.setViewForCommentReview(this, commentLayout, moment.reviews, -1, moment, this);
        
      //赞和评论的具体数量统计
        textView_commentdetail_like.setText(nlikers+"");
        textView_commentdetail_comment.setText(nReviews+"");
        
        txtLikeName.setVisibility(nlikers > 0 ? View.VISIBLE : View.GONE);
        reviewLayout.setVisibility(nlikers + nReviews > 0 ? View.VISIBLE : View.GONE);
        reviewDivider.setVisibility(nlikers > 0 && nReviews > 0 ? View.VISIBLE : View.GONE);

        // hide like and comment button if the moment is mine
//        String myuid = mWeb.fGetMyUserIDFromLocal();
//        if (moment.owner != null && moment.owner.userID.equals(myuid)) {
//            reviewButtons.setVisibility(View.GONE);
//        } else {
//            if (moment.likedByMe) {
//                btnLike.setEnabled(false);
//                layoutLike.setEnabled(false);
//                btnLike.setBackgroundResource(R.drawable.icon_btn_liked);
//            } else {
//                btnLike.setEnabled(true);
//                layoutLike.setEnabled(true);
//                btnLike.setBackgroundResource(R.drawable.icon_btn_like);
//            }
//        }

        if (moment.likedByMe) {
//            btnLike.setEnabled(false);
//            layoutLike.setEnabled(false);
            btnLike.setBackgroundResource(R.drawable.timeline_like_a);
        } else {
//            btnLike.setEnabled(true);
//            layoutLike.setEnabled(true);
            btnLike.setBackgroundResource(R.drawable.timeline_like);
        }

        TextView tvMomentTagDesc=(TextView) findViewById(R.id.moment_item_tag);
        ImageView ivMomentTagColor=(ImageView) findViewById(R.id.iv_moment_item_tag_color_flag);
        MomentAdapter.setTagdesc(this, moment, ivMomentTagColor, tvMomentTagDesc,
                (LinearLayout) findViewById(R.id.vote_survey_layout),
                (LinearLayoutAsListView) findViewById(R.id.vote_survey_options),
                surveyMomentSelectedItem,
                (Button) findViewById(R.id.vote_survey_button));

//        TextView tvMomentShareRange=(TextView) findViewById(R.id.tv_share_rang_ind);
//        ImageView ivMomentShareRange=(ImageView) findViewById(R.id.iv_share_rang_ind);
//        if(TextUtils.isEmpty(moment.visibility) || moment.visibility.equals(Moment.VISIBILITY_ALL)) {
//            tvMomentShareRange.setText(R.string.share_range_public_short);
//            ivMomentShareRange.setImageResource(R.drawable.timeline_public);
//        } else {
//            tvMomentShareRange.setText(R.string.share_range_private);
//            ivMomentShareRange.setImageResource(R.drawable.timeline_limited);
//        }
        
//        View.OnClickListener clickListener=new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MomentDetailActivity.this,ShareRangeSelectActivity.class);
//                intent.putStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS,moment.limitedDepartmentList);
//                intent.putExtra(ShareRangeSelectActivity.SHOWN_ONLY,true);
//                startActivity(intent);
//            }
//        };
        
//        tvMomentShareRange.setOnClickListener(clickListener);
//        ivMomentShareRange.setOnClickListener(clickListener);

//        String mMyUid = mPrefUtil.getUid();
//        if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && !moment.owner.userID.equals(mMyUid)) {
//            tvMomentShareRange.setVisibility(View.GONE);
//            ivMomentShareRange.setVisibility(View.GONE);
//        } else {
//            tvMomentShareRange.setVisibility(View.VISIBLE);
//            ivMomentShareRange.setVisibility(View.VISIBLE);
//        }
    }

    /**
     * 跳转到好友
     * @param context
     * @param moment
     */
    public static void launch(Context context, Moment moment) {
        Intent intent = new Intent(context, MomentDetailActivity.class);
        intent.putExtra("moment", moment);
        context.startActivity(intent);
    }
    
    /**
     * 跳转到自己
     * @param context
     * @param moment
     */
    public static void launchForOwner(Context context, Moment moment) {
        Intent intent = new Intent(context, MomentDetailActivity.class);
        intent.putExtra("moment", moment);
        intent.putExtra("isowner", 1);//给自己多传一个标志值
        context.startActivity(intent);
    }

    @Override
    public void replyToMoment(int momentPosition, final Moment moment, final Review replyTo, boolean like) {
        if (mMenu == null)
            mMenu = new BottomButtonBoard(this, findViewById(android.R.id.content));
        else
            mMenu.clearView();

        if (replyTo == null) {
            TimelineActivity.replyToMoment_helper(-1, moment.id, replyTo, this,
                    this, this, getLikeBtnClickListener(moment.id));
        } else {
            TimelineActivity.doWithReview(-1, moment.id, replyTo, mMenu, this, this,
                    this, onMomentReviewDeleteListener, getLikeBtnClickListener(moment.id));
        }
       
        //点击评论按钮，进入详情页自动弹起输入软键盘,而不是item
      if (mInputMgr != null && getIntent().getBooleanExtra("button_reply", false)) {
    	  
    	  mInputMgr.mTxtContent.setFocusable(true);
          mInputMgr.mTxtContent.setFocusableInTouchMode(true);
          mInputMgr.mTxtContent.requestFocus();
          
        Handler hanlder = new Handler();
        hanlder.postDelayed(new Runnable() {
    		
    		@Override
    		public void run() {
    			InputMethodManager imm = (InputMethodManager)mInputMgr.mTxtContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);  
    	        imm.showSoftInput(mInputMgr.mTxtContent, 0);
    		}
    	}, 200);
    	  
      }
      
      
      
    }

    @Override
    public void onMomentClicked(int position, Moment moment) {
        // nothing to do
    }

    private View.OnClickListener getLikeBtnClickListener(final String momentId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Moment m;
                if (TextUtils.equals(moment.id, momentId)) {
                    m = moment;
                } else {
                    m = dbHelper.fetchMoment(momentId);
                }
                if (null != m) {
                    doLikeMoment_async(m);
                }
            }
        };
    }


    @Override
    public void setInputBoardMangager(InputBoardManager m) {
        mInputMgr = m;
    }

    @Override
    public InputBoardManager getInputBoardMangager() {
        return mInputMgr;
    }

    @Override
    public void onHeightChanged(int height) {
    }

    @Override
    public void onTextInputted(String text) {
        if (mInputMgr == null)
            return;
        if (TextUtils.isEmpty(text.trim())) {
            mMsgBox.toast(R.string.comment_content_cannot_empty);
            return;
        }
        if (text.length() > TimelineActivity.COMMENT_MOST_WORDS) {
            mMsgBox.toast(String.format(getString(R.string.moments_comment_oom), TimelineActivity.COMMENT_MOST_WORDS));
            return;
        }
        mInputMgr.setSoftKeyboardVisibility(false);
//        mInputMgr.setSoftKeyboardVisibility(true);
        
        String mid = mInputMgr.extra().getString(EXTRA_REPLY_TO_MOMENT_ID);
        Review replyTo = mInputMgr.extra().getParcelable(EXTRA_REPLY_TO_REVIEW);
        if (mid == null)
            return;

        Moment m;
        if (TextUtils.equals(mid, moment.id)) {
            m = moment;
        } else {
            m = dbHelper.fetchMoment(mid);
        }
        if (m == null)
            return;
        doReviewMoment_async(m,
                replyTo == null ? null : replyTo.id,
                text);
//        mInputMgr.hide();
    }

    @Override
    public void onVoiceInputted(String path, int duration) {
        mMsgBox.show(null, getString(R.string.moments_voice_not_supported_in_review));
    }

    @Override
    public void onStampInputted(Stamp s) {
        mMsgBox.show(null, getString(R.string.moments_stamp_not_supported_in_review));
    }

    @Override
    public void onPhotoInputted(String path, String thumbPath) {
        mMsgBox.show(null, getString(R.string.moments_photo_not_supported_in_review));
    }

    @Override
    public void onHybirdInputted(String text, String imagePath, String imageThumbPath, String voicePath, int voiceDuration) {
        mMsgBox.show(null, getString(R.string.moments_hybird_not_supported_in_review));
    }

    @Override
    public void onHybirdRequested() {
        mMsgBox.show(null, getString(R.string.moments_hybird_not_supported_in_review));
    }

    @Override
    public void onVideoInputted(String path, String thumbPath) {
        mMsgBox.show(null, getString(R.string.moments_video_not_supported_in_review));
    }

    @Override
    public void onVideoChatRequested() {

    }

    @Override
    public void onCallRequested() {
    }

    @Override
    public void onLocationInputted(double latitude, double longitude, String address) {
        mMsgBox.show(null, getString(R.string.moments_loc_not_supported_in_review));
    }

    @Override
    public void onBackPressed() {
        // back steps:
        // multimedia -> text -> hide -> back
        if (mInputMgr != null && mInputMgr.isShowing()) {
            if (mInputMgr.showingFlags() == InputBoardManager.FLAG_SHOW_TEXT) {
//                mInputMgr.hide();
            } else {
                mInputMgr.show(InputBoardManager.FLAG_SHOW_TEXT);
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void toastCannotSendMsg() {
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }

	@Override
	public void willRecordAudio() {
		
	}
}
