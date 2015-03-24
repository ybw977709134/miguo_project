package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.InputBoardManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.GroupTreeAdapter;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.utils.AsyncTaskExecutor;


/**
 * Created by jacky on 15-3-23.
 */
public class ParentChatroomActivity extends Activity implements AdapterView.OnItemClickListener,View.OnClickListener{

    public static  final String FLAG_HOMEWORK = "flag_from_homework";

    private InnerGroupTreeAdapter adapter;
    private MessageBox msgbox;
    private List<GroupChatRoom> schools = new ArrayList<GroupChatRoom>();
    private int errno;
    private ListView listview;

    private boolean mIsFromHomeWork = false;
    private String[] path = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_parentchatroom);

        Intent intent = getIntent();
        mIsFromHomeWork = intent.getBooleanExtra(FLAG_HOMEWORK,false);
        Uri mImageUri = intent.getData();

        if(mIsFromHomeWork){
            TextView txt_title = (TextView) findViewById(R.id.title_parentchatroom);
            txt_title.setText("在线作业");
            setImagesPath(mImageUri);
        }

        listview = (ListView)findViewById(R.id.listview);
        listview.setOnItemClickListener(this);
        findViewById(R.id.title_back).setOnClickListener(this);

        msgbox = new MessageBox(this);

        schools.addAll(new Database(this).fetchSchools());
        updateUi();
        if(schools.isEmpty()){
            refresh();
        }
    }


    private void setImagesPath(Uri imgUri){
        MediaInputHelper mediaInputHelper = new MediaInputHelper();
        mediaInputHelper.handleImageResult(this,new Intent().setData(imgUri), InputBoardManager.PHOTO_SEND_WIDTH, InputBoardManager.PHOTO_SEND_HEIGHT,
                InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT,path);
    }

    private void refresh() {
        msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                schools.clear();
                errno = WowTalkWebServerIF.getInstance(ParentChatroomActivity.this).getMySchoolsErrno(true, schools);
                return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();
                if (errno == ErrorCode.OK) {
                    new Database(ParentChatroomActivity.this).storeSchools(schools);
                } else {
                    msgbox.toast(R.string.timeout_contacts_message, Toast.LENGTH_SHORT);
                }
                updateUi();
            }
        });
    }

    private void updateUi() {
        if (isEmpty()) {
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.GONE);
            schools.clear();
            adapter = new InnerGroupTreeAdapter(this, schools);
            listview.setAdapter(adapter);
        }
    }

    private boolean isEmpty() {
        return schools == null || schools.isEmpty();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                Context context = this;
                if (context == null)
                    return;

                Object item = adapterView.getItemAtPosition(i);
                if (item instanceof ContactTreeNode) {
                    ContactTreeNode contact = (ContactTreeNode) item;
                    if (!contact.isGroup()) {
                        String uid = contact.getGUID();
                        String myUid = PrefUtil.getInstance(context).getUid();
                        if(mIsFromHomeWork){
                            if (TextUtils.equals(uid, myUid)) {
                                Toast.makeText(this,"请不要发给自己!",Toast.LENGTH_LONG).show();
                            }else {
                                Buddy buddy = new Database(context).buddyWithUserID(uid);
                                chatWith(uid, TextUtils.isEmpty(buddy.alias)?buddy.nickName:buddy.alias);
                            }
                        }else{
                            if (!TextUtils.equals(uid, myUid)) {
                                Buddy buddy = new Database(context).buddyWithUserID(uid);
                                ContactInfoActivity.launch(context, Person.fromBuddy(buddy),
                                        buddy == null ? 0 : buddy.getFriendShipWithMe(),true);
                            } else {
                                startActivity(new Intent(context, MyInfoActivity.class));
                            }
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void chatWith(String uid, String name) {
        Intent intent = new Intent(this,MessageComposerActivity.class);
        intent.putExtra(MessageComposerActivity.KEY_TARGET_UID,uid);
        intent.putExtra(MessageComposerActivity.KEY_TARGET_DISPLAYNAME, name);
        intent.putExtra(MessageComposerActivity.LAUCH_WITH_SEND_MSG,true);
        intent.putExtra(MessageComposerActivity.SEND_PIC_PATH,path[0]);
        intent.putExtra(MessageComposerActivity.SEND_PIC_THUMB,path[1]);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.title_back){
            finish();
        }
    }

    private  class InnerGroupTreeAdapter extends BaseAdapter {
        private final static int VIEW_TYPE_GROUP_SCHOOL = 0;
        private final static int VIEW_TYPE_GROUP_CLASSROOM = 1;
        private final static int VIEW_TYPE_BUDDY = 2;
        private final static int VIEW_TYPE_COUNT = 3;

        private Context context;
        private ArrayList<ContactTreeNode> items;

        public InnerGroupTreeAdapter(Context context, Collection<GroupChatRoom> topLevelGroups) {
            this.context = context;
            items = new ArrayList<ContactTreeNode>(countRecursive(topLevelGroups));
            for (GroupChatRoom g : topLevelGroups) {
                addRecursive(items, g, null);
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            ContactTreeNode node = items.get(position);
            if (node.isGroup()) {
                if (node.getIndentLevel() == 0)
                    return VIEW_TYPE_GROUP_SCHOOL;
                else
                    return VIEW_TYPE_GROUP_CLASSROOM;
            } else {
                return VIEW_TYPE_BUDDY;
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ContactTreeNode node = items.get(position);

            View lView;
            int viewType = getItemViewType(position);
            if (convertView != null) {
                lView = convertView;
            } else {
                switch (viewType) {
                    case VIEW_TYPE_GROUP_SCHOOL:
                        lView = LayoutInflater.from(context).inflate(R.layout.listitem_school, null);
                        break;
                    case VIEW_TYPE_GROUP_CLASSROOM:
                        lView = LayoutInflater.from(context).inflate(R.layout.listitem_classroom, null);
                        break;
                    case VIEW_TYPE_BUDDY:
                    default:
                        lView = LayoutInflater.from(context).inflate(R.layout.listitem_contact, null);
                        break;
                }
            }

            switch (viewType) {
                case VIEW_TYPE_GROUP_SCHOOL:
                    setupSchoolItemView(position, node, lView);
                    break;
                case VIEW_TYPE_GROUP_CLASSROOM:
                    setupClassRoomItemView(position, node, lView);
                    break;
                case VIEW_TYPE_BUDDY:
                default:
                    if(mIsFromHomeWork){
                        if(node.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
                            setupBuddyItemView(node, lView);
                        }
                    }else{
                        setupBuddyItemView(node, lView);
                    }

                    break;
            }

            return lView;
        }

        private void setupBuddyItemView(ContactTreeNode node, View view) {
            AQuery q = new AQuery(view);

            q.find(R.id.contact_name).text(node.name());
            q.find(R.id.contact_state).invisible();
            PhotoDisplayHelper.displayPhoto(context, q.find(R.id.contact_photo).getImageView(),
                    R.drawable.default_avatar_90, node, true);
            if(node.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
                q.find(R.id.imageView_tag_stu_tea).visible();
            }else{
                q.find(R.id.imageView_tag_stu_tea).gone();
            }

            indent(node, view);
        }

        private void setupSchoolItemView(final int position, final ContactTreeNode node, View view) {
            setupGroupItemView(position, node, view);
        }

        private void setupClassRoomItemView(final int position, final ContactTreeNode node, View view) {
            setupGroupItemView(position, node, view);

            if(mIsFromHomeWork){
                view.findViewById(R.id.btn_chat).setVisibility(View.GONE);
            }else {
                new AQuery(view).find(R.id.btn_chat).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MessageComposerActivity.launchToChatWithGroup(context, MessageComposerActivity.class, node.getGUID());
                    }
                });
            }
        }

        private void setupGroupItemView(final int position, final ContactTreeNode node, View view) {
            TextView txtGroupName = (TextView) view.findViewById(R.id.group_name);
            txtGroupName.setText(node.name());
            txtGroupName.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    node.isExpanded() ? R.drawable.tree_arrow_down : R.drawable.tree_arrow_right,
                    0);

            int paddingDp = 10;
            int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) paddingDp, context.getResources().getDisplayMetrics());
            txtGroupName.setCompoundDrawablePadding(paddingPx);

            indent(node, view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleTreeNode(position, node);
                }
            });
        }

        private void indent(ContactTreeNode node, View view) {
            int indentDp = 10;
            int indentPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) indentDp, context.getResources().getDisplayMetrics());
            view.setPadding(
                    node.getIndentLevel() * indentPx + view.getPaddingRight(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    view.getPaddingBottom());
        }

        private void toggleTreeNode(int position, ContactTreeNode node) {
            if (node.isExpanded()) {
                // collapse
                int i = position + 1;
                while (i < items.size() && items.get(i).getIndentLevel() > node.getIndentLevel()) {
                    items.remove(i);
                }
            } else {
                // expand
                if (node.isExpandable()) {
                    items.addAll(position + 1, node.children);
                }
            }
            node.toggleExpandingState();
            notifyDataSetChanged();
        }

        private void addRecursive(ArrayList<ContactTreeNode> items, GroupChatRoom g, GroupChatRoom parent) {
            items.add(new ContactTreeNode(g, parent));
            if (g.isExpand && g.childGroups != null && !g.childGroups.isEmpty()) {
                for (GroupChatRoom c : g.childGroups) {
                    addRecursive(items, c, g);
                }
            }
        }

        private int countRecursive(Collection<GroupChatRoom> items) {
            if (items == null || items.isEmpty()) {
                return 0;
            }
            int cnt = items.size();
            for (GroupChatRoom g : items) {
                cnt += countRecursive(g.childGroups);
            }
            return cnt;
        }
    }
}
