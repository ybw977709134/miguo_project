package co.onemeter.oneapp.ui;

import java.util.ArrayList;

import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;

import com.wowtech.dslv.DragSortListView;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class FavoriteGroupsSortActivity extends ListActivity implements OnClickListener{

    private Database mDbHelper;
    private MessageBox mMsgBox;

    private ImageButton mBackBtn;
    private ImageButton mConfirmBtn;
    private ArrayList<GroupChatRoom> mSortedRooms = new ArrayList<GroupChatRoom>();
    private FavoriteGroupsAdapter mAdapter;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                DragSortListView list = getListView();
                GroupChatRoom item = (GroupChatRoom) mAdapter.getItem(from);
                mAdapter.remove(item);
                mAdapter.insert(item, to);
                list.moveCheckState(from, to);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public DragSortListView getListView() {
        return (DragSortListView) super.getListView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_groups_sort);

        mDbHelper = new Database(FavoriteGroupsSortActivity.this);
        mMsgBox = new MessageBox(FavoriteGroupsSortActivity.this);

        initView();

        DragSortListView list = getListView();
        list.setDropListener(onDrop);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSortedRooms = mDbHelper.fetchFavoriteGroupChatRooms();
        if (null == mAdapter) {
            mAdapter = new FavoriteGroupsAdapter();
            setListAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void initView() {
        mBackBtn = (ImageButton) findViewById(R.id.title_back);
        mConfirmBtn = (ImageButton) findViewById(R.id.title_confirm);

        mBackBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            handleBackEvent();
            break;
        case R.id.title_confirm:
            // 提交服务器
            confirmSortFavoriteGroups();
            break;
        default:
            break;
        }
    }

    /**
     * 向服务器提交新的排序
     */
    private void confirmSortFavoriteGroups() {
        final String[] favoriteGroupIds = new String[mSortedRooms.size()];
        for (int i = 0; i < favoriteGroupIds.length; i++) {
            favoriteGroupIds[i] = mSortedRooms.get(i).groupID;
        }

        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(FavoriteGroupsSortActivity.this).sortFavoriteGroups(favoriteGroupIds);
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                Log.d("FavoriteGroupsSortActivity#confirmSortFavoriteGroups, resultCode is " + result);
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    finish();
                } else {
                    mMsgBox.toast(R.string.favoriteGroupsSort_failure);
                }
            }
        }.execute((Void)null);
    }

    @Override
    public void onBackPressed() {
        handleBackEvent();
    }

    private void handleBackEvent() {
        finish();
    }

    private class FavoriteGroupsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSortedRooms.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortedRooms.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void remove(Object obj) {
            mSortedRooms.remove(obj);
        }

        public void insert(GroupChatRoom room, int positon) {
            mSortedRooms.add(positon, room);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            GroupChatRoom chatRoom = mSortedRooms.get(position);
            ViewHolder tempHolder = null;
            if (null == convertView) {
                tempHolder = new ViewHolder();
                convertView = LayoutInflater.from(FavoriteGroupsSortActivity.this).inflate(R.layout.listitem_favorite_group_sort, null);
                tempHolder.groupNameView = (TextView) convertView.findViewById(R.id.group_name);
                convertView.setTag(tempHolder);
            } else {
                tempHolder = (ViewHolder) convertView.getTag();
            }

            final ViewHolder holder = tempHolder;
            holder.groupNameView.setText(chatRoom.groupNameOriginal);
            return convertView;
        }

        class ViewHolder {
            TextView groupNameView;
        }
    }
}
