package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.Iterator;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.PendingRequest;
import org.wowtalk.ui.msg.RoundedImageView;
import co.onemeter.oneapp.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

public class GroupJoinRequestActivity extends Activity implements View.OnClickListener {
    public static final String INTENT_DATA_ACCEPT = "accept";
    public static final String INTENT_DATA_REJECT = "reject";
    public static final String INTENT_DATA_BUDDY= "buddy";
    public static final String GROUP_ID= "group_id";

    private static final int REQ_APPLY_MANAGE = 1;

    private class ApplyAdapter extends BaseAdapter {
        private Context mContext;

        public ApplyAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mPendingRequests.size();
        }

        @Override
        public Object getItem(int position) {
            return mPendingRequests.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PendingRequest p = mPendingRequests.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_group_apply, parent, false);
            }
            RoundedImageView imgPhoto = (RoundedImageView) convertView.findViewById(R.id.img_photo);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            imgPhoto.setBackgroundDrawable(null);
            Buddy b = new Buddy(p.uid);
            b.photoUploadedTimeStamp = p.buddy_photo_timestamp;
            PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_avatar_90, b, true);
            txtName.setText(p.nickname);
            return convertView;
        }
    }

    private ImageButton mBtnTitleBack;
    private TextView mTitleText;

    private ListView mListApply;

    private ArrayList<PendingRequest> mPendingRequests;

    private ApplyAdapter mApplyAdapter;
    private Database mDBHelper;
    private String mGroupId;

    private void initView() {
        mBtnTitleBack = (ImageButton) findViewById(R.id.title_back);
        mTitleText = (TextView) findViewById(R.id.title_text);
        mListApply = (ListView) findViewById(R.id.list_apply);

        mBtnTitleBack.setOnClickListener(this);
        String strTitle = getString(R.string.group_apply);
        if (mPendingRequests == null || mPendingRequests.size() == 0) {
            mTitleText.setText(strTitle.substring(0, strTitle.length() - 4));
        } else {
            mTitleText.setText(String.format(strTitle, mPendingRequests.size()));
        }

        mApplyAdapter = new ApplyAdapter(GroupJoinRequestActivity.this);
        mListApply.setAdapter(mApplyAdapter);
        mListApply.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PendingRequest p = mPendingRequests.get(position);
                ApplyInfoActivity.launch(GroupJoinRequestActivity.this, p, REQ_APPLY_MANAGE);
            }
        });
    }

    private void getPendings() {
        mPendingRequests = new ArrayList<PendingRequest>();
        ArrayList<PendingRequest> pendings = new ArrayList<PendingRequest>();
        mDBHelper.fetchPendingRequest(pendings);
        for (PendingRequest p : pendings) {
            if ((p.group_id != null && p.group_id.equals(mGroupId))
                    && p.type == PendingRequest.GROUP_ADMIN) {
                mPendingRequests.add(p);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                setResult(RESULT_OK);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_APPLY_MANAGE && resultCode == RESULT_OK) {
            int id = data.getIntExtra("pid", 0);
            PendingRequest request = null;
            for (Iterator<PendingRequest> iterator = mPendingRequests.iterator(); iterator.hasNext();) {
                request = iterator.next();
                if (id == request.id) {
                    iterator.remove();
                    mApplyAdapter.notifyDataSetChanged();
                    String strTitle = getString(R.string.group_apply);
                    if (mPendingRequests == null || mPendingRequests.isEmpty()) {
                        mTitleText.setText(strTitle.substring(0, strTitle.length() - 4));
                    } else {
                        mTitleText.setText(String.format(strTitle, mPendingRequests.size()));
                    }
                    break;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_join_request);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        Intent intent = getIntent();
        if (null != intent) {
            mGroupId = intent.getStringExtra(GROUP_ID);
        }
        mDBHelper = new Database(this);
        getPendings();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
