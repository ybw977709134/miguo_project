package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.TextView;
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.Database;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.TimerTextView;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-10-10
 * Time: 下午2:05
 * To change this template use File | Settings | File Templates.
 */
public class MediaPlayerWraper {
    private Activity activityRef;
    private MediaPlayer mediaPlayerInstance;

    private String mediaPath, temMediaPath;
    private TextView tvShowPlayingTime;
    private boolean withFullTimeAppend;

    private MediaPlayerWraperListener wraperListener;
    private TimeElapseReportRunnable playingVoiceRunnable;

    private MessageBox mMsgBox;
    private long duration;
    private boolean isFromMomentAdapter = true;//针对从MomentAdapter中 new的对象，加上特殊条件 bug549相关
    public MediaPlayerWraper(Activity activity) {
        activityRef=activity;
        mediaPlayerInstance=null;

        mMsgBox = new MessageBox(activity);
        isFromMomentAdapter = false;
    }

    public MediaPlayerWraper(Activity activity, boolean isFromMomentAdapter) {
        activityRef=activity;
        mediaPlayerInstance=null;

        mMsgBox = new MessageBox(activity);
        isFromMomentAdapter = true;
    }
    
    public void setPlayingTimeTV(TextView tv,boolean append) {
        tvShowPlayingTime=tv;
        withFullTimeAppend=append;
    }

    public void setWraperListener(MediaPlayerWraperListener listener) {
        wraperListener=listener;
    }
    
    public String getPlayingMediaPath() {
        return mediaPath;
    }

    public void setTemMediaPath(String temMediaPath)
    {
    	this.temMediaPath = temMediaPath;
    }
    
    public boolean isSameTvPlayTime(TextView v)
    {
    	return tvShowPlayingTime == v;
    }
    
    public void triggerPlayer(String path,long preSize) {
        if(TextUtils.isEmpty(path)) {
            return;
        }
        if(!TextUtils.isEmpty(mediaPath)) {
            String oldPath=mediaPath;
            stop();
            //if same media playing,stop only,no replay
            if(path.equals(oldPath)) {
                return;
            }
        }
        duration=preSize;
        mediaPath=path;
//        mMsgBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<String, Void, MediaPlayer>() {
            @Override
            protected MediaPlayer doInBackground(String... params) {
                MediaPlayer player = new MediaPlayer();

                try {
                    player.setDataSource(params[0]);
                    player.prepare();
                    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            stop();
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();

                    if (null != player) {
                        player.release();
                        player = null;
                    }

                    Database.deleteAFile(params[0]);
                }

                return player;
            }

            @Override
            protected void onPostExecute(MediaPlayer player) {
//                mMsgBox.dismissWait();

                if (null != player) {
                    if (TextUtils.isEmpty(mediaPath)) {
                        player.release();
                    } else {
                        mediaPlayerInstance = player;

                        duration = mediaPlayerInstance.getDuration();

                        //start thread to set time accordingly
                        playingVoiceRunnable = new TimeElapseReportRunnable();
                        playingVoiceRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
                            @Override
                            public void reportElapse(final long elapsed) {
                                activityRef.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (null != mediaPlayerInstance) {
                                            setTime();
                                        }
                                    }
                                });
                            }
                        });
                        new Thread(playingVoiceRunnable).start();

                        //notify begin
                        if (null != wraperListener) {
                            wraperListener.onPlayBegin(mediaPath);
                        }

                        mediaPlayerInstance.start();

                        setTime();
                    }
                } else {
                    if (null != wraperListener) {
                        wraperListener.onPlayFail(mediaPath);
                    }
                    stop();
                }
            }
        }, mediaPath);
    }

    private long savedLastMediaPosition=0;
    private void setTime() {
        if(null == tvShowPlayingTime) {
            return;
        }
        //MomentActivity中切换tag时，时间更新出错的bug
        if(isFromMomentAdapter && !mediaPath.equals(temMediaPath))
        {
//        	tvShowPlayingTime.setText(makeMyTimeDisplayFromMS(duration));
        	return;
        }
        if(null == mediaPlayerInstance) {
            tvShowPlayingTime.setText(makeMyTimeDisplayFromMS(duration));
        } else {
            long curPosition=mediaPlayerInstance.getCurrentPosition();
            if(curPosition < savedLastMediaPosition) {
                //I donot know why it can get smaller position when near finish,but it's true
                //so fix it with last position
                //can not show smaller times as time go on(00:04->00:03)
                curPosition=savedLastMediaPosition;
            } else {
                savedLastMediaPosition = curPosition;
            }
            String time = makeMyTimeDisplayFromMS(curPosition);
            if (withFullTimeAppend) {
                time += "/"+makeMyTimeDisplayFromMS(mediaPlayerInstance.getDuration());
            }
            tvShowPlayingTime.setText(time);
        }
    }

    public void stop() {
        if(null != mediaPlayerInstance) {
            mediaPlayerInstance.release();
            mediaPlayerInstance=null;
        }

        setTime();

        savedLastMediaPosition=0;

        mediaPath=null;
        duration=0;
        tvShowPlayingTime=null;

        if(null != playingVoiceRunnable) {
            playingVoiceRunnable.stop();
        }
        playingVoiceRunnable=null;

        if(null != wraperListener) {
            wraperListener.onPlayComplete(mediaPath);
        }
        wraperListener=null;
    }

    public static String makeMyTimeDisplayFromMS(long ms) {
        long seconds=ms/1000;
        return String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, seconds/60, seconds % 60);
    }

    public static interface MediaPlayerWraperListener {
        void onPlayFail(String path);
        void onPlayBegin(String path);
        void onPlayComplete(String path);
    }
}
