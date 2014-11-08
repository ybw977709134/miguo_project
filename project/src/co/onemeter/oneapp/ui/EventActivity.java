package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventActivity extends Activity implements OnClickListener, MenuBar.OnDropdownMenuItemClickListener {
    private class EventAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private CopyOnWriteArrayList<WEvent> eventList;
		
		// hold file id, to avoid redundant downloading.
		private HashSet<String> mDownloadingLocks = new HashSet<String>();
		
		public EventAdapter(Context context, ArrayList<WEvent> list) {
			inflater = LayoutInflater.from(context);
			this.eventList = new CopyOnWriteArrayList<WEvent>(list);
		}
		@Override
		public int getCount() {
			return eventList.size();
		}

		@Override
		public Object getItem(int position) {
			return eventList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public void setDataSource(ArrayList<WEvent> list) {
            this.eventList = new CopyOnWriteArrayList<WEvent>(list);
            notifyDataSetChanged();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View lView = null;
			WEvent act = eventList.get(position);
//			lView = inflater.inflate(R.layout.listitem_event, null);
			if (convertView == null) {
				lView = inflater.inflate(R.layout.listitem_event, null);
			} else {
				lView = convertView;
			}
            AQuery q = new AQuery(lView);

			TextView txtTitle = (TextView) lView.findViewById(R.id.event_title);
			txtTitle.setText(act.title);

			TextView txtTime = (TextView) lView.findViewById(R.id.event_time);
			txtTime.setText(String.format(getResources().getString(R.string.event_time),
                    new SimpleDateFormat("MM月dd日 HH:mm").format(act.startTime)
                            + "-"
                            + new SimpleDateFormat("HH:mm").format(act.endTime)));
			TextView txtPlace = (TextView) lView.findViewById(R.id.event_place);
			txtPlace.setText(String.format(getResources().getString(R.string.event_place), act.address));
			TextView txtCount = (TextView) lView.findViewById(R.id.event_count);
			txtCount.setText(String.format(getResources().getString(R.string.event_member_count), act.capacity));
			TextView txtIntroduce = (TextView) lView.findViewById(R.id.event_introduce);
			txtIntroduce.setText(act.description);
            q.find(R.id.event_category).text(
                    String.format(getString(R.string.event_category),
                            WEventUiHelper.getEventCatetoryText(getBaseContext(), act.category)));
            q.find(R.id.event_host).text(
                    String.format(getString(R.string.event_host), act.host));
			ImageView imgPhoto = (ImageView) lView.findViewById(R.id.event_photo);
			// display first photo
			boolean hasPhoto = false;
			if(act.multimedias != null && !act.multimedias.isEmpty() && !TextUtils.isEmpty(act.thumbNail)) {
				for(WFile f : act.multimedias) {
                    if(!f.thumb_fileid.equals(act.thumbNail)) {
                        //only show thumbnail here
                        continue;
                    }
//					f.setExt("jpg"); // XXX
					if("jpg".equalsIgnoreCase(f.getExt())
							|| "jpeg".equalsIgnoreCase(f.getExt())
							|| "bmp".equalsIgnoreCase(f.getExt())
							|| "png".equalsIgnoreCase(f.getExt())) {
						hasPhoto = true;



						if(!Utils.isNullOrEmpty(f.localThumbnailPath) && new File(f.localThumbnailPath).exists()) {
							imgPhoto.setScaleType(ScaleType.CENTER_CROP);
							imgPhoto.setImageDrawable(new BitmapDrawable(
									EventActivity.this.getResources(),
									f.localThumbnailPath));
						} else if(!mDownloadingLocks.contains(f.thumb_fileid)) {
							imgPhoto.setScaleType(ScaleType.CENTER_CROP);
							imgPhoto.setImageDrawable(null);
							mDownloadingLocks.add(f.thumb_fileid);
							
//							f.localPath = PhotoDisplayHelper.makeLocalFilePath(f.fileid, f.getExt());
							new AsyncTask<WFile, Integer, Void> () {

								WFile f = null;
								boolean ok = false;
								
								@Override
								protected Void doInBackground(WFile... arg0) {
									
									f = arg0[0];
									
									WowTalkWebServerIF.getInstance(EventActivity.this)
									.fGetFileFromServer(
                                            f.thumb_fileid,
                                            WEvent.MEDIA_FILE_REMOTE_DIR,
											new NetworkIFDelegate(){

										@Override
										public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
											ok = false;
											Log.e("AvatarUtils.displayPhoto() failed to download: "
													+ new String(arg1).toString());
										}

										@Override
										public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
											ok = true;
											Log.i("AvatarUtils.displayPhoto() succeed");
										}

										@Override
										public void setProgress(int arg0, int arg1) {
										}

									}, 0, f.localThumbnailPath, null);
									return null;
								}
								
								@Override
								protected void onPostExecute(Void v) {
									mDownloadingLocks.remove(f.thumb_fileid);
									Log.i("download complete, fileid=" + f.thumb_fileid + ", result=" + ok);
									if(ok) {
										// save localPath into db
//										Database db = new Database(EventActivity.this);
//										db.open();
//										if(!db.updateEventMediaPath(f.fileid, f.localPath)) {
//											Log.e("failed to updateEventMediaPath: "
//													+ "fileid=" + f.fileid
//													+ ", localPath=" + f.localPath);
//										}

										// 调用 imgPhoto.setImageDrawable() 貌似不管用，可能是因为
										// 现在显示的已经不是原来的View了。
										refresh();
									}
								}

							}.execute(f);
						}
					}
				}
			}
			
//			View photoWrapper = lView.findViewById(R.id.photoWrapper);
			if(!hasPhoto) {
                imgPhoto.setVisibility(View.GONE);
			} else {
                imgPhoto.setVisibility(View.VISIBLE);
			}
 			return lView;
		}

        public void clear() {
            eventList.clear();
            notifyDataSetChanged();
        }

        public void addAll(List<WEvent> items) {
            eventList.addAll(items);
            notifyDataSetChanged();
        }
    }

    private class FilterBar extends MenuBar {

        public String[] hosts;

        /**
         * @param context
         * @param layoutResId
         * @param menuItemResIds
         * @param dialogBackground 作为对话框下方的屏幕背景，一般为半透明的黑色。
         */
        public FilterBar(Context context, int layoutResId, int[] menuItemResIds, View dialogBackground) {
            super(context, layoutResId, menuItemResIds, dialogBackground);
        }

        @Override
        protected String[] getSubItems(int itemId) {
            switch (itemId) {
                case R.id.btn_filter1: {
                    return new String[]{
                            getString(R.string.event_filter1_all),
                            getString(R.string.event_filter1_joind),
                            getString(R.string.event_filter1_my),
                    };
                }
                case R.id.btn_filter2: {
                    return new String[]{
                            getString(R.string.event_filter2_comingsoon),
                            getString(R.string.event_filter2_onging),
                            getString(R.string.event_filter2_expired),
                    };
                }
            }
            return new String[0];
        }
    }

    public static final String EVENT_DETAIL_BUNDLE = "event_detail_id";

	private ImageButton ibRight;
	private ListView lvEvent;
	private TextView txtTitle;
    private View newEventPanel;

	private ArrayList<WEvent> acts;
	private EventAdapter eventAdapter;

