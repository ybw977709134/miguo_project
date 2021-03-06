package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.ShareRangeLimitedDepAdapter;
import co.onemeter.oneapp.contacts.adapter.ContactGroupIterationAdapter;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.Moment;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-10-9
 * Time: 上午11:42
 * To change this template use File | Settings | File Templates.
 */
public class ShareRangeSelectActivity extends Activity implements View.OnClickListener{
    public final static String SHARE_RANGE_TAG="share_range_tag";
    public final static String LITMITED_DEPS="limited_deps";
    public final static String SHOWN_ONLY="shown_only";

    private MessageBox mMsgBox;
    private Database mDb;

    private int shareRangeTag;
    private ArrayList<String> limitedDepsList;

    private RelativeLayout rlSharePublicLayout;
    private ImageView ivSharePublicInd;
    private RelativeLayout rlShareLimitedLayout;
    private ImageView ivShareLimitedInd;
    private ListView lvLimitedDep;
    private TextView tvAddDep;

    private ArrayList<GroupChatRoom> departmentList;
    private ArrayList<GroupChatRoom> rootGroups;

    private ImageButton btnTitleLeft;
    private View departmentLayout;

    private ShareRangeLimitedDepAdapter limitedDepAdapter;
    private boolean isShownOnly;

    /**
     * 利用GroupChatRoom的parentGrupId属性，构造GroupChatRoom的树形结构
     */
    public static ArrayList<GroupChatRoom> treeAllGroupRooms(Context context, ArrayList<GroupChatRoom> mAllGroupRooms) {
        ArrayList<GroupChatRoom> rootGroups = new ArrayList<GroupChatRoom>();
        GroupChatRoom outerGroup = null;
        GroupChatRoom innerGroup = null;
        for (int i = 0; i < mAllGroupRooms.size(); i++) {
            outerGroup = mAllGroupRooms.get(i);
            if (null == outerGroup.childGroups) {
                outerGroup.childGroups = new ArrayList<GroupChatRoom>();
            }
            if (!outerGroup.isEditable && TextUtils.isEmpty(outerGroup.parentGroupId)) {
                outerGroup.groupNameOriginal = context.getString(R.string.contactsforbiz_root_group_name_display);
                rootGroups.add(outerGroup);
            }
            for (int j = i + 1; j < mAllGroupRooms.size(); j++) {
                innerGroup = mAllGroupRooms.get(j);
                // outerGroup可能是innerGroup的父节点；也可能是innerGroup的子节点
                if (!TextUtils.isEmpty(innerGroup.parentGroupId)
                        && innerGroup.parentGroupId.equals(outerGroup.groupID)) {
                    outerGroup.childGroups.add(innerGroup);
                } else if (!TextUtils.isEmpty(outerGroup.parentGroupId)
                        && outerGroup.parentGroupId.equals(innerGroup.groupID)) {
                    if (null == innerGroup.childGroups) {
                        innerGroup.childGroups = new ArrayList<GroupChatRoom>();
                    }
                    innerGroup.childGroups.add(outerGroup);
                }
            }
        }
        return rootGroups;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_range_select_with_dep_layout);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {
        mMsgBox = new MessageBox(this);
        mDb = new Database(this);

        limitedDepsList=getIntent().getStringArrayListExtra(LITMITED_DEPS);

        rlSharePublicLayout=(RelativeLayout) findViewById(R.id.share_range_all_layout);
        ivSharePublicInd=(ImageView) findViewById(R.id.share_range_public_ind);
        rlShareLimitedLayout=(RelativeLayout) findViewById(R.id.share_range_limited_layout);
        ivShareLimitedInd=(ImageView) findViewById(R.id.share_range_limited_ind);
        lvLimitedDep=(ListView) findViewById(R.id.limited_dep_list);
        tvAddDep=(TextView) findViewById(R.id.tv_add_selected_department);

        isShownOnly=getIntent().getBooleanExtra(SHOWN_ONLY,false);
        if(isShownOnly) {
            findViewById(R.id.title_share_range_confirm).setVisibility(View.GONE);
            tvAddDep.setVisibility(View.GONE);
        }

        limitedDepAdapter=new ShareRangeLimitedDepAdapter(this,limitedDepsList,isShownOnly);
        lvLimitedDep.setAdapter(limitedDepAdapter);

        btnTitleLeft = (ImageButton) findViewById(R.id.title_back);
        departmentLayout=findViewById(R.id.layout_department);

        btnTitleLeft.setOnClickListener(this);
        findViewById(R.id.title_share_range_confirm).setOnClickListener(this);

        rlSharePublicLayout.setOnClickListener(this);
//        ivSharePublicInd.setOnClickListener(this);
        rlShareLimitedLayout.setOnClickListener(this);
//        ivShareLimitedInd.setOnClickListener(this);
//        lvLimitedDep.setOnClickListener(this);
        tvAddDep.setOnClickListener(this);

        if(limitedDepsList.size() > 0) {
            setShareRangeStatus(false);
        } else {
            setShareRangeStatus(true);
        }
    }

