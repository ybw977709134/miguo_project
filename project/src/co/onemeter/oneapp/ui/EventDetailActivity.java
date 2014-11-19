package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.ImageViewActivity;
import org.wowtalk.ui.MessageBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;

public class EventDetailActivity extends Activity implements OnClickListener {
	private class GalleryAdapter extends BaseAdapter {
		private Context context;

//        private ArrayList<String> imagePathListFromAsset=new ArrayList<String>();
        private ArrayList<String> imagePathListFromEvent=null;

//		public GalleryAdapter(Context context) {
//			this.context = context;
//
//            for(int i=0; i<4; ++i) {
//                imagePathListFromAsset.add("pseudo_photo_landscape" + (i+1) + ".jpg");
//            }
//		}

        public GalleryAdapter(Context context,ArrayList<String> imageList) {
            this.context = context;

            imagePathListFromEvent=imageList;
        }

		@Override
		public int getCount() {
            return Integer.MAX_VALUE;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
//			AssetManager am = getAssets();
			InputStream in = null;
            Drawable drawable;

            ImageView imageView;
            if(null == convertView) {
                convertView=LayoutInflater.from(context).inflate(R.layout.event_detail_gallery_item_layout, null);
            }
            imageView=(ImageView)convertView.findViewById(R.id.content_image);

			try {
//                if(null == imagePathListFromEvent) {
//                    in = am.open("pseudo_photo_landscape" + (position % 4 + 1) + ".jpg");
//                    drawable = Drawable.createFromStream(in, null);
//                    imageView.setImageDrawable(drawable);
//                } else {
                    if(new File(imagePathListFromEvent.get(position%imagePathListFromEvent.size())).exists()) {
                        in = new FileInputStream(imagePathListFromEvent.get(position%imagePathListFromEvent.size()));
                        drawable = Drawable.createFromStream(in, null);
                        imageView.setImageDrawable(drawable);
                    } else {
                        imageView.setImageResource(R.drawable.feed_default_pic);
                    }
//                }
			} catch (IOException e) {
				e.printStackTrace();
			}

			return convertView;
		}
		
	}
	public static final String INTENT_EXTRA_SIGNUP = "intent_extra_signup";
	
	private ImageButton btnTitleBack;
	
	private Gallery gallery;
	private TextView txtDetailIntroduce;

//	private MediaPlayer mPlayer;
	
//	private WEvent event;
	private WEvent eventDetail;

    private String mediaSoundPath;
//	private boolean _isPlayingAudio = false;
	
	private Timer timer = new Timer();
    private MessageBox mMsgBox;

    private MediaPlayerWraper mediaPlayerWraper;

    private void doStopPlayingAudio() {
        mediaPlayerWraper.stop();
//		if (!_isPlayingAudio || null == mediaSoundPath)
//			return;
//		try {
//            btnVoice.setBackgroundResource(R.drawable.timeline_play);
//
//			if (mPlayer != null) {
//				mPlayer.stop();
//				mPlayer.release();
//				mPlayer = null;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		_isPlayingAudio = false;
	}
	
//	private void fFetchEventDetailByID(String eventDetailionId) {
//
//	}

    private void fixMediaSoundStatus() {
        boolean existSoundMedia=false;
        for(int i=0; i<eventDetail.multimedias.size(); ++i) {
            if(eventDetail.multimedias.get(i).localPath.matches("\\*\\.(mp4|m4a|aac|3gpp|avi|wav)$")) {
                mediaSoundPath=eventDetail.multimedias.get(i).localPath;
                existSoundMedia=true;
                break;
            }
        }
    }

	private void initView() {
		initGallery();

		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtDetailIntroduce = (TextView) findViewById(R.id.detail_introduce);

		btnTitleBack.setOnClickListener(this);

        AQuery q = new AQuery(this);
        WEventUiHelper helper = new WEventUiHelper(this);

        // title
        q.find(R.id.event_title).text(eventDetail.title);

        // time
        q.find(R.id.event_time).text(helper.formatField(
                getResources().getString(R.string.event_time_label),
                new SimpleDateFormat("MM月dd日 HH:mm").format(eventDetail.startTime)
                        + "-"
                        + new SimpleDateFormat("HH:mm").format(eventDetail.endTime)));

        // place
        q.find(R.id.event_place).text(helper.formatField(
                getResources().getString(R.string.event_place_label),
                eventDetail.address));

        // member count
        q.find(R.id.event_count).text(helper.formatField(
                getResources().getString(R.string.event_member_count_label),
                String.format(getResources().getString(R.string.event_member_count_value), eventDetail.joinedMemberCount),
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.text_red)));

