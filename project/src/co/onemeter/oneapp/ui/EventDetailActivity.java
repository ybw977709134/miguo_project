package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.ApplicationInfoItemAdapter;
import co.onemeter.oneapp.ui.widget.DampScollView;
import co.onemeter.oneapp.utils.ListViewUtils;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class EventDetailActivity extends Activity implements OnClickListener {
    private static final int NUM_COLUMNS = 4;

    public static final String INTENT_EXTRA_SIGNUP = "intent_extra_signup";
	
	private WEvent eventDetail;
    private MessageBox msgbox;
    private TextView btn_right_up;
    private TextView btn_right_down;
    private ListView listView_applicantsInfo;
    private ArrayList<Buddy> BuddyList=new ArrayList<Buddy>();
    private ApplicationInfoItemAdapter applicationInfoItemAdapter = null;

    private void downloadImage(final WFile aFile, final boolean thumbnail, final ImageView imageView) {

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {
            boolean ok = true;

            @Override
            protected Void doInBackground(Void... arg0) {
                WowTalkWebServerIF.getInstance(EventDetailActivity.this)
                        .fGetFileFromServer(thumbnail ? aFile.thumb_fileid : aFile.fileid,
                                WEvent.MEDIA_FILE_REMOTE_DIR, new NetworkIFDelegate() {
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
                if (ok) {
                    displayImage(thumbnail ? aFile.localThumbnailPath : aFile.localPath, imageView, false);
                }
            }

        });
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
                    if (!displayImage(aFile.localPath, imageView, true)) {
                        downloadImage(aFile, false, imageView);
                    }
                }
                // else
                {
                    eventImageList.add(aFile.localThumbnailPath);

                    // create image view
                    ImageView imageView = addImageView(row, numImages);

                    // set image source
                    if (!displayImage(filename, imageView, false)) {
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

    private boolean displayImage(String filename, ImageView imageView, boolean asAlbumCover) {
        boolean ok = false;
        try {
            if (new File(filename).exists()) {
                int size = asAlbumCover ? 300 : 100;
                new ImageResizer(this, size).loadImage(filename, imageView);
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
            	//Log.i("--membercount", eventDetail.joinedMemberCount + "");
            	//Log.i("--capacity", eventDetail.capacity + "");
            	if(eventDetail.is_get_member_info){

                    if (System.currentTimeMillis() > eventDetail.endTime.getTime()) {//报名时期已过时

                        MessageDialog dialog = new MessageDialog(EventDetailActivity.this,false,MessageDialog.SIZE_NORMAL);
                        dialog.setTitle("");
                        dialog.setMessage("该活动已结束，请期待下次活动！");
                        dialog.setCancelable(false);
                        dialog.show();

                    } else {
                        Intent intent = new Intent(this, SubmitInformationActivity.class);
                        startActivityForResult(intent, 100);
                    }

            	}else{
            		joinEvent();
            		btn_right_up.setVisibility(View.GONE);
                    btn_right_down.setVisibility(View.VISIBLE);
            	}
                
                break;

            case R.id.right_button_down:
                MessageDialog dialog = new MessageDialog(this);
                dialog.setTitle(getString(R.string.contacts_QRcode_dialogtitle));
                dialog.setMessage(getString(R.string.event_cancel_join_msg));
                dialog.setRightBold(true);
                dialog.setCancelable(false);//不能点击空白处取消
                dialog.setOnLeftClickListener(getString(R.string.cancel),null);
                dialog.setOnRightClickListener(getString(R.string.ok), new MessageDialog.MessageDialogClickListener() {
                    @Override
                    public void onclick(MessageDialog dialog) {
                        dialog.dismiss();
                        cancel_join_event();//取消报名
                        btn_right_down.setVisibility(View.GONE);
                        btn_right_up.setVisibility(View.VISIBLE);
                        Toast.makeText(EventDetailActivity.this, R.string.require_cancel_event_joined, Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
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
//        detail_applicants = (TextView) findViewById(R.id.detail_applicants);
        listView_applicantsInfo = (ListView) findViewById(R.id.listView_applicantsInfo);

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
		applicationInfoItemAdapter = new ApplicationInfoItemAdapter(this, BuddyList);
		if(isApplicantsVisible()){
            showApplicants(false);
			loadApplicantsInfo();
		}else{
            hideApplicants();
		}
		
		DampScollView dampScrollView = (DampScollView) findViewById(R.id.sv_main);
		ImageView imgView = (ImageView) findViewById(R.id.image_cover);
		dampScrollView.setImageView(imgView);
	}

    private boolean isApplicantsVisible() {
        return eventDetail.owner_uid.equals(PrefUtil.getInstance(this).getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode == RESULT_OK){
    		String name = data.getStringExtra(SubmitInformationActivity.SUBMITNAME);
    		String phone = data.getStringExtra(SubmitInformationActivity.SUBMITPHONE);
    		joinEventWithDetail(name,phone);//参加报名，填写信息。
    	}
    }
    
    private void loadApplicantsInfo(){
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            int errno = ErrorCode.OK;
            private ArrayList<Buddy> members = new ArrayList<>();

            @Override
            protected Integer doInBackground(Void... arg0) {
                EventWebServerIF web = EventWebServerIF.getInstance(EventDetailActivity.this);
                errno = web.fGetApplicantsInfo(members, eventDetail.id);
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    BuddyList.clear();
                    BuddyList.addAll(members);
                    listView_applicantsInfo.setAdapter(applicationInfoItemAdapter);
                    ListViewUtils.setListViewHeightBasedOnChildren(listView_applicantsInfo);
                }
                showApplicants(BuddyList.isEmpty());
            }

        });
    }

    private void hideApplicants() {
        findViewById(R.id.event_table_apply).setVisibility(View.GONE);
        findViewById(R.id.txt_no_applicants).setVisibility(View.GONE);
    }

    private void showApplicants(boolean isEmpty) {
        findViewById(R.id.event_table_apply).setVisibility(View.VISIBLE);
        findViewById(R.id.txt_no_applicants).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        findViewById(R.id.listView_applicantsInfo).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void joinEvent() {
        msgbox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {
                EventWebServerIF web = EventWebServerIF.getInstance(EventDetailActivity.this);
                return web.fJoinEvent(eventDetail.id);

            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    msgbox.toast(R.string.require_join_event_success);
                    btn_right_up.setVisibility(View.GONE);
                    btn_right_down.setVisibility(View.VISIBLE);
                    refresh(false, false);
                } else {
                    msgbox.toast(R.string.require_join_event_joined);
                }
            }
        });
    }
    
    private void joinEventWithDetail(final String name,final String phone_number){
    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {
                EventWebServerIF web = EventWebServerIF.getInstance(EventDetailActivity.this);
                return web.fJoinEventWithDetail(eventDetail.id, name, phone_number);
            }

            @Override
            public void onPostExecute(Integer errno) {
                //           msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    msgbox.toast(R.string.require_join_event_success);
                    btn_right_up.setVisibility(View.GONE);
                    btn_right_down.setVisibility(View.VISIBLE);
                    refresh(false, false);
                } else {
                    msgbox.toast(R.string.require_join_event_joined);
                }
            }
        });
    }
    
    /**
     * 取消报名
     */
    private void cancel_join_event(){
 //  	 msgbox.showWait();
    	 AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

             @Override
             protected Integer doInBackground(Void... voids) {
                 EventWebServerIF web = EventWebServerIF.getInstance(EventDetailActivity.this);
                 return web.fDeleteEvent(eventDetail.id);
             }

             @Override
             public void onPostExecute(Integer errno) {
                 //           msgbox.dismissWait();
                 if (errno == ErrorCode.OK) {
                     //                msgbox.toast(R.string.require_cancel_event_joined);
                     refresh(false, false);
                 } else {
                     //               msgbox.toast(R.string.require_cancel_event_fail);
                 }
             }
         });
    	
    }

    private void refresh(final boolean showProgressBar,final boolean needUpdateGallery) {
        if (showProgressBar)
            msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {
                EventWebServerIF web = EventWebServerIF.getInstance(EventDetailActivity.this);
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

                // refresh applicants
                if (isApplicantsVisible())
                    loadApplicantsInfo();
            }
        });
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
        
        if(eventDetail.capacity > 0 && eventDetail.joinedMemberCount >= eventDetail.capacity){
        	btn_right_up.setText(getString(R.string.event_quota_full));
        	btn_right_up.setTextSize(12);
        	btn_right_up.setEnabled(false);
    	}
        
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
                String.format(getResources().getString(R.string.event_cost_yuan), eventDetail.costGolds),
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
        String tele = eventDetail.telephone;
        if(!tele.equals("0")){
        	q.find(R.id.event_tel).text(helper.formatField(
                getString(R.string.event_tel_label),
                tele));
        }else{
        	q.find(R.id.event_tel).text(helper.formatField(
                    getString(R.string.event_tel_label),
                    ""));
        }
        

        q.find(R.id.detail_introduce).text(eventDetail.description);
    }

}
