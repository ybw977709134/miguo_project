package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.GroupTreeAdapter;
import co.onemeter.oneapp.contacts.model.ContactTreeNode;
import co.onemeter.oneapp.utils.ThemeHelper;
import com.androidquery.AQuery;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MessageBox;

import java.util.List;

/**
 * <p>显示“校园里”通讯录。</p>
 * Created by pzy on 11/22/14.
 */
public class SchoolMatesFragment extends Fragment
        implements BottomButtonBoard.OptionsMenuProvider, AdapterView.OnItemClickListener {

    private static final int REQ_ADD_CLASS = 1;
    Adapter adapter;
    AQuery aQuery;
    MessageBox msgbox;
    List<GroupChatRoom> schools;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schoolmates, container, false);
        aQuery = new AQuery(v);
        aQuery.find(R.id.listview).itemClicked(this);

        msgbox = new MessageBox(getActivity());

        schools = new Database(getActivity()).fetchSchools();
        updateUi();
        if (isEmpty())
            refresh();

        return v;
    }

    public boolean handleBackPress() {
        return false;
    }

    public void gotoTop() {

    }

    public void refresh() {
        msgbox.showWait();

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                schools = WowTalkWebServerIF.getInstance(getActivity()).getMySchools();
                return schools != null && !schools.isEmpty() ? ErrorCode.OK : ErrorCode.OPERATION_FAILED;
            }

            @Override
            public void onPostExecute(Integer errno) {
                msgbox.dismissWait();

                if (errno == ErrorCode.OK) {
                    new Database(getActivity()).storeSchools(schools);
                }

                updateUi();
            }
        }.execute((Void)null);
    }

    private void updateUi() {
        if (!isEmpty()) {
            adapter = new GroupTreeAdapter(getActivity(), schools);
            aQuery.find(R.id.listview).adapter(adapter);
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.GONE);
        } else {
            aQuery.find(R.id.schoolmate_emptyview).visibility(View.VISIBLE);
        }
    }

    @Override
    public String[] getOptionsMenuItems(Context context) {
        return new String[] {
                context.getString(R.string.add),
                context.getString(R.string.refresh_from_server) };
    }

    @Override
    public int[] getOptionsMenuItemIcons(Context context) {
        return new int[] { R.drawable.nav_refresh_selector, R.drawable.nav_add_selector };
    }

    @Override
    public boolean onOptionsItemSelected(int position) {
        switch (position) {
            case 1: {
                Intent intent = new Intent(getActivity(), AddClassActivity.class);
                ThemeHelper.putExtraCurrThemeResId(intent, getActivity());
                startActivityForResult(intent, REQ_ADD_CLASS);
                return true;
            }
            case 0:
                refresh();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADD_CLASS && resultCode == Activity.RESULT_OK) {
            refresh();
        }
    }

    public boolean isEmpty() {
        return schools == null || schools.isEmpty();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        try {
            Context context = getActivity();
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
                        ContactInfoActivity.launch(context, uid,
                                buddy == null ? 0 : buddy.getFriendShipWithMe());
                    } else {
                        startActivity(new Intent(context, MyInfoActivity.class));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}