//	private static EventActivity instance;
	
	private Database mDb = null;

    private FilterBar tf;


//	public static EventActivity instance() {
//		if (instance == null)
//				return null;
//		return instance;
//	}

	protected void refresh() {
		this.runOnUiThread(new Runnable(){
			@Override
			public void run() {
                fSetShownEvents();
			}
		});
	}

//	public static boolean isInstancated() {
//		return instance != null;
//	}
	
	private void initView() {
        ibRight = (ImageButton) findViewById(R.id.right_button);
		txtTitle = (TextView) findViewById(R.id.title_text);
		lvEvent = (ListView) findViewById(R.id.event_list);
        newEventPanel = findViewById(R.id.new_event_panel);

        ibRight.setOnClickListener(this);
		txtTitle.setOnClickListener(this);

		lvEvent.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                                    long id) {
                fGointoASpecificEvent((WEvent) lvEvent.getItemAtPosition(position));
            }
        });

        View c = findViewById(R.id.dialog_container);
        c.setVisibility(View.INVISIBLE);
        tf = new FilterBar(this, R.layout.event_filter,
                new int[]{ R.id.btn_filter1, R.id.btn_filter2 },
                c);
        tf.setOnFilterChangedListener(this);
        lvEvent.addHeaderView(tf.getView());

        fSetShownEvents();
        hideNewEventPanel();

        AQuery q = new AQuery(this);
        q.find(R.id.title_left).clicked(this);
        q.find(R.id.vg_new_qa).clicked(this);
        q.find(R.id.vg_new_vote).clicked(this);
        q.find(R.id.vg_new_offline).clicked(this);
        q.find(R.id.new_event_panel).clicked(this);

        // TODO control create moment button visibility according to user type.
	}
	
	private void downloadLatestEvents() {
        lastRefreshEventTime=System.currentTimeMillis();

		new Thread(new Runnable() {

			@Override
			public void run() {
				int errno = WowEventWebServerIF.getInstance(EventActivity.this).fGetLatestEvents();
				if (errno == ErrorCode.OK) {
                    refresh();
				}
			}
			
		}).start();
	}

    private void downloadPreviousEvents(final String timestamp) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                int errno = WowEventWebServerIF.getInstance(EventActivity.this).fGetPreviousEvents(timestamp);
                if (errno == ErrorCode.OK) {
                    refresh();
                }
            }

        }).start();
    }

    private void fixEventLocalPath() {
        for(WEvent aEvent : acts) {
            if(null != aEvent.multimedias) {
                for(WFile f : aEvent.multimedias) {
                    f.localThumbnailPath=PhotoDisplayHelper.makeLocalFilePath(f.thumb_fileid, f.getExt());
                    f.localPath=PhotoDisplayHelper.makeLocalFilePath(f.fileid, f.getExt());
                }
            }
        }
    }
	
	private void fSetShownEvents() {
		switch (curEventGroupToShow) {
            case 0:
                acts = mDb.fetchAllEvents();
                break;
            case 1:
                acts = mDb.fetchJoinedEvents();
                break;
            case 2:
                acts = mDb.fetchNotJoinedEvents();
                break;
//		case 1:
//			acts = mDb.fetchOfficialEvents();
//			break;
//		case 2:
//			acts = mDb.fetchAppliedEvents();
//			break;
//		case 3:
//			acts = mDb.fetchJoinedEvents();
//			break;
//		case 4:
            default:
                acts = mDb.fetchAllEvents();
                break;
        }

        fixEventLocalPath();

        eventAdapter = new EventAdapter(EventActivity.this, acts);
        lvEvent.setAdapter(eventAdapter);
		eventAdapter.notifyDataSetChanged();
	}
	
	private void fGointoASpecificEvent(WEvent wa) {
		if (wa == null)
			return;
		Intent intent = new Intent(this, EventDetailActivity.class);
		Bundle b = new Bundle();
		b.putParcelable(EVENT_DETAIL_BUNDLE, wa);
		intent.putExtras(b);
		startActivity(intent);
	}

    private int curEventGroupToShow=0;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.right_button:
                final int plan = 2;
                if (plan == 1)
                    toogleNewEventPanel();
                else
                    gotoCreateEvent("offline");
                break;
            case R.id.title_left:
                onBackPressed();
                break;
            case R.id.title_text:
                break;
            case R.id.vg_new_qa:
                gotoCreateEvent("qa");
                hideNewEventPanel();
                break;
            case R.id.vg_new_vote:
                gotoCreateEvent("vote");
                hideNewEventPanel();
                break;
            case R.id.vg_new_offline:
                gotoCreateEvent("offline");
                hideNewEventPanel();
                break;
            case R.id.new_event_panel:
                hideNewEventPanel();
                break;
            default:
                break;
        }
    }

    private void gotoCreateEvent(String cat) {
        startActivity(new Intent(EventActivity.this, CreateEventActivity.class)
                .putExtra(CreateEventActivity.EXTRA_EVENT_CATEGORY, cat)
                .putExtra(CreateEventActivity.EXTRA_PAGE_TITLE,
                        WEventUiHelper.getEventCatetoryText(this, cat)));
    }

    private void toogleNewEventPanel() {
        if (isNewEventPanelVisible()) {
            hideNewEventPanel();
        } else {
            showNewEventPanel();
        }
    }

    private void showNewEventPanel() {
        if (tf != null && tf.isShowingDialog()) {
            tf.tryDismissAll();
        }
        newEventPanel.setVisibility(View.VISIBLE);
    }

    private void hideNewEventPanel() {
        newEventPanel.setVisibility(View.GONE);
    }

    private boolean isNewEventPanelVisible() {
        return newEventPanel.getVisibility() == View.VISIBLE;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_all);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        if(mDb == null) {
            mDb = new Database(this);
        }

