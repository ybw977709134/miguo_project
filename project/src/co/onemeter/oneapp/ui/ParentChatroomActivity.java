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

        if(mIsFromHomeWork){
            Log.i("--this way--");
            schools.addAll(new Database(this).fetchSchoolsJustTeacher());
        }else {
            schools.addAll(new Database(this).fetchSchools());
        }

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
                        schools.addAll(dbHelper.fetchSchools());
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

    private  class InnerGroupTreeAdapter extends GroupTreeAdapter {
        Context context;
        public InnerGroupTreeAdapter(Context context, Collection<GroupChatRoom> topLevelGroups) {
            super(context,topLevelGroups);
            this.context = context;
        }


        @Override
        protected void setupClassRoomItemView(final int position,final ContactTreeNode node, View view) {
            super.setupClassRoomItemView(position, node, view);
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
    }
}
