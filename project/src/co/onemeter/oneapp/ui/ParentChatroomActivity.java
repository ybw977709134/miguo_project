package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final int REQ_Parent_Chat = 1;

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
        findViewById(R.id.btn_add).setOnClickListener(this);

        msgbox = new MessageBox(this);

        Database db = new Database(this);
        if(mIsFromHomeWork){
            schools.addAll(db.fetchSchoolsJustTeacher());
        }else {
            schools.addAll(db.fetchSchoolsNoBuddies());
        }
        db.close();

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
                    Database dbHelper = new Database(ParentChatroomActivity.this);
                    dbHelper.storeSchools(schools);
                    schools.clear();
                    if(mIsFromHomeWork){
                        schools.addAll(dbHelper.fetchSchoolsJustTeacher());
                    }else {
                        schools.addAll(dbHelper.fetchSchoolsNoBuddies());
                    }
                    dbHelper.close();
                } else {
                    msgbox.toast(R.string.timeout_contacts_message, Toast.LENGTH_SHORT);
                }
                updateUi();
            }
        });
    }

    private void updateUi() {
        if (isEmpty()) {
            schools.clear();
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.GONE);
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
                                MessageComposerActivity.launchToChatWithBuddyWithPicture(this,uid,path,true);
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.title_back){
            finish();
        }

        if (v.getId() == R.id.btn_add) {
            Intent intent = new Intent(this,AddClassActivity.class);
            startActivityForResult(intent, REQ_Parent_Chat);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case  REQ_Parent_Chat:
                    refresh();
                    break;
            }
        }
    }

    private  class InnerGroupTreeAdapter extends GroupTreeAdapter {
        Context context;
        public InnerGroupTreeAdapter(Context context, Collection<GroupChatRoom> topLevelGroups) {
            super(context,topLevelGroups);
            this.context = context;
        }

        @Override
        protected void setupClassRoomItemView(final int position,final ContactTreeNode node, View view) {
            super.setupClassRoomItemView(position, node, view);
            TextView btn_chat = (TextView) view.findViewById(R.id.btn_chat);
            if(mIsFromHomeWork){
                btn_chat.setVisibility(View.GONE);
            }else {
                btn_chat.setClickable(true);
                btn_chat.setText("进入群聊");
                btn_chat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MessageComposerActivity.launchToChatWithGroup(context, MessageComposerActivity.class, node.getGUID());
                    }
                });
                TextView btn_group_name = (TextView) view.findViewById(R.id.group_name);
                btn_group_name.setClickable(true);
                btn_group_name.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MessageComposerActivity.launchToChatWithGroup(context, MessageComposerActivity.class, node.getGUID());
                    }
                });
            }
        }
    }
}