//		instance = this;
		initView();
	}

    private long lastRefreshEventTime;
    private final static long REFRESH_EVENT_INTERVAL=30*1000;
	@Override
	protected void onResume() {
		super.onResume();
        MobclickAgent.onResume(this);

        if(System.currentTimeMillis()-lastRefreshEventTime >= REFRESH_EVENT_INTERVAL) {
            downloadLatestEvents();
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        MobclickAgent.onPause(this);
	}

    @Override
    public void onBackPressed() {
        if (tf != null && tf.isShowingDialog()) {
            tf.tryDismissAll();
            return;
        }
        if (isNewEventPanelVisible()) {
            hideNewEventPanel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onDropdownMenuShow(int subMenuResId) {
        if (isNewEventPanelVisible())
            hideNewEventPanel();
    }

    @Override
    public void onDropdownMenuItemClick(int subMenuResId, int itemIdx) {
        if (subMenuResId == R.id.btn_filter1) {
            if (itemIdx == 0) {
                eventAdapter.setDataSource(acts);
            } else if (itemIdx == 1) { // joined
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                for (WEvent e : acts) {
                    if (e.membership == WEvent.MEMBER_SHIP_JOINED) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            } else if (itemIdx == 2) { // my
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                String myUid = PrefUtil.getInstance(this).getUid();
                for (WEvent e : acts) {
                    if (TextUtils.equals(myUid, e.owner_uid)) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            }
            return;
        } else if (subMenuResId == R.id.btn_filter2) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (itemIdx == 0) {
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                for (WEvent e : acts) {
                    if (e.startTime.getTime() > now) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            } else if (itemIdx == 1) { // on going
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                for (WEvent e : acts) {
                    if (e.startTime.getTime() < now && e.endTime.getTime() > now) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            } else if (itemIdx == 2) { // expired
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                for (WEvent e : acts) {
                    if (e.endTime.getTime() < now) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            }
            return;
        }

        Toast.makeText(this, subMenuResId + "/" + itemIdx, Toast.LENGTH_SHORT).show();
    }

}
