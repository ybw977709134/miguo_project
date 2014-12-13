package org.wowtalk.ui.msg;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.androidquery.AQuery;

import java.io.File;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybird_image_voice_text_player);


        AQuery q = new AQuery(this);

        Bundle b = getIntent().getExtras();
        q.find(R.id.img_msg_image).image(new File(b.getString(EXTRA_IN_IMAGE_FILENAME)), 0);
        q.find(R.id.txt_msg_text).text(b.getString(EXTRA_IN_TEXT));
        q.find(R.id.btn_msg_audio).text(b.getString(EXTRA_IN_VOICE_FILENAME))
                .clicked(this);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btn_msg_audio) {
            // TODO play
        }
    }
}