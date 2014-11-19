package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import co.onemeter.oneapp.R;
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
	private TextView txtEventTitle;
	private TextView txtTime;
	private TextView txtPlace;
	private TextView txtCategory;
	private TextView txtCost;
    private TextView tvMailSendTo;
	
	private ImageButton btnVoice;
	private TextView txtVoiceDesc;
	
	private TextView txtDetailIntroduce;
	
	private Button btnSignup;
	private Button btnBBS;
	
//	private MediaPlayer mPlayer;
	
//	private WEvent event;
	private WEvent eventDetail;

    private String mediaSoundPath;
//	private boolean _isPlayingAudio = false;
	
	private Timer timer = new Timer();
    private MessageBox mMsgBox;

    private MediaPlayerWraper mediaPlayerWraper;
	
	private void doStartPlayingAudio() {
        if(!mediaSoundPath.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(txtVoiceDesc,true);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    btnVoice.setImageResource(R.drawable.timeline_player_play);
                }

                @Override
                public void onPlayBegin(String path) {
                    btnVoice.setImageResource(R.drawable.timeline_player_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    btnVoice.setImageResource(R.drawable.timeline_player_play);
                }
            });
            mediaPlayerWraper.triggerPlayer(mediaSoundPath,0);
        } else {
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(mediaSoundPath,0);
        }

//		Log.i("YUANQU", "start player");
//		if (_isPlayingAudio ||  null == mediaSoundPath)
//			return;
//		try {
//			if (mPlayer == null) {
//				mPlayer = new MediaPlayer();
//				mPlayer.setOnCompletionListener(new OnCompletionListener() {
//
//					@Override
//					public void onCompletion(MediaPlayer mp) {
//						// TODO Auto-generated method stub
//						//doStopPlayingAudio();
//						timer.cancel();
//						mp.stop();
//						mp.release();
//						mp = null;
//                        btnVoice.setBackgroundResource(R.drawable.timeline_play);
//					}
//				});
//
//				try {
//					//mPlayer = MediaPlayer.create(this, R.raw.ringback);
////					final String path = eventDetail.multimedias.get(0).localPath; // XXX check ext
//                    final String path = mediaSoundPath;
//					Uri uri = Uri.parse(path);
//					mPlayer = new MediaPlayer().create(this, uri);
//					int duration = mPlayer.getDuration();
//					mPlayer.getCurrentPosition();
//					System.out.println("duration :" + duration);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				mPlayer.prepare();
//				//mPlayer.setLooping(true);
//			}
//            btnVoice.setBackgroundResource(R.drawable.timeline_stop);
//			mPlayer.start();
////			timer.schedule(new TimerTask() {
////
////				@Override
////				public void run() {
////					// TODO Auto-generated method stub
////					if (mPlayer.getCurrentPosition() % 1000 > 900 && mPlayer.getCurrentPosition() % 1000 < 1100) {
////						Message msg = Message.obtain();
////						msg.what = mPlayer.getCurrentPosition();
////						handler.sendMessage(msg);
////					}
//////					if (mPlayer.getCurrentPosition() > 3000) {
//////						this.cancel();
//////					}
////				}
////			}, 0, 200);
//
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		_isPlayingAudio = true;
		
	}
	
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
	
//	private void fFetchEventDetailByID(String actionId) {
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

        LinearLayout voiceLayout=(LinearLayout) findViewById(R.id.event_detail_voice_layout);
        if(!existSoundMedia) {
            voiceLayout.setVisibility(View.GONE);
//            btnVoice.setBackgroundColor(R.color.gray);
//            btnVoice.setImageResource(R.drawable.play_n);
//            txtVoiceDesc.setTextColor(R.color.gray);
        } else {
            voiceLayout.setVisibility(View.VISIBLE);
        }
    }

	private void initView() {
		initGallery();


		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtEventTitle = (TextView) findViewById(R.id.event_title);
		txtTime = (TextView) findViewById(R.id.event_time);
		txtPlace = (TextView) findViewById(R.id.event_place);
		txtCategory = (TextView) findViewById(R.id.event_category);
        txtCost = (TextView) findViewById(R.id.event_cost);
		
		btnVoice = (ImageButton) findViewById(R.id.event_voice_button);
        txtVoiceDesc = (TextView) findViewById(R.id.event_voice_text);
		
		txtDetailIntroduce = (TextView) findViewById(R.id.detail_introduce);
		
		btnSignup = (Button) findViewById(R.id.signup_button);
		btnBBS = (Button) findViewById(R.id.bbs_button);

        tvMailSendTo=(TextView) findViewById(R.id.mail_send_to);

		
		btnTitleBack.setOnClickListener(this);
		btnVoice.setOnClickListener(this);
		btnSignup.setOnClickListener(this);
		btnBBS.setOnClickListener(this);
        tvMailSendTo.setOnClickListener(this);

        txtEventTitle.setText(eventDetail.title);
        txtTime.setText(String.format(getResources().getString(R.string.event_time),
                new SimpleDateFormat("MM月dd日 HH:mm").format(eventDetail.startTime)
                        + "-"
                        + new SimpleDateFormat("HH:mm").format(eventDetail.endTime)));
        txtPlace.setText(String.format(getResources().getString(R.string.event_place), eventDetail.address));
        txtCategory.setText(String.format(getResources().getString(R.string.event_category),
                WEventUiHelper.getEventCatetoryText(this, eventDetail.category)));
        txtCategory.setVisibility(View.GONE);
        txtCost.setText(String.format(getResources().getString(R.string.event_cost), eventDetail.costGolds));
        txtDetailIntroduce.setText(eventDetail.description);
        tvMailSendTo.setText(eventDetail.contactEmail);

        if(WEvent.EVENT_TYPE_SIMPLE == eventDetail.event_type) {
            LinearLayout layoutDetail=(LinearLayout) findViewById(R.id.bao_ming_more_info_layout);
            layoutDetail.setVisibility(View.GONE);
        }

        if(WEvent.MEMBER_SHIP_JOINED == eventDetail.membership) {
            btnSignup.setEnabled(false);
            btnSignup.setText(R.string.event_signed_up);
        }

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
            case R.id.event_voice_button:
//                if (_isPlayingAudio) {
//                    doStopPlayingAudio();
//                } else {
//                    doStartPlayingAudio();
//                }
                doStartPlayingAudio();
                break;
            case R.id.signup_button:
//			Intent signIntent = new Intent(EventDetailActivity.this, EventApplyActivity.class);
//			signIntent.putExtra(INTENT_EXTRA_SIGNUP, eventDetail.id);
//			startActivity(signIntent);
                requireToJoinEvent();
                break;
            case R.id.bbs_button:
//			Intent bbsIntent = new Intent();
//			startActivity(bbsIntent);
                break;
            case R.id.mail_send_to:
                sendEmailByIntent(new String[]{tvMailSendTo.getText().toString()},
                        getString(R.string.require_to_join_event)+" "+eventDetail.title,
                        "");
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
            name=((EditText) findViewById(R.id.edt_name)).getText().toString();
            phoneNumber=((EditText) findViewById(R.id.edt_name)).getText().toString();

            if(TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)) {
                mMsgBox.toast(R.string.event_detail_send_detail_can_not_empty);
                return;
            }
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

                    btnSignup.setText(R.string.event_signed_up);
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
