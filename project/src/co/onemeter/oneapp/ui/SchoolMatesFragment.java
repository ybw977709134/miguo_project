package co.onemeter.oneapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactGroupIterationAdapter;
import com.androidquery.AQuery;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.ui.BottomButtonBoard;

import java.util.ArrayList;

/**
 * <p>显示“校园里”通讯录。</p>
 * Created by pzy on 11/22/14.
 */
public class SchoolMatesFragment extends Fragment
        implements BottomButtonBoard.OptionsMenuProvider {

    Adapter adapter;
    AQuery aQuery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schoolmates, container, false);
        aQuery = new AQuery(v);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

//        ArrayList<GroupChatRoom> groups = new Database(getActivity()).fetchAllGroupChatRooms(true);
        ArrayList<GroupChatRoom> groups = new Database(getActivity()).fetchAllGroupChatRooms();
        adapter = new ContactGroupIterationAdapter(getActivity(), groups,
                new ContactGroupIterationAdapter.GroupNameClickedListener(){

                    @Override
                    public void onGroupNameClicked(GroupChatRoom chatRoom) {

                    }

                    @Override
                    public void onSendMsgClicked() {

                    }
                });

        aQuery.find(R.id.listview).adapter(adapter);
    }

    public boolean handleBackPress() {
        return false;
    }

    public void gotoTop() {

    }

    public void refresh() {

    }

    @Override
    public String[] getOptionsMenuItems(Context context) {
        return new String[] {
                context.getString(R.string.refresh_from_server) };
    }

    @Override
    public boolean onOptionsItemSelected(int position) {
        switch (position) {
            case 0:
                refresh();
                return true;
            default:
                return false;
        }
    }
}