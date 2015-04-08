package co.onemeter.oneapp.ui;


import java.util.LinkedList;
import java.util.List;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Bulletins;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.GroupMember;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageDialog;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 班级通知页面。
 * Created by zz on 03/31/2015.
 */
public class ClassNotificationActivity extends Activity implements OnClickListener,OnItemLongClickListener, OnRefreshListener<ListView>, OnLastItemVisibleListener,MenuBar.OnDropdownMenuItemClickListener, OnScrollListener{
	private ImageButton btn_notice_back;
	private ImageButton btn_add;
//	private ListView listView_notice_show;
	private PullToRefreshListView pullListView;
	private ListView lvEvent;
	
	private List<Bulletins> bulletins;
	private BulletinAdapter adapter;
	private String classId;
	private ClassFilterBar filterBar;
	private List<GroupChatRoom> classrooms;
	private String[] className = new String[]{};
	private String tag;
	private String classDetail_classId;
	private int count = 10;
	private long lastTimeStamp = 0;
	private Database dbHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_class_notification);
		
		initView();
		bulletins.clear();
		getClassBulletin(lastTimeStamp);
		getSchoolClassInfo();

	}
	
	private void initView(){
		dbHelper = new Database(ClassNotificationActivity.this);
		tag = getIntent().getStringExtra(ClassDetailActivity.EXTRA_CLASS_DETAIL);
		classDetail_classId = getIntent().getStringExtra("classId");
		btn_notice_back = (ImageButton) findViewById(R.id.btn_notice_back);
		btn_add = (ImageButton) findViewById(R.id.btn_add);
		pullListView = (PullToRefreshListView) findViewById(R.id.listView_notice_show);
		lvEvent = pullListView.getRefreshableView();
		
		btn_notice_back.setOnClickListener(this);
		btn_add.setOnClickListener(this);
		
		bulletins = new LinkedList<Bulletins>();
		adapter = new BulletinAdapter(ClassNotificationActivity.this,bulletins);
		lvEvent.setAdapter(adapter);
//		classId = "1678ff8f-2a41-438a-bb22-4f55530857f1";
		if(classDetail_classId != null){
			classId = classDetail_classId;
		}
		lvEvent.setOnItemLongClickListener(this);
		
		if(PrefUtil.getInstance(ClassNotificationActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT){
			btn_add.setVisibility(View.GONE);
		}
		
		View c = findViewById(R.id.dialog_container);
        c.setVisibility(View.INVISIBLE);
        filterBar = new ClassFilterBar(this,c);
        filterBar.setOnFilterChangedListener(this);
        if(tag == null){
        	lvEvent.addHeaderView(filterBar.getView());
        }
        
		pullListView.setOnRefreshListener(this);
        pullListView.setOnLastItemVisibleListener(this);
        pullListView.setOnScrollListener(this);
        classrooms = new LinkedList<GroupChatRoom>();
        filterBar.setStringArrayData(className);
	}
	private void getClassBulletin(final long time){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
//				bulletins.clear();
				WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(ClassNotificationActivity.this);
				bulletins.addAll(web.fGetClassBulletin(classId,time,count));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {			
				adapter.notifyDataSetChanged();
				pullListView.onRefreshComplete();
			}

		});
	}
	
	private void delClassBulletin(final int position){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				int errno= WowTalkWebServerIF.getInstance(ClassNotificationActivity.this).delClassBulletin(bulletins.get(position).bulletin_id);				
				return errno;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				bulletins.clear();
				getClassBulletin(0);			
			}

		});
	}
	private void getSchoolClassInfo(){
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				classrooms.clear();
				classrooms.addAll(WowTalkWebServerIF.getInstance(ClassNotificationActivity.this).getSchoolClassRooms(null));
				return null;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				int length = classrooms.size();
				className = new String[length + 1];
				className[0] = "全部";
				for(int i= 1;i < length+1;i++){
					className[i] = classrooms.get(i-1).groupNameOriginal;					
				}
				filterBar.setStringArrayData(className);

			}

		});
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_notice_back:
			finish();
			break;
		case R.id.btn_add:
			Intent intent = new Intent(ClassNotificationActivity.this, SendNotificationActivity.class);
			Bundle bundle=new Bundle();
			bundle.putStringArray("className", className);
			bundle.putString("classDetail_classId", classDetail_classId);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1001);;
			break;
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(requestCode == 1001){
				getClassBulletin(lastTimeStamp);
			}
		}
	}
	
	class BulletinAdapter extends BaseAdapter{
		private List<Bulletins> bulletins;
		private Context context;

		public BulletinAdapter(Context context,List<Bulletins> bulletins){
			this.context = context;
			this.bulletins = bulletins;
		}
		@Override
		public int getCount() {
			return bulletins.size();
		}

		@Override
		public Object getItem(int position) {
			return bulletins.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHodler holder = null;
			if(null == convertView){
				holder = new ViewHodler();
				convertView = getLayoutInflater().inflate(R.layout.listitem_notice, parent, false);
				holder.notice_photo = (ImageView) convertView.findViewById(R.id.notice_photo);
				holder.notice_name = (TextView) convertView.findViewById(R.id.notice_name);
				holder.notice_time = (TextView) convertView.findViewById(R.id.notice_time);
				holder.textView_notice = (TextView) convertView.findViewById(R.id.textView_notice);
				convertView.setTag(holder);
			}else{
				holder = (ViewHodler) convertView.getTag();
			}
			Bulletins b = bulletins.get(position);
			if(b.uid != null){
			Database dbHelper=new Database(context);
//            Buddy buddy=dbHelper.buddyWithUserID(b.uid);
			if(classId == null){
				classId = b.class_id;
			}
            List<GroupMember> buddyList =  dbHelper.fetchGroupMembers(classId);
            if(buddyList != null){
            	for(GroupMember buddy : buddyList){
            	if(buddy.getGUID().equals(b.uid)){
            	    holder.notice_name.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
            	    PhotoDisplayHelper.displayPhoto(context, holder.notice_photo,
            	    		R.drawable.default_official_avatar_90, buddy, true);
            	}
            	
            }
            }
            
            
            	
    			String text = dbHelper.fetchMoment(b.moment_id).text;
    			long timestamp = dbHelper.fetchMoment(b.moment_id).timestamp;
    			holder.textView_notice.setText(text);
    			holder.notice_time.setText(String.valueOf(Utils.stampsToDateTime(timestamp)));
            }
            
            
			
			return convertView;
		}
		class ViewHodler{
			ImageView notice_photo;
			TextView notice_name;
			TextView notice_time;
			TextView textView_notice;
		}	
		
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			final int position, long id) {
		if(PrefUtil.getInstance(ClassNotificationActivity.this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
			MessageDialog dialog = new MessageDialog(ClassNotificationActivity.this);
            dialog.setTitle("");
            dialog.setMessage("你确定要删除吗?");
            dialog.setOnRightClickListener("取消", null);
            
            dialog.setOnLeftClickListener("确定", new MessageDialog.MessageDialogClickListener() {
                @Override
                public void onclick(MessageDialog dialog) {
                    
                    delClassBulletin(position-2);
                    dialog.dismiss();
                }
            }
            );
            dialog.show();
		}
		 
		
		return true;
	}
	static class ClassFilterBar extends MenuBar {

	    public String[] hosts = new String[]{};

	    /**
	     * @param context
	     * @param dialogBackground 作为对话框下方的屏幕背景，一般为半透明的黑色。
	     */
	    public ClassFilterBar(Context context, View dialogBackground) {
	        super(context, R.layout.class_filter,  new int[]{ R.id.btn_filter_class}, dialogBackground);
	        setBackgroundColor(0xFFFFFFFF);
	    }

	    @Override
	    protected String[] getSubItems(int itemId) {
	        switch (itemId) {
	            case R.id.btn_filter_class:
	                return hosts;
	        }
	        return hosts;
	    }

	    public void setStringArrayData(String[] data){
	        this.hosts = data;
	    }
	}
	@Override
	public void onRefresh(PullToRefreshBase<ListView> refreshView) {
		bulletins.clear();
		getClassBulletin(0);
		
	}

	@Override
	public void onLastItemVisible() {	
		
	}

	@Override
	public void onDropdownMenuShow(int subMenuResId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDropdownMenuItemClick(int subMenuResId, int itemIdx) {
		if(itemIdx == 0){
			classId = null;
		}else{
			classId = classrooms.get(itemIdx-1).groupID;
		}
		bulletins.clear();
		getClassBulletin(0);
		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == OnScrollListener.SCROLL_STATE_IDLE){
			if(view.getLastVisiblePosition() == view.getCount() - 1){
				String momentId = bulletins.get(bulletins.size() - 1).moment_id;
				lastTimeStamp = dbHelper.fetchMoment(momentId).timestamp;
				getClassBulletin(lastTimeStamp);
				
			}
		}
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		
	}

}
