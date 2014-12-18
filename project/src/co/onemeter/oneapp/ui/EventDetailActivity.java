package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.ImageViewActivity;
import org.wowtalk.ui.MessageBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class EventDetailActivity extends Activity implements OnClickListener {
    private static final int NUM_COLUMNS = 4;

    public static final String INTENT_EXTRA_SIGNUP = "intent_extra_signup";
	
	private WEvent eventDetail;
    private MessageBox msgbox;
    private TextView btn_right_up;
    private TextView btn_right_down;

    private void downloadImage(final WFile aFile, final boolean thumbnail, final ImageView imageView) {

        new AsyncTask<Void, Integer, Void> () {
            boolean ok = true;

            @Override
            protected Void doInBackground(Void... arg0) {
                WowTalkWebServerIF.getInstance(EventDetailActivity.this)
                        .fGetFileFromServer(thumbnail ? aFile.thumb_fileid : aFile.fileid,
                                WEvent.MEDIA_FILE_REMOTE_DIR, new NetworkIFDelegate(){
                                    @Override
                                    public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                                        ok = false;
                                    }
                                    @Override
                                    public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                                        ok = true;
                                    }
                                    @Override
                                    public void setProgress(int arg0, int arg1) {
                                    }

                                },
                                0,
                                thumbnail ? aFile.localThumbnailPath : aFile.localPath,
                                null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if(ok) {
                    displayImage(thumbnail ? aFile.localThumbnailPath : aFile.localPath, imageView);
                }
            }

        }.execute((Void)null);
    }

    private boolean isFileAPhoto(String fileName) {
        return fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".tif") ||
                fileName.endsWith(".png");
    }

	private void initGallery() {
		ViewGroup gallery = (ViewGroup) findViewById(R.id.images_grid);

        int numImages = 0;

        if(null != eventDetail.multimedias && !eventDetail.multimedias.isEmpty()) {
            ArrayList<String> eventImageList=new ArrayList<String>();

            LinearLayout row = addImageRow(gallery);
            for(WFile aFile : eventDetail.multimedias) {
                String filename = aFile.localThumbnailPath;
                if (!isFileAPhoto(filename))
                    continue;

                // display big image
                if (numImages == 0) {
                    ImageView imageView = (ImageView)findViewById(R.id.image_cover);
                    if (!displayImage(aFile.localPath, imageView)) {
                        downloadImage(aFile, false, imageView);
                    }
                }
                // else
                {
                    eventImageList.add(aFile.localThumbnailPath);

                    // create image view
                    ImageView imageView = addImageView(row, numImages);

                    // set image source
                    if (!displayImage(filename, imageView)) {
                        imageView.setImageResource(R.drawable.feed_default_pic);
                        downloadImage(aFile, true, imageView);
                    }
                }

                if (++numImages % NUM_COLUMNS == 0) {
                    row = addImageRow(gallery);
                }
            }
        }

        if (numImages == 0) {
            new AQuery(this).find(R.id.images_section).visibility(View.GONE);
        }
	}

    private boolean displayImage(String filename, ImageView imageView) {
        boolean ok = false;
        try {
            if (new File(filename).exists()) {
                InputStream in = new FileInputStream(filename);
                Drawable drawable = Drawable.createFromStream(in, null);
                imageView.setImageDrawable(drawable);
                ok = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ok;
    }

    private ImageView addImageView(LinearLayout row, final int position) {
        int padding = 10;
        int width = (GlobalValue.screenW - (NUM_COLUMNS + 1) * padding) / NUM_COLUMNS;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);
        lp.setMargins(padding, padding, position < NUM_COLUMNS - 1 ? 0 : padding, padding);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(lp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(imageView);

        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageViewActivity.launch(EventDetailActivity.this,
                        position,
                        eventDetail.multimedias,
                        ImageViewActivity.UPDATE_WITH_CHAT_MESSAGE);
            }
        });
        return imageView;
    }

    private LinearLayout addImageRow(ViewGroup gallery) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gallery.addView(row);
        return row;
    }

    @Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_button:
                finish();
                break;
            case R.id.right_button_up:
                joinEvent();//参加报名
                btn_right_up.setVisibility(View.GONE);
                btn_right_down.setVisibility(View.VISIBLE);
                break;
            case R.id.right_button_down:
            	Builder builder = new AlertDialog.Builder(EventDetailActivity.this);
            	builder.setTitle("提示");
            	builder.setMessage("你确认取消报名吗？");
            	builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancel_join_event();//取消报名
						btn_right_down.setVisibility(View.GONE);
		            	btn_right_up.setVisibility(View.VISIBLE);
		            	Toast.makeText(EventDetailActivity.this, R.string.require_cancel_event_joined, Toast.LENGTH_SHORT).show();
					}
				});
            	builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
            	
            	builder.create().show();//显示取消报名的对话框           	
            
            	break;
            default:
                break;
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_detail);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        
        btn_right_up = (TextView) findViewById(R.id.right_button_up);
        btn_right_down = (TextView) findViewById(R.id.right_button_down);

        eventDetail = getIntent().getExtras().getParcelable(EventActivity.EVENT_DETAIL_BUNDLE);
        
        

        if (eventDetail == null) { 
            return;
        } else {
        	if (eventDetail.membership == WEvent.MEMBER_SHIP_JOINED) {
        		btn_right_up.setVisibility(View.GONE);
        		btn_right_down.setVisibility(View.VISIBLE);
        	}
        }
        
        	
        
        msgbox = new MessageBox(this);

		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    private void joinEvent() {
   //     msgbox.showWait();
        new AsyncTask<Void, Void, Integer>(){

            @Override
            protected Integer doInBackground(Void... voids) {
                WowEventWebServerIF web = WowEventWebServerIF.getInstance(EventDetailActivity.this);
                return web.fJoinEvent(eventDetail.id);
               
            }

            @Override
            public void onPostExecute(Integer errno) {
     //           msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    msgbox.toast(R.string.require_join_event_success);
                    refresh(false,false);
                } else {
                    msgbox.toast(R.string.require_join_event_joined);
                }
            }
        }.execute((Void)null);
    }
    
    /**
     * 取消报名
     */
    private void cancel_join_event(){
 //  	 msgbox.showWait();
    	 new AsyncTask<Void, Void, Integer>(){

            @Override
            protected Integer doInBackground(Void... voids) {
                 WowEventWebServerIF web = WowEventWebServerIF.getInstance(EventDetailActivity.this);
                return web.fDeleteEvent(eventDetail.id);                
             }

             @Override
             public void onPostExecute(Integer errno) {
      //           msgbox.dismissWait();
                 if (errno == ErrorCode.OK) {
     //                msgbox.toast(R.string.require_cancel_event_joined);
                     refresh(false,false);
                 } else {
     //               msgbox.toast(R.string.require_cancel_event_fail);
                 }
             }
         }.execute((Void)null);
    	
    }

    private void refresh(final boolean showProgressBar,final boolean needUpdateGallery) {
        if (showProgressBar)
            msgbox.showWait();

        new AsyncTask<Void, Void, Integer>(){

            @Override
            protected Integer doInBackground(Void... voids) {
                WowEventWebServerIF web = WowEventWebServerIF.getInstance(EventDetailActivity.this);
                return web.fGetEventInfo(eventDetail.id);
            }

            @Override
            public void onPostExecute(Integer errno) {
                if (showProgressBar)
                    msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    WEvent e = new Database(EventDetailActivity.this).fetchEvent(eventDetail.id);
                    if (e != null) {
                        WEventUiHelper.fixMediaLocalPath(e);
                        eventDetail = e;
                        updateUI(needUpdateGallery);
                    }
                }
            }
        }.execute((Void)null);
    }

    private void updateUI(boolean needUpdateGallery) {
    	if(needUpdateGallery){
    		initGallery();
    	}
        displayEventAttributes();
    }

    private void initView() {

        AQuery q = new AQuery(this);
        q.find(R.id.left_button).clicked(this);
        q.find(R.id.right_button_up).clicked(this);
        q.find(R.id.right_button_down).clicked(this);
        
//        btn_right_up = (TextView) findViewById(R.id.right_button_up);
//        btn_right_down = (TextView) findViewById(R.id.right_button_down);
        
        updateUI(true);
    }

    private void displayEventAttributes() {
        AQuery q = new AQuery(this);
        WEventUiHelper helper = new WEventUiHelper(this);

        // title
        q.find(R.id.event_title).text(eventDetail.title);

        // time
        q.find(R.id.event_time_start).text(helper.formatField(
                getResources().getString(R.string.event_time_label_start),
                new SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(eventDetail.startTime)));

        // time
        q.find(R.id.event_time_end).text(helper.formatField(
                getResources().getString(R.string.event_time_label_end),
                new SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(eventDetail.endTime)));
        // place
        q.find(R.id.event_place).text(helper.formatField(
                getResources().getString(R.string.event_place_label),
                eventDetail.address));

        // member count
        q.find(R.id.event_count).text(Integer.toString(eventDetail.joinedMemberCount));

        // cost
        q.find(R.id.event_cost).text(helper.formatField(
                getResources().getString(R.string.event_cost_label),
                String.format(getResources().getString(R.string.event_cost_value), eventDetail.costGolds),
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.red)));

        // category
        q.find(R.id.event_category).text(helper.formatField(
                getString(R.string.event_category_label),
                WEventUiHelper.getEventCatetoryText(getBaseContext(), eventDetail.category)));

        // host
        q.find(R.id.event_host).text(helper.formatField(
                getString(R.string.event_host_label),
                eventDetail.host));

        // tel
        q.find(R.id.event_tel).text(helper.formatField(
                getString(R.string.event_tel_label),
                eventDetail.contactEmail));

        q.find(R.id.detail_introduce).text(eventDetail.description);
    }

}
