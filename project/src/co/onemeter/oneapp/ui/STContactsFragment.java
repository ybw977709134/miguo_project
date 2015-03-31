package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.IHasPhoto;
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
    private String[] path;

    private View mContentView;
    private ListView lvContacts;
    private ListView lvGroups;
    private SideBar mSideBar;
    private ScrollView mainscrollview;
    private LinearLayout mMainLinearLayout;
    private MessageBox mMsgBox;

    private ContactListAdapter contactAdapter;
    private ContactGroupAdapter groupsAdapter;

    private ArrayList<GroupChatRoom> groupRooms = new ArrayList<>();
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getArguments().getStringArray(SendToActivity.INTENT_PAHT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_stcontacts,container,false);
        mMsgBox = new MessageBox(getActivity());
        initView();
        return mContentView;
    }

    private void initView(){
        lvContacts = (ListView) mContentView.findViewById(R.id.contacts_list);
        lvGroups = (ListView) mContentView.findViewById(R.id.groups_list);
        mSideBar = (SideBar) mContentView.findViewById(R.id.side_bar_stcontacts);
        mMainLinearLayout = (LinearLayout) mContentView.findViewById(R.id.main_linear_layout);
        mainscrollview = (ScrollView) mContentView.findViewById(R.id.stcontacts_scrollview);

        mSideBar.setOnTouchingLetterChangedListener(this);

        getMyBuddyListFromLocal();;
        getMyGroupsFromLocal();

        lvContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<Person> persons = contactAdapter.getPersonSource();
                String uid = persons.get(position).getGUID();
                MessageComposerActivity.launchToChatWithBuddyWithPicture(getActivity(),uid,path,true);
            }
        });

        lvGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MessageComposerActivity.launchToChatWithGroupWithPicture(getActivity(),groupRooms.get(position).groupID,path,true);
            }
        });
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
                contactAdapter = new InnerContactAdapter(getActivity());
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
                        groupsAdapter = new InnerContactGroupAdapter(getActivity(), groupRooms);
                        lvGroups.setAdapter(groupsAdapter);
                        ListHeightUtil.setListHeight(lvGroups);
                    }
                });
            }
        }).start();
    }

    private class InnerContactAdapter extends ContactListAdapter{
        Context context;
        public InnerContactAdapter(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            char oldFirstChar;
            char newFirstChar;
            if (position == 0) {
                oldFirstChar = 0;
            } else {
                oldFirstChar = mFilteredPersons.get(position - 1).getSortKey().toUpperCase().trim().charAt(0);
                if (oldFirstChar < 'A') {
                    oldFirstChar = '#';
                }
            }
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contact, null);
                holder.txtContactFirstChar = (TextView) convertView.findViewById(R.id.contact_first_char);
                holder.imgDivider = (ImageView) convertView.findViewById(R.id.divider_view);
                holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
                holder.imgSelected = (ImageView) convertView.findViewById(R.id.img_selected);
                holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
                holder.txtContactState = (TextView) convertView.findViewById(R.id.contact_state);
                holder.isStuTea = (ImageView) convertView.findViewById(R.id.imageView_tag_stu_tea);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Person person = mFilteredPersons.get(position);
            if (mIsShowFirstChar) {
                newFirstChar = person.getSortKey().toUpperCase().trim().charAt(0);
                if (newFirstChar < 'A')
                    newFirstChar = '#';
                if (newFirstChar > oldFirstChar) {
                    holder.txtContactFirstChar.setVisibility(View.VISIBLE);
                    holder.txtContactFirstChar.setText(String.valueOf(newFirstChar));
                    holder.txtContactFirstChar.setBackgroundColor(context.getResources().getColor(R.color.background_light));
                    holder.txtContactFirstChar.setTextColor(context.getResources().getColor(R.color.black_24));
                } else {
                    holder.txtContactFirstChar.setVisibility(View.GONE);
                }
            } else {
                holder.imgDivider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
            IHasPhoto entity = person;
            holder.imgPhoto.setBackgroundDrawable(null);
            if (person.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
                PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                        R.drawable.default_official_avatar_90, entity, true);
            } else {
                PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                        R.drawable.default_avatar_90, entity, true);
            }

            if(person.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
                holder.isStuTea.setVisibility(View.VISIBLE);
            }else {
                holder.isStuTea.setVisibility(View.GONE);
            }
            holder.imgSelected.setVisibility(View.GONE);

            holder.txtContactState.setVisibility(View.GONE);
            holder.txtContactName.setText(person.getName());
            holder.txtContactName.setTextColor(0xff000000);
            holder.txtContactName.setTextSize(15);
            holder.txtContactName.setGravity(Gravity.CENTER);

            return convertView;
        }

        private class ViewHolder {
            TextView txtContactFirstChar;
            ImageView imgDivider;
            ImageView imgSelected;
            ImageView imgPhoto;
            TextView txtContactName;
            TextView txtContactState;
            ImageView isStuTea;
        }
    }

    private class InnerContactGroupAdapter extends ContactGroupAdapter{
        Context context;
        public InnerContactGroupAdapter(Context context, ArrayList<GroupChatRoom> groupRooms) {
            super(context, groupRooms);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            GroupChatRoom group = groupRooms.get(position);
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contact, null);
                holder.imgThumbnail = (ImageView) convertView.findViewById(R.id.contact_photo);
                holder.txtName = (TextView) convertView.findViewById(R.id.contact_name);
                holder.txtIntroduce = (TextView) convertView.findViewById(R.id.contact_state);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.txtIntroduce.setVisibility(View.GONE);

            holder.imgThumbnail.setBackgroundDrawable(null);
            PhotoDisplayHelper.displayPhoto(context, holder.imgThumbnail, R.drawable.default_group_avatar_90, group, true);

            holder.txtName.setText(group.groupNameOriginal);
            holder.txtName.setTextColor(0xff000000);
            holder.txtName.setTextSize(15);
            holder.txtName.setGravity(Gravity.CENTER_VERTICAL);
            return convertView;
        }

        private class ViewHolder {
            ImageView imgThumbnail;
            TextView txtName;
            TextView txtIntroduce;
        }
    }
}
