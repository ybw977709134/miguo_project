package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactGroupAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;
import co.onemeter.oneapp.contacts.model.Person;

/**
 * Created by jacky on 15-3-27.
 */
public class STContactsFragment extends Fragment implements SideBar.OnTouchingLetterChangedListener{

    private View mContentView;
    private ListView lvContacts;
    private ListView lvGroups;
    private SideBar mSideBar;
    private ScrollView mainscrollview;
    private LinearLayout mMainLinearLayout;
    private MessageBox mMsgBox;

    private ContactListAdapter contactAdapter;
    private ContactGroupAdapter groupsAdapter;

    private ArrayList<Buddy> buddie = new ArrayList<>();
    private ArrayList<GroupChatRoom> groupRooms = new ArrayList<>();
    private Handler mHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_stcontacts,container,false);
        mMsgBox = new MessageBox(getActivity());
        initView();
        return mContentView;
    }

    private void initView(){
        lvContacts = (ListView) mContentView.findViewById(R.id.groups_list);
        lvGroups = (ListView) mContentView.findViewById(R.id.contacts_list);
        mSideBar = (SideBar) mContentView.findViewById(R.id.side_bar_stcontacts);
        mMainLinearLayout = (LinearLayout) mContentView.findViewById(R.id.main_linear_layout);
        mainscrollview = (ScrollView) mContentView.findViewById(R.id.stcontacts_scrollview);

        mSideBar.setOnTouchingLetterChangedListener(this);

        getMyBuddyListFromLocal();;
        getMyGroupsFromLocal();
    }

    private void gotoTop(){
        mainscrollview.smoothScrollTo(0,0);
    }

    //处理siderbar的触摸事件
    @Override
    public void onTouchingLetterChanged(String s) {
        if(mMsgBox == null)
            mMsgBox = new MessageBox(getActivity());
        if (s.equals("Se")) {
            mMsgBox.toast(R.string.search_toast, 0);
            gotoTop();
        } else {
            mMsgBox.toast(s, 0);
            if (contactAdapter == null)
                return;
            ArrayList<Person> persons = contactAdapter.getPersonSource();
            if (persons == null || persons.isEmpty())
                return;

            int height = 0;
            for (int i = 0; i < mMainLinearLayout.getChildCount() - 1; i++) {
                View view = mMainLinearLayout.getChildAt(i);
                if (view != null) {
                    height += view.getHeight();
                }
            }
            int numCount = 0;
            for (Person person : persons) {
                if (person.getSortKey().substring(0, 1).toUpperCase().equals(s)) {
                    mainscrollview.smoothScrollTo(0, height);
                    break;
                }
                View view = lvContacts.getChildAt(numCount);
                if (view != null) {
                    height += view.getHeight();
                    numCount++;
                }
            }
        }
    }

    @Override
    public void onTouchCanceled() {
        if(mMsgBox != null){
            mMsgBox.dismissToast();
        }
    }

    private void getMyBuddyListFromLocal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                contactAdapter = new ContactListAdapter(getActivity());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        lvContacts.setAdapter(contactAdapter);
                        ListHeightUtil.setListHeight(lvContacts);
                    }
                });
            }
        }).start();
    }

    private void getMyGroupsFromLocal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Database dbHelper = new Database(getActivity());
                groupRooms = dbHelper.fetchNonTempGroupChatRooms();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        groupsAdapter = new ContactGroupAdapter(getActivity(), groupRooms);
                        lvGroups.setAdapter(groupsAdapter);
                        ListHeightUtil.setListHeight(lvGroups);
                    }
                });
            }
        }).start();
    }
}
