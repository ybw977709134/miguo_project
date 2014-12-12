package org.wowtalk.ui.msg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    
    private LinearLayout layout_hybird_image_voice_text_record;//录音布局
    private LinearLayout layout_hybird_image_voice_text_endrecord;//结束录音后的布局
    private ImageView imageView_hybird_image_voice_text_record;//录音图片
    private TextView textView_hybird_image_voice_text_recordtime;//录音时间1
    private TextView textView_hybird_image_voice_text_recorddown;//按下，能录120秒
    private TextView textView_hybird_image_voice_text_endrecordsound;//点击结束录音
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybird_image_voice_text);

//        Bundle extras = getIntent().getExtras();
//        String imageFilename = extras.getString(EXTRA_IN_IMAGE_FILENAME);

        // TODO
        // #1 把输入的图片显示为背景；
        // #2 让用户输入语音音；
        // #3 让用户输入文字；
        // #4 预览以上内容；
        // #5 通过 EXTRA_OUT_* 输出以上内容；
//        Bundle outData = new Bundle();
//        outData.putString(EXTRA_OUT_TEXT, "hello hybird");
//        outData.putString(EXTRA_OUT_IMAGE_FILENAME, imageFilename);
//        outData.putString(EXTRA_OUT_VOICE_FILENAME, null);
//        outData.putInt(EXTRA_OUT_VOICE_DURATION, 0);
//
//        setResult(RESULT_OK, new Intent().putExtras(outData));
        
        //实例化各个控件对象
        layout_hybird_image_voice_text_record = (LinearLayout) findViewById(R.id.layout_hybird_image_voice_text_record);
        layout_hybird_image_voice_text_endrecord = (LinearLayout) findViewById(R.id.layout_hybird_image_voice_text_endrecord);
        imageView_hybird_image_voice_text_record = (ImageView) findViewById(R.id.imageView_hybird_image_voice_text_record);
        textView_hybird_image_voice_text_recordtime = (TextView) findViewById(R.id.textView_hybird_image_voice_text_recordtime);
        textView_hybird_image_voice_text_recorddown = (TextView) findViewById(R.id.textView_hybird_image_voice_text_recorddown);
        textView_hybird_image_voice_text_endrecordsound = (TextView) findViewById(R.id.textView_hybird_image_voice_text_endrecordsound);
    }
    
    public void clickButton(View view){
	
    	if (R.id.imageButton_hybird_image_voice_text_back == view.getId()) {//返回
    		finish();
    		
    	} else if (R.id.textView_hybird_image_voice_text_preview == view.getId()) {//预览
    		Intent intent = new Intent(HybirdImageVoiceTextEditor.this,HybirdImageVoiceTextPreview.class);
    		startActivity(intent);
    		
    	} else if (R.id.textView_hybird_image_voice_text_upload == view.getId()) {//点击添加图片//点击图片图标能查看大图
    		
    	} else if (R.id.textView_hybird_image_voice_text_recorddown == view.getId()) {//点击录音
    		imageView_hybird_image_voice_text_record.setImageResource(R.drawable.timeline_record_a);
    		textView_hybird_image_voice_text_recordtime.setVisibility(View.VISIBLE);
    		textView_hybird_image_voice_text_recorddown.setVisibility(View.GONE);
    		textView_hybird_image_voice_text_endrecordsound.setVisibility(View.VISIBLE);
    		
    	} else if (R.id.textView_hybird_image_voice_text_endrecordsound == view.getId()) {//点击结束录音
    		layout_hybird_image_voice_text_record.setVisibility(View.GONE);
    		layout_hybird_image_voice_text_endrecord.setVisibility(View.VISIBLE);
    		
    	} else if (R.id.imageView_hybird_image_voice_text_record_play == view.getId()) {//点击播放
    		
    	} else if (R.id.imageView_hybird_image_voice_text_endrecord_cancel == view.getId()) {//点击取消
    		layout_hybird_image_voice_text_endrecord.setVisibility(View.GONE);
    		layout_hybird_image_voice_text_record.setVisibility(View.VISIBLE);
    		imageView_hybird_image_voice_text_record.setImageResource(R.drawable.messages_icon_more_voice);
    		textView_hybird_image_voice_text_recordtime.setVisibility(View.GONE);
    		textView_hybird_image_voice_text_endrecordsound.setVisibility(View.GONE);
    		textView_hybird_image_voice_text_recorddown .setVisibility(View.VISIBLE);
    		
    	} 
    }
}