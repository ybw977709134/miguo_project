package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.Log;
import org.wowtalk.api.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventActivity extends Activity implements OnClickListener, MenuBar.OnDropdownMenuItemClickListener, OnRefreshListener<ListView>, OnLastItemVisibleListener {
    private class EventAdapter extends BaseAdapter {

        private final static int VIEWTYPE_NORMAL = 0;
        private final static int VIEWTYPE_LAST = 1;

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
		public WEvent getItem(int position) {
			return eventList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

        public void replaceItem(int position, WEvent newValue) {
            eventList.remove(position);
            eventList.add(position, newValue);
            notifyDataSetChanged();
        }

		public void setDataSource(ArrayList<WEvent> list) {
            this.eventList = new CopyOnWriteArrayList<WEvent>(list);
            notifyDataSetChanged();
		}

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position < getCount() - 1 ? VIEWTYPE_NORMAL : VIEWTYPE_LAST;
        }
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View lView;
			WEvent act = eventList.get(position);
			if (convertView == null) {
                lView = inflater.inflate(
                        getItemViewType(position) == VIEWTYPE_NORMAL
                                ? R.layout.listitem_event
                                : R.layout.listitem_event_last
                        , null);
			} else {
				lView = convertView;
			}
            AQuery q = new AQuery(lView);

            // title
            q.find(R.id.event_title).text(act.title);

            // time
            q.find(R.id.event_time_start).text(formatField(
                            getResources().getString(R.string.event_time_label_start),
                            new SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(act.startTime)));
            q.find(R.id.event_time_end).text(formatField(
                    getResources().getString(R.string.event_time_label_end),
                    new SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(act.endTime)));

            // place
            q.find(R.id.event_place).text(formatField(
                    getResources().getString(R.string.event_place_label),
                    act.address));

            // member count
            q.find(R.id.event_count).text(formatField(
                    getResources().getString(R.string.event_member_count_label),
                    String.format(getResources().getString(R.string.event_member_count_value), act.joinedMemberCount),
                    getResources().getColor(R.color.text_gray3),
                    getResources().getColor(R.color.red)));

            // category
            q.find(R.id.event_category).text(formatField(
                    getString(R.string.event_category_label),
                    WEventUiHelper.getEventCatetoryText(getBaseContext(), act.category)));

            // host
            q.find(R.id.event_host).text(formatField(
                    getString(R.string.event_host_label),
                    act.host));

			ImageView imgPhoto = (ImageView) lView.findViewById(R.id.event_photo);
			// display first photo
			boolean hasPhoto = false;
			if(act.multimedias != null && !act.multimedias.isEmpty()) {
                // display first image as event cover
				for(WFile f : act.multimedias) {
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
							AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<WFile, Integer, Void>() {

                                WFile f = null;
                                boolean ok = false;

                                @Override
                                protected Void doInBackground(WFile... arg0) {

                                    f = arg0[0];

                                    WowTalkWebServerIF.getInstance(EventActivity.this)
                                            .fGetFileFromServer(
                                                    f.thumb_fileid,
                                                    WEvent.MEDIA_FILE_REMOTE_DIR,
                                                    new NetworkIFDelegate() {

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
                                    if (ok) {
                                        // 调用 imgPhoto.setImageDrawable() 貌似不管用，可能是因为
                                        // 现在显示的已经不是原来的View了。
                                        refresh();
                                    }
                                }

                            }, f);
						}
                        break;
					}
				}
			}
			if(!hasPhoto){
				imgPhoto.setImageResource(R.drawable.events_default_pic);
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
            setBackgroundColor(getResources().getColor(R.color.background_light));
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
    private final String GET_COMMING_EVENT_0 = "0";//只返回尚未开始的活动（升序）
    private final String GET_FINISHED_EVENT_1 = "1";//只返回已过期的活动（降序）
    private final String GET_ING_EVENT_2 = "2";//只返回进行中的活动（升序）

	private ImageButton ibRight;
	private ListView lvEvent;
	private TextView txtTitle;
    private View newEventPanel;

	private ArrayList<WEvent> acts;
	private EventAdapter eventAdapter;

//	private static EventActivity instance;
	
	private Database mDb = null;

    private FilterBar tf;
    
    /*
     * 用于标记活动状态
     */
    private int eventStateTag = 0;
    /*
     * 用于标记左侧筛选栏状态
     */
    private int curFilterLeftTag = 0;

    private String outletEventId;


//	public static EventActivity instance() {
//		if (instance == null)
//				return null;
//		return instance;
//	}

//	public static boolean isInstancated() {
//		return instance != null;
//	}
	
	private PullToRefreshListView pullListView;
	private void initView() {
        ibRight = (ImageButton) findViewById(R.id.right_button);
		txtTitle = (TextView) findViewById(R.id.title_text);
		pullListView = (PullToRefreshListView) findViewById(R.id.event_list);
		lvEvent = pullListView.getRefreshableView();
        //lvEvent.setEmptyView(findViewById(R.id.progressbar_event_loading));
        newEventPanel = findViewById(R.id.new_event_panel);

        // 只有已关联到学校的教师才可发布活动
        ibRight.setVisibility(View.GONE);
        if(Buddy.ACCOUNT_TYPE_TEACHER == PrefUtil.getInstance(this).getMyAccountType()){
            // 检查是否有学校
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    return !WowTalkWebServerIF.getInstance(EventActivity.this).getMySchools(false).isEmpty();
                }

                @Override
                protected void onPostExecute(Boolean hasSchools) {
                    ibRight.setVisibility(hasSchools ? View.VISIBLE : View.GONE);
                }
            });
        }

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
        pullListView.setOnRefreshListener(this);
        pullListView.setOnLastItemVisibleListener(this);

        //默认下载所有即将进行的活动
        downloadLatestEvents(GET_COMMING_EVENT_0,null);
        hideNewEventPanel();

        AQuery q = new AQuery(this);
        q.find(R.id.title_left).clicked(this);
        q.find(R.id.vg_new_qa).clicked(this);
        q.find(R.id.vg_new_vote).clicked(this);
        q.find(R.id.vg_new_offline).clicked(this);
        q.find(R.id.new_event_panel).clicked(this);

	}
	
	private void downloadLatestEvents(final String get_finished_event_only,final String max_startdate) {
        lastRefreshEventTime=System.currentTimeMillis();

		new Thread(new Runnable() {

			@Override
			public void run() {
				int errno = EventWebServerIF.getInstance(EventActivity.this).fGetLatestEvents(get_finished_event_only,max_startdate);
				if (errno == ErrorCode.OK) {
                    refresh();
				}
			}
			
		}).start();
	}

	protected void refresh() {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fSetShownEvents();
            }
        });
	}
	
    private void downloadPreviousEvents(final String timestamp) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                int errno = EventWebServerIF.getInstance(EventActivity.this).fGetPreviousEvents(timestamp);
                if (errno == ErrorCode.OK) {
                    refresh();
                }
            }

        }).start();
    }

    private void fSetShownEvents() {
        switch (curFilterLeftTag) {
            case 0:
                acts = mDb.fetchAllEvents();
                break;
            case 1:
                acts = mDb.fetchJoinedEvents();
                break;
            case 2:
                acts = mDb.fetchNotJoinedEvents();
                break;
            default:
                acts = mDb.fetchAllEvents();
                break;
        }

        fixEventLocalPath();

        if(eventAdapter != null){
            if (curFilterLeftTag == 0) {
                eventAdapter.setDataSource(acts);
            } else if (curFilterLeftTag == 1) { // joined
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                for (WEvent e : acts) {
                    if (e.membership == WEvent.MEMBER_SHIP_JOINED) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            } else if (curFilterLeftTag == 2) { // my
                ArrayList<WEvent> filtered = new ArrayList<WEvent>();
                String myUid = PrefUtil.getInstance(this).getUid();
                for (WEvent e : acts) {
                    if (TextUtils.equals(myUid, e.owner_uid)) {
                        filtered.add(e);
                    }
                }
                eventAdapter.setDataSource(filtered);
            }
        }else{
            eventAdapter = new EventAdapter(EventActivity.this, acts);
            lvEvent.setAdapter(eventAdapter);
            eventAdapter.notifyDataSetChanged();
        }
        pullListView.onRefreshComplete();
    }

    private void fixEventLocalPath() {
        for(WEvent e : acts) {
            WEventUiHelper.fixMediaLocalPath(e);
        }
    }

	private void fGointoASpecificEvent(WEvent wa) {
		if (wa == null)
			return;
        outletEventId = wa.id;
		Intent intent = new Intent(this, EventDetailActivity.class);
		Bundle b = new Bundle();
		b.putParcelable(EVENT_DETAIL_BUNDLE, wa);
		intent.putExtras(b);
		startActivity(intent);
	}

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case NEW_EVENT:
                    WEvent event = data.getParcelableExtra("event");
                    ArrayList<WEvent> events = new ArrayList<WEvent>();
                    events.add(event);
                    if (acts != null)
                        events.addAll(acts);
                    if (eventAdapter != null) {
                        eventAdapter.clear();
                        eventAdapter.addAll(events);
                    } else {
                        eventAdapter = new EventAdapter(EventActivity.this, events);
                        lvEvent.setAdapter(eventAdapter);
                    }
                    break;
            }
        }
    }

    private final static int NEW_EVENT = 100001;

    private void gotoCreateEvent(String cat) {
        startActivityForResult(new Intent(EventActivity.this, CreateEventActivity.class)
                .putExtra(CreateEventActivity.EXTRA_EVENT_CATEGORY, cat)
                .putExtra(CreateEventActivity.EXTRA_PAGE_TITLE,
                        WEventUiHelper.getEventCatetoryText(this, cat)), NEW_EVENT);
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

        // 活动对象的状态可能已经发生了变化（比如报名人数），
        // 因此从数据库中读取最新状态。
        if (eventAdapter != null && outletEventId != null) {
            for (int i = 0; i < eventAdapter.getCount(); ++i) {
                if (TextUtils.equals(eventAdapter.getItem(i).id, outletEventId)) {
                    WEvent e = mDb.fetchEvent(outletEventId);
                    if (e != null) {
                        WEventUiHelper.fixMediaLocalPath(e);
                        eventAdapter.replaceItem(i, e);
                    }
                    break;
                }
            }
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
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
        	curFilterLeftTag = itemIdx;


            if (curFilterLeftTag == 0) {
                downloadLatestEvents(GET_COMMING_EVENT_0,null);
                MenuBar.getBtnFilter1().setText("所有活动");
            } else if (curFilterLeftTag == 1) { // on going
                downloadLatestEvents(null,null);
                MenuBar.getBtnFilter1().setText("已报名活动");
            } else if (curFilterLeftTag == 2) { // expired
                downloadLatestEvents(GET_FINISHED_EVENT_1,null);
                MenuBar.getBtnFilter1().setText("我发布的活动");
            }

        } else if (subMenuResId == R.id.btn_filter2) {
        	eventStateTag = itemIdx;
            if (itemIdx == 0) {
            	downloadLatestEvents(GET_COMMING_EVENT_0,null);
                MenuBar.getBtnFilter2().setText("即将进行的活动");
            } else if (itemIdx == 1) { // on going
            	downloadLatestEvents(null,null);
                MenuBar.getBtnFilter2().setText("进行中的活动");
            } else if (itemIdx == 2) { // expired
                downloadLatestEvents(GET_FINISHED_EVENT_1,null);
                MenuBar.getBtnFilter2().setText("已结束的活动");
            }


        }
//        Toast.makeText(EventActivity.this, subMenuResId + "/" + itemIdx, Toast.LENGTH_LONG).show();

    }

    private Spannable formatField(String label, String value){
        return formatField(label, value,
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.text_gray2));
    }

    private Spannable formatField(String label, String value, int color1, int color2){
        int start = 0;
        SpannableStringBuilder str = new SpannableStringBuilder(label == null ? "" : label);
        str.append(": ");
        int end = str.length();
        str.setSpan(new ForegroundColorSpan(color1),
                start, end, 0);
        start = end;
        str.append(value == null ? "" : value);
        end = str.length();
        str.setSpan(new ForegroundColorSpan(color2),
                start, end, 0);
        return str;
    }

	@Override
	public void onRefresh(PullToRefreshBase<ListView> refreshView) {
		if(curFilterLeftTag == 0){
		switch (eventStateTag) {
            //即将进行
            case 0:
                downloadLatestEvents(GET_COMMING_EVENT_0,null);
                break;
            //进行中
            case 1:
                downloadLatestEvents(null,null);
                break;

            //已过期
            case 2:
                downloadLatestEvents(GET_FINISHED_EVENT_1,null);
                break;
            default: break;
            }
		}else{
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					pullListView.onRefreshComplete();					
				}
			});
		}
	}

	//上啦加载更多
	private void upPullLoadmore(final String get_finished_event_only, final String max_startdate, final int position) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				int errno = EventWebServerIF.getInstance(EventActivity.this).fGetLatestEvents(get_finished_event_only,max_startdate);
				if (errno == ErrorCode.OK) {
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							switch (curFilterLeftTag) {
					            case 0:
					                acts = mDb.fetchAllEvents();
					                break;
					            case 1:
					                acts = mDb.fetchJoinedEvents();
					                break;
					            case 2:
					                acts = mDb.fetchNotJoinedEvents();
					                break;
					            default:
					                acts = mDb.fetchAllEvents();
					                break;
				        }

				        fixEventLocalPath();
				        eventAdapter.addAll(acts);
						}
					});
				}
			}
			
		}).start();
	}
	
	@Override
	public void onLastItemVisible() {
		String time = null;
		if(acts.size() > 0){
			switch (eventStateTag) {
                //即将进行
                case 0:
                    time = acts.get(acts.size()-1).startTime.getTime() + "";
                    upPullLoadmore(GET_COMMING_EVENT_0, time.substring(0, time.length() - 3), acts.size());
                    break;

                //进行中
                case 1:
                    time = acts.get(acts.size()-1).startTime.getTime() + "";
                    upPullLoadmore(null, time.substring(0, time.length() - 3), acts.size());
                    break;

                //已过期
                case 2:
                    time = acts.get(acts.size() -1).startTime.getTime() + "";
                    upPullLoadmore(GET_FINISHED_EVENT_1, time.substring(0, time.length() - 3), acts.size());
                    break;
                default: break;
                }
		}
		
	}
}
