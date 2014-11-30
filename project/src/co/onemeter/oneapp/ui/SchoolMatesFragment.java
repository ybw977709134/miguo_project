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
import co.onemeter.oneapp.contacts.adapter.GroupTreeAdapter;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import com.androidquery.AQuery;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.ui.BottomButtonBoard;

import java.util.ArrayList;
import java.util.UUID;

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
//        ArrayList<GroupChatRoom> groups = new Database(getActivity()).fetchAllGroupChatRooms();
        ContactUtil.fFetchNormalPersons(getActivity());
        ArrayList<GroupChatRoom> groups = makeFakeData(ContactGroupIterationAdapter.GROUP_ID_ROOT, 3, 0, 3);
        adapter = new GroupTreeAdapter(getActivity(), groups);
        aQuery.find(R.id.listview).adapter(adapter);
    }

    private ArrayList<GroupChatRoom> makeFakeData(
            String parentGroupId, int count, int currLevel, int recursiveDepth) {
        ArrayList<GroupChatRoom> result = new ArrayList<GroupChatRoom>(count);

        for (int groupId = 0; groupId < count; ++groupId) {
            // ctor params:
            // String groupID,
            // String groupNameOriginal,
            // String groupNameLocal,
            // String groupStatus,
            // int maxNumber,
            // int memberCount,
            // boolean isTemporaryGroup
            String name = String.format("GROUP %d-%d", currLevel, groupId + 1);
            GroupChatRoom g = new GroupChatRoom(
                    UUID.randomUUID().toString(),
                    name,
                    name,
                    "",
                    10,
                    10,
                    false
            );
            g.parentGroupId = parentGroupId;
            g.level = currLevel;
            g.memberList = ContactUtil.allBuddies;

            if (recursiveDepth > 0) {
                ArrayList<GroupChatRoom> children = makeFakeData(g.groupID, count, currLevel + 1, recursiveDepth - 1);
                if (g.childGroups == null) {
                    g.childGroups = children;
                } else {
                    g.childGroups.addAll(children);
                }
            }

            result.add(g);
        }
        return result;
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
    public int[] getOptionsMenuItemIcons(Context context) {
        return new int[] { R.drawable.nav_refresh_selector };
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