        // cost
        q.find(R.id.event_cost).text(helper.formatField(
                getResources().getString(R.string.event_cost_label),
                String.format(getResources().getString(R.string.event_cost_value), eventDetail.costGolds),
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.text_red)));

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

        txtDetailIntroduce.setText(eventDetail.description);

        fixMediaSoundStatus();
	}

    private void tryLoadEventThumbImageFromServer(final WFile aFile) {
        if(!new File(aFile.localThumbnailPath).exists()) {

            new AsyncTask<Void, Integer, Void> () {
                boolean ok = true;

                @Override
                protected Void doInBackground(Void... arg0) {
                    WowTalkWebServerIF.getInstance(EventDetailActivity.this)
                            .fGetFileFromServer(aFile.thumb_fileid, WEvent.MEDIA_FILE_REMOTE_DIR,new NetworkIFDelegate(){
                                @Override
                                public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                                    ok = false;
                                    org.wowtalk.Log.e("AvatarUtils.displayPhoto() failed to download: "
                                            + new String(arg1).toString());
                                }
                                @Override
                                public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                                    ok = true;
                                    org.wowtalk.Log.i("AvatarUtils.displayPhoto() succeed");
                                }
                                @Override
                                public void setProgress(int arg0, int arg1) {
                                }

                            }, 0, aFile.localThumbnailPath,null);
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    org.wowtalk.Log.i("download complete, fileid=" + aFile.fileid + ", result=" + ok);
                    if(ok) {
                        galleryAdapter.notifyDataSetChanged();
                    }
                }

            }.execute((Void)null);
        }
    }

    private boolean isFileAPhoto(String fileName) {
        return fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".tif") ||
                fileName.endsWith(".png");
    }

    private GalleryAdapter galleryAdapter;
	private void initGallery() {
		if (gallery == null) {
			gallery = (Gallery) findViewById(R.id.gallery);
		}

        ArrayList<String> eventImageList=new ArrayList<String>();
        if(null != eventDetail.multimedias) {
            for(WFile aFile : eventDetail.multimedias) {
                if(isFileAPhoto(aFile.localPath)) {
                    tryLoadEventThumbImageFromServer(aFile);

                    eventImageList.add(aFile.localThumbnailPath);
                }
            }
        }

        if(eventImageList.size() > 0) {
            galleryAdapter=new GalleryAdapter(this,eventImageList);
            gallery.setAdapter(galleryAdapter);
        } else {
            gallery.setVisibility(View.GONE);
            return;
//            galleryAdapter=new GalleryAdapter(this);
//            gallery.setAdapter(galleryAdapter);
        }

		gallery.setSelection(Integer.MAX_VALUE / 2, false);
		gallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
                ImageViewActivity.launch(EventDetailActivity.this,
                        position%eventDetail.multimedias.size(),
                        eventDetail.multimedias,
                        ImageViewActivity.UPDATE_WITH_CHAT_MESSAGE);
			}

		});
	}

	@Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    private void sendEmailByIntent(String[] destMails,String subject,String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, destMails);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(""));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.title_send_mail)));
    }

    private void requireToJoinEvent() {
        final String name;
        final String phoneNumber;
        if(WEvent.EVENT_TYPE_WITH_DETAIL == eventDetail.event_type) {
            name="";
            phoneNumber="";
//            name=((EditText) findViewById(R.id.edt_name)).getText().toString();
//            phoneNumber=((EditText) findViewById(R.id.edt_name)).getText().toString();
//
//            if(TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)) {
//                mMsgBox.toast(R.string.event_detail_send_detail_can_not_empty);
//                return;
//            }
        } else {
            name="";
            phoneNumber="";
        }

        mMsgBox.showWait();

        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int errno = ErrorCode.UNKNOWN;

                if(WEvent.EVENT_TYPE_SIMPLE == eventDetail.event_type) {
                    errno=WowEventWebServerIF.getInstance(EventDetailActivity.this).fJoinEvent(eventDetail.id);
                } else if (WEvent.EVENT_TYPE_WITH_DETAIL == eventDetail.event_type) {
                    errno=WowEventWebServerIF.getInstance(EventDetailActivity.this).fJoinEventWithDetail(eventDetail.id,
                            name,
                            phoneNumber,
                            "");
                }
                return errno;
            }
            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                if(ErrorCode.OK == result) {
                    mMsgBox.toast(R.string.require_join_event_success);
                } else {
                    mMsgBox.toast(R.string.require_join_event_fail);
                }
            }
        }.execute((Void)null);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_detail);

        mMsgBox = new MessageBox(this);
        mediaPlayerWraper=new MediaPlayerWraper(this);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		if (getIntent() == null)
			return;
        eventDetail = getIntent().getExtras().getParcelable(EventActivity.EVENT_DETAIL_BUNDLE);
		
		initView();
		if (eventDetail == null)
			return;

		
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

    @Override
    protected void onStop() {
        super.onStop();
        doStopPlayingAudio();
    }

}