    private void setShareRangeStatus(boolean isPublic) {
        if(isPublic) {
            ivSharePublicInd.setVisibility(View.VISIBLE);
            ivShareLimitedInd.setVisibility(View.INVISIBLE);

            shareRangeTag = Moment.VISIBILITY_ALL;
        } else {
            ivSharePublicInd.setVisibility(View.INVISIBLE);
            ivShareLimitedInd.setVisibility(View.VISIBLE);

            shareRangeTag = Moment.VISIBVILITY_LIMITED;
        }
    }

    private ContactGroupIterationAdapter.GroupNameClickedListener mSelectedGroupListener
            = new ContactGroupIterationAdapter.GroupNameClickedListener() {
        @Override
        public void onGroupNameClicked(GroupChatRoom chatRoom) {
//            showDepartment();
            if(!limitedDepsList.contains(chatRoom.groupID)) {
                limitedDepsList.add(chatRoom.groupID);
            } else {
                limitedDepsList.remove(chatRoom.groupID);
            }

            limitedDepAdapter.notifyDataSetChanged();
            mGroupIterationAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSendMsgClicked() {
        }
    };
    private ContactGroupIterationAdapter mGroupIterationAdapter;

    private void doShowDepartment() {
        ListView lvDepartment=(ListView) findViewById(R.id.lv_department);

        if (null == mGroupIterationAdapter) {
            mGroupIterationAdapter = new ContactGroupIterationAdapter(this, rootGroups, mSelectedGroupListener);
            mGroupIterationAdapter.setShowGroupNameOnly(true,true,limitedDepsList);
            lvDepartment.setAdapter(mGroupIterationAdapter);
        }

        if(View.GONE == departmentLayout.getVisibility()) {
            departmentLayout.setVisibility(View.VISIBLE);
            btnTitleLeft.setImageResource(R.drawable.nav_close_selector);
        } else {
            departmentLayout.setVisibility(View.GONE);
            btnTitleLeft.setImageResource(R.drawable.nav_back_selector);
        }
        mGroupIterationAdapter.notifyDataSetChanged();
    }

    private void showDepartment() {
        if(null == departmentList) {
            mMsgBox.showWait();
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    departmentList = mDb.fetchNonTempGroupChatRooms(true);

                    rootGroups = new ArrayList<GroupChatRoom>();
                    rootGroups.addAll(treeAllGroupRooms(ShareRangeSelectActivity.this, departmentList));
                    return null;
                }

                @Override
                protected void onPostExecute(Integer newReviewsCount) {
                    mMsgBox.dismissWait();
                    doShowDepartment();
                }
            });
        } else {
            doShowDepartment();
        }
    }

    @Override
    public void onBackPressed() {
        if(View.VISIBLE == departmentLayout.getVisibility()) {
            showDepartment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.share_range_all_layout:
                if(!isShownOnly) {
                    setShareRangeStatus(true);
                }
                break;
            case R.id.tv_add_selected_department:
                showDepartment();
                //continue execute
            case R.id.share_range_limited_layout:
                if(!isShownOnly) {
                    setShareRangeStatus(false);
                }
                break;
            case R.id.title_back:
                if(View.GONE == departmentLayout.getVisibility()) {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                } else {
                    showDepartment();
                }
                break;
            case R.id.title_share_range_confirm:
                if(shareRangeTag == Moment.VISIBVILITY_LIMITED &&
                        limitedDepsList.size() <= 0) {
                    mMsgBox.toast(R.string.limited_deps_can_not_empty);
                    return;
                }
                setResult(Activity.RESULT_OK);

                Intent intent= new Intent();
                intent.putExtra(SHARE_RANGE_TAG, shareRangeTag);
                intent.putStringArrayListExtra(LITMITED_DEPS, limitedDepsList);
                setResult(RESULT_OK, intent);

                finish();
                break;
            default:
                break;
        }
    }
}
