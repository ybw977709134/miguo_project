package org.wowtalk.ui.msg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * <p>“图文音”混合消息的编辑器。</p>
 * Created by pzy on 12/7/14.
 */
public class HybirdImageVoiceTextEditor extends Activity {
    /** 输入图片绝对路径。*/
    public static final String EXTRA_IN_IMAGE_FILENAME = "in_image_filename";
    /** 输出图片绝对路径。*/
    public static final String EXTRA_OUT_IMAGE_FILENAME = "out_image_filename";
    /** 输出语音绝对路径。*/
    public static final String EXTRA_OUT_VOICE_FILENAME = "out_voice_filename";
    /** 输出语音时长（秒）。*/
    public static final String EXTRA_OUT_VOICE_DURATION = "out_voice_duration";
    /** 输出消息文本。*/
    public static final String EXTRA_OUT_TEXT = "out_text";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybird_image_voice_text);

        Bundle extras = getIntent().getExtras();
        String imageFilename = extras.getString(EXTRA_IN_IMAGE_FILENAME);

        // TODO
        // #1 把输入的图片显示为背景；
        // #2 让用户输入语音音；
        // #3 让用户输入文字；
        // #4 预览以上内容；
        // #5 通过 EXTRA_OUT_* 输出以上内容；
        Bundle outData = new Bundle();
        outData.putString(EXTRA_OUT_TEXT, "hello hybird");
        outData.putString(EXTRA_OUT_IMAGE_FILENAME, imageFilename);
        outData.putString(EXTRA_OUT_VOICE_FILENAME, null);
        outData.putInt(EXTRA_OUT_VOICE_DURATION, 0);

        setResult(RESULT_OK, new Intent().putExtras(outData));
    }
}