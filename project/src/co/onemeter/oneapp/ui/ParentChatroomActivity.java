package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

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

    private GroupTreeAdapter adapter;
    private MessageBox msgbox;
    private List<GroupChatRoom> schools;
    private int errno;
    private ListView listview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parentchatroom);

        listview = (ListView)findViewById(R.id.listview);
        listview.setOnItemClickListener(this);
        findViewById(R.id.title_back).setOnClickListener(this);

        msgbox = new MessageBox(this);

        schools = new Database(this).fetchSchools();
        updateUi();
    }

    public void refresh() {
        msgbox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
//                schools = WowTalkWebServerIF.getInstance(getActivity()).getMySchools(true);
//                return schools != null && !schools.isEmpty() ? ErrorCode.OK : ErrorCode.OPERATION_FAILED;
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
//                	schools = new Database(getActivity()).fetchSchools();
//                	if(schools != null && !schools.isEmpty()){
//                    	msgbox.toast(R.string.timeout_message);
//                	}else if(schools == null && schools.isEmpty()) {
//                		msgbox.toast(R.string.no_school_class);
//                	}
                    msgbox.toast(R.string.timeout_contacts_message, Toast.LENGTH_SHORT);
                }
                updateUi();
            }
        });
    }

    private void updateUi() {
        if (!isEmpty()) {
            adapter = new GroupTreeAdapter(this, schools);
            listview.setAdapter(adapter);
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.GONE);
        } else {
            findViewById(R.id.schoolmate_emptyview).setVisibility(View.GONE);
            schools.clear();
//            adapter.notifyDataSetChanged();
            adapter = new GroupTreeAdapter(this, schools);
            listview.setAdapter(adapter);
        }
    }

    public boolean isEmpty() {
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
                    if (!TextUtils.equals(uid, myUid)) {
                        Buddy buddy = new Database(context).buddyWithUserID(uid);
                        ContactInfoActivity.launch(context, Person.fromBuddy(buddy),
                                buddy == null ? 0 : buddy.getFriendShipWithMe(),true);
                    } else {
                        startActivity(new Intent(context, MyInfoActivity.class));
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
    }
}
