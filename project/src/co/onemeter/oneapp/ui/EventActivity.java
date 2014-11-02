package co.onemeter.oneapp.ui;

import android.app.ActionBar.LayoutParams;
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
								boolean ok = true;
								
								@Override
								protected Void doInBackground(WFile... arg0) {
									
									f = arg0[0];
									
									WowTalkWebServerIF.getInstance(EventActivity.this)
									.fGetFileFromServer(f.thumb_fileid,
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

									}, 0, f.localThumbnailPath);
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
                case R.id.btn_host: {
                    String[] a = getDistinctHosts();
                    hosts = new String[1 + a.length];
                    hosts[0] = getString(R.string.event_filter_host_all);
                    for (int i = 0; i < a.length; ++i) {
                        hosts[1 + i] = a[i];
                    }
                    return hosts;
                }
                case R.id.btn_type: {
                    return getResources().getStringArray(R.array.event_category_text);
                }
                case R.id.btn_time:
                    return getResources().getStringArray(R.array.event_time);
            }
            return new String[0];
        }
    }
	
	private class EventCategoryAdapter extends BaseAdapter {

		public EventCategoryAdapter() {
			
		}

		@Override
		public int getCount() {
			return eventCategories.length;
		}

		@Override
		public Object getItem(int position) {
			return eventCategories[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View lView = null;
			if (convertView == null) {
				lView = LayoutInflater.from(EventActivity.this).inflate(R.layout.listitem_popup, null);
			} else {
				lView = convertView;
			}
			TextView eventCategory = (TextView) lView.findViewById(R.id.group_name);
			eventCategory.setText(getResources().getString(eventCategories[position]));
			if (txtTitle.getText().equals(getResources().getString(eventCategories[position]))) {
				eventCategory.setTextColor(getResources().getColor(R.color.blue));
			} else {
				eventCategory.setTextColor(getResources().getColor(R.color.gray));
			}
			return lView;
		}
		
	}
	public static final String EVENT_DETAIL_BUNDLE = "event_detail_id";
	
	private static final int[] eventCategories = {
		R.string.events_all,
//		R.string.events_official,
//		R.string.events_applied,
		R.string.events_joined,
        R.string.events_not_joined,
//		R.string.events_my
	};

	private RelativeLayout mNavBar;
	private ImageButton ibRight;
	private ListView lvEvent;
	private TextView txtTitle;

	private ArrayList<WEvent> acts;
	private EventCategoryAdapter categroyAdapter;
	private EventAdapter eventAdapter;
	private PopupWindow categoryPopup;
	
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
		mNavBar = (RelativeLayout) findViewById(R.id.title_bar);
        ibRight = (ImageButton) findViewById(R.id.right_button);
		txtTitle = (TextView) findViewById(R.id.title_text);
		lvEvent = (ListView) findViewById(R.id.event_list);

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
                new int[]{ R.id.btn_host, R.id.btn_type, R.id.btn_time },
                c);
        tf.setOnFilterChangedListener(this);
        lvEvent.addHeaderView(tf.getView());

        fSetShownEvents();

        AQuery q = new AQuery(this);
        q.find(R.id.title_left).clicked(this);
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
	private void fShowPopup() {
		View lView = LayoutInflater.from(this).inflate(R.layout.popup_list, null);

        ListView lvEventCategory=(ListView) lView.findViewById(R.id.list_group);
        lvEventCategory.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                txtTitle.setText(getResources().getString(eventCategories[position]));
                categoryPopup.dismiss();
                curEventGroupToShow=position;
                fSetShownEvents();
            }
        });


		if (categroyAdapter == null) {
			categroyAdapter = new EventCategoryAdapter();
			lvEventCategory.setAdapter(categroyAdapter);
		}
        categroyAdapter.notifyDataSetChanged();

		if (categoryPopup == null) {
//			categoryPopup = new PopupWindow(lView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            categoryPopup = Utils.getFixedPopupWindow(lView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		categoryPopup.setFocusable(true);
		categoryPopup.setOutsideTouchable(true);
		categoryPopup.setBackgroundDrawable(new BitmapDrawable());
		categoryPopup.setWidth(DensityUtil.dip2px(EventActivity.this, 250));
		categoryPopup.showAsDropDown(mNavBar, (getWindowManager().getDefaultDisplay().getWidth() - categoryPopup.getWidth()) / 2, 0);
		categoryPopup.update();
	}


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.right_button:
                Intent detailIntent = new Intent(EventActivity.this, CreateEventActivity.class);
                startActivity(detailIntent);
                break;
            case R.id.title_left:
                onBackPressed();
                break;
            case R.id.title_text:
                fShowPopup();
                break;
            default:
                break;
        }
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
        super.onBackPressed();
    }

    @Override
    public void onDropdownMenuItemClick(int subMenuResId, int itemIdx) {
        if (subMenuResId == R.id.btn_host) {
            if (itemIdx == 0) {
                eventAdapter.setDataSource(acts);
            } else {
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                String selectedHost = tf.hosts != null && itemIdx >= 0 && itemIdx < tf.hosts.length ?
                        tf.hosts[itemIdx] : null;
                for (WEvent e : acts) {
                    if (TextUtils.equals(selectedHost, e.host)) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            }
            return;
        } else if (subMenuResId == R.id.btn_type) {
            if (itemIdx == 0) {
                eventAdapter.setDataSource(acts);
            } else {
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                String selectedCat = WEventUiHelper.getCategoryNameByIndex(this, itemIdx);
                for (WEvent e : acts) {
                    if (TextUtils.equals(selectedCat, e.category)) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            }
            return;
        }

        Toast.makeText(this, subMenuResId + "/" + itemIdx, Toast.LENGTH_SHORT).show();
    }

    private String[] getDistinctHosts() {
        HashSet<String> hosts = new HashSet<String>();
        for (WEvent e : acts) {
            hosts.add(e.host);
        }
        return hosts.toArray(new String[hosts.size()]);
    }
}
