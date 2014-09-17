package co.onemeter.oneapp.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import com.wowtech.DraggableListView.AutoDragActivity;
import org.wowtalk.api.*;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-13
 * Time: 上午9:38
 * To change this template use File | Settings | File Templates.
 */
public class DraggableEventActivity extends AutoDragActivity implements View.OnClickListener {
    private class EventAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private ArrayList<WEvent> eventList;

        // hold file id, to avoid redundant downloading.
        private HashSet<String> mDownloadingLocks = new HashSet<String>();

        public EventAdapter(Context context, ArrayList<WEvent> list) {
            inflater = LayoutInflater.from(context);
            this.eventList = list;
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
            eventList = list;
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
            TextView txtTitle = (TextView) lView.findViewById(R.id.event_title);
            txtTitle.setText(act.title);

            TextView txtTime = (TextView) lView.findViewById(R.id.event_time);
            txtTime.setText(String.format(getResources().getString(R.string.event_time), act.event_start_date+"-"+act.event_dead_line));
            TextView txtPlace = (TextView) lView.findViewById(R.id.event_place);
            txtPlace.setText(String.format(getResources().getString(R.string.event_place), act.address));
            TextView txtCount = (TextView) lView.findViewById(R.id.event_count);
            txtCount.setText(String.format(getResources().getString(R.string.event_member_count), act.capacity));
            TextView txtIntroduce = (TextView) lView.findViewById(R.id.event_introduce);
            txtIntroduce.setText(act.description);
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
                            imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgPhoto.setImageDrawable(new BitmapDrawable(
                                    DraggableEventActivity.this.getResources(),
                                    f.localThumbnailPath));
                        } else if(!mDownloadingLocks.contains(f.thumb_fileid)) {
                            imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgPhoto.setImageDrawable(null);
                            mDownloadingLocks.add(f.thumb_fileid);

//							f.localPath = PhotoDisplayHelper.makeLocalFilePath(f.fileid, f.getExt());
                            new AsyncTask<WFile, Integer, Void>() {

                                WFile f = null;
                                boolean ok = true;

                                @Override
                                protected Void doInBackground(WFile... arg0) {

                                    f = arg0[0];

                                    WowTalkWebServerIF.getInstance(DraggableEventActivity.this)
                                            .fGetFileFromServer(f.thumb_fileid,GlobalSetting.S3_MOMENT_FILE_DIR,
                                                    new NetworkIFDelegate(){

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

                                                    }, 0, f.localThumbnailPath,null);
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void v) {
                                    mDownloadingLocks.remove(f.thumb_fileid);
                                    org.wowtalk.Log.i("download complete, fileid=" + f.thumb_fileid + ", result=" + ok);
                                    if(ok) {
                                        fSetShownEvents();
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
                lView = LayoutInflater.from(DraggableEventActivity.this).inflate(R.layout.listitem_popup, null);
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
    private TextView txtTitle;

    private ArrayList<WEvent> acts;
    private EventCategoryAdapter categroyAdapter;
    private EventAdapter eventAdapter;
    private PopupWindow categoryPopup;

    private Database mDb = null;

    private void initView() {
        findViewById(R.id.title_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.title_txt).setVisibility(View.VISIBLE);
        findViewById(R.id.title_txt_right_drawable).setVisibility(View.VISIBLE);

        mNavBar = (RelativeLayout) findViewById(R.id.title_bar);
        txtTitle = (TextView) findViewById(R.id.title_txt);
        setContentListViewAttr(new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),R.drawable.divider_320)),-1);

        txtTitle.setText(R.string.events_all);
        txtTitle.setOnClickListener(this);

        TextView tvEmptyContent=(TextView)LayoutInflater.from(this).inflate(R.layout.no_event_view_layout,null);
        setEmptyContentView(tvEmptyContent);

        fSetShownEvents();
        triggerAutoLoadMore();
        notifyLoadDataFinish();
    }

    private void downloadLatestEvents() {
        lastRefreshEventTime=System.currentTimeMillis();

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int errno = WowEventWebServerIF.getInstance(DraggableEventActivity.this).fGetLatestEvents();
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    fSetShownEvents();
                } else {
                    Log.e("get lastest events failed");
                }
                notifyRefreshFinish();
            }
        }.execute((Void) null);
    }

    private void downloadPreviousEvents(final String timestamp) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int errno = WowEventWebServerIF.getInstance(DraggableEventActivity.this).fGetPreviousEvents(timestamp);
                return errno;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    fSetShownEvents();
                } else {
                    Log.e("get previous events failed");
                }
                notifyLoadMoreFinish();
            }
        }.execute((Void) null);
    }

    private void fixEventLocalPath() {
        for(WEvent aEvent : acts) {
            if(null != aEvent.multimedias) {
                for(WFile f : aEvent.multimedias) {
                    f.localThumbnailPath= PhotoDisplayHelper.makeLocalFilePath(f.thumb_fileid, f.getExt());
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

        if(null == eventAdapter) {
            eventAdapter = new EventAdapter(DraggableEventActivity.this, acts);
            setContentListViewAdapter(eventAdapter);
        } else {
            eventAdapter.setDataSource(acts);
        }

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
        lvEventCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
//            categoryPopup = new PopupWindow(lView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
            categoryPopup = Utils.getFixedPopupWindow(lView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        }
        categoryPopup.setFocusable(true);
        categoryPopup.setOutsideTouchable(true);
        categoryPopup.setBackgroundDrawable(new BitmapDrawable());
        categoryPopup.setWidth(DensityUtil.dip2px(DraggableEventActivity.this, 250));
        categoryPopup.showAsDropDown(mNavBar, (getWindowManager().getDefaultDisplay().getWidth() - categoryPopup.getWidth()) / 2, 0);
        categoryPopup.update();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_txt:
                fShowPopup();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.event_all);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        if(mDb == null) {
            mDb = new Database(this);
        }

        initView();
    }

    private static long lastRefreshEventTime;
    private final static long REFRESH_EVENT_INTERVAL=30*1000;
    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);

        if(System.currentTimeMillis()-lastRefreshEventTime >= REFRESH_EVENT_INTERVAL) {
            triggerRefresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onRefreshHandle() {
        downloadLatestEvents();
    }

    @Override
    public void onMoreHandle() {
        String minTimeStamp="";
        if(null != acts && acts.size() > 0) {
            minTimeStamp=acts.get(acts.size()-1).timeStamp;
        }
        downloadPreviousEvents(minTimeStamp);
    }

    @Override
    public void onItemClickedHandle(AdapterView<?> parent, View view, int position, long id) {
        fGointoASpecificEvent(acts.get(position));
    }
}
