package org.wowtalk.ui.msg;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import junit.framework.Assert;

import java.io.File;
import java.io.IOException;

/**
 * <p>“图文音”混合消息的播放器。</p>
 * Created by pzy on 12/7/14.
 */
public class HybirdImageVoiceTextPreview extends Activity implements View.OnClickListener {
    /** 输入图片绝对路径。*/
    public static final String EXTRA_IN_IMAGE_FILENAME = "in_image_filename";
    /** 输入语音绝对路径。*/
    public static final String EXTRA_IN_VOICE_FILENAME = "in_voice_filename";
    /** 输入语音时长（秒）。*/
    public static final String EXTRA_IN_VOICE_DURATION = "in_voice_duration";
    /** 输入消息文本。*/
    public static final String EXTRA_IN_TEXT = "in_text";

    String audioFilename;
    MediaPlayer mediaPlayer;
    Thread audioPositionMonitorThrd;
    View audioProgressBarElapsed;
    View audioProgressBarRemained;
    TextView audioButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybird_image_voice_text_player);

        AQuery q = new AQuery(this);

        Bundle b = getIntent().getExtras();
        q.find(R.id.img_msg_image).image(new File(b.getString(EXTRA_IN_IMAGE_FILENAME)), 0);
        q.find(R.id.txt_msg_text).text(b.getString(EXTRA_IN_TEXT));
        audioButton = q.find(R.id.btn_msg_audio).clicked(this).getTextView();

        audioProgressBarElapsed = q.find(R.id.progressbar_elapsed).getView();
        audioProgressBarRemained = q.find(R.id.progressbar_remained).getView();

        audioFilename = b.getString(EXTRA_IN_VOICE_FILENAME);
        if (audioFilename != null && new File(audioFilename).exists()) {
            playAudio();
        } else {
            q.find(R.id.btn_msg_audio).invisible();
        }

        startAudioPositionMonitorThread();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btn_msg_audio) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    audioButton.setText(R.string.msg_audio_resume);
                } else {
                    mediaPlayer.start();
                    audioButton.setText(R.string.msg_audio_pause);
                }
            } else {
                replayAudio();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }

        stopAudioPositionMonitorThread();
    }

    /**
     * Init and play.
     */
    private void playAudio() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFilename);
            mediaPlayer.prepare();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    audioButton.setText(R.string.msg_audio_restart);
                }
            });

            setAudioProgress(0);
            audioButton.setText(R.string.msg_audio_pause);
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer = null;
        }
    }

    /**
     * Reset and play.
     */
    private void replayAudio() {
        Assert.assertTrue(mediaPlayer != null);
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (!mediaPlayer.isPlaying())
                mediaPlayer.start();
        }
    }

    private void startAudioPositionMonitorThread() {
        audioPositionMonitorThrd = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAudioProgress(mediaPlayer.getCurrentPosition() * 100 / mediaPlayer.getDuration());
                        }
                    });

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        audioPositionMonitorThrd.start();
    }

    private void stopAudioPositionMonitorThread() {
        if (audioPositionMonitorThrd != null) {
            audioPositionMonitorThrd.interrupt();
        }
    }

    private void setAudioProgress(int progress) {

        audioProgressBarElapsed.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                100 - progress));

        audioProgressBarRemained.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                progress));
    }
}