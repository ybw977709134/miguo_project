package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.androidquery.AQuery;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;

import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.GroupTreeAdapter;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * Created by jacky on 15-3-27.
 */
public class STSchoolMateFragment extends Fragment implements AdapterView.OnItemClickListener{
    private String[] path;

    GroupTreeAdapter adapter;
    List<GroupChatRoom> schools;
    int errno;
    AQuery aQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getArguments().getStringArray(SendToActivity.INTENT_PAHT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.activity_parentchatroom, container, false);
        rootview.findViewById(R.id.title_bar).setVisibility(View.GONE);
        aQuery = new AQuery(rootview);
        aQuery.find(R.id.listview).itemClicked(this);

        schools = new Database(getActivity()).fetchSchools();
        updateUi();

        if (isEmpty()) {
            refresh();
        }
        return rootview;
    }

    public void refresh() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                schools.clear();
                errno = WowTalkWebServerIF.getInstance(getActivity()).getMySchoolsErrno(true, schools);
                return errno;
            }

            @Override
            public void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    new Database(getActivity()).storeSchools(schools);
                } else {
                    Toast.makeText(getActivity(),R.string.timeout_contacts_message, Toast.LENGTH_SHORT);
                }
                updateUi();
            }
        });
    }

    private void updateUi() {
        if (!isEmpty()) {
            adapter = new GroupTreeAdapter(getActivity(), schools);
            aQuery.find(R.id.listview).adapter(adapter);
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.GONE);
        } else {
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.VISIBLE);
            schools.clear();
        }
    }

    public boolean isEmpty() {
        return schools == null || schools.isEmpty();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        try {
            Context context = getActivity();
            Object item = adapterView.getItemAtPosition(i);
            if (item instanceof ContactTreeNode) {
                ContactTreeNode contact = (ContactTreeNode) item;
                if (!contact.isGroup()) {
                    String uid = contact.getGUID();
                    String myUid = PrefUtil.getInstance(context).getUid();
                    if (TextUtils.equals(uid, myUid)) {
                        Toast.makeText(context,"请不要发给自己!",Toast.LENGTH_LONG).show();
                    }else {
                        MessageComposerActivity.launchToChatWithBuddyWithPicture(context,uid,path,true);
                    }
                }
            }
        } catch (Exception e) {

        }
    }
}
