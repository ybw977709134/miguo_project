package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.ImageViewActivity;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ContactInfoActivity extends Activity implements OnClickListener{
    private static final int MOMENT_NUMBER = 3;

	private static final String EXTRA_BUDDY_TYPE = "buddy_type";
	private static final String EXTRA_BUDDY_DETAIL = "buddy_detail";

	public static final int BUDDY_TYPE_IS_FRIEND = 1001;
	public static final int BUDDY_TYPE_NOT_FRIEND = 1002;
	public static final int BUDDY_TYPE_NOT_USER = 1003;
    public static final int BUDDY_TYPE_UNKNOWN = 0;
    public static final int BUDDY_TYPE_MYSELF = 1;

//	private static final int MSG_FETCH_MOMENT_SUCCESS = 100;

    private static final String[] IMAGE_TYPES = new String[] {
            "bmp",
            "jpg",
            "tiff",
            "gif",
            "pcx",
            "tga",
            "exif",
            "fpx",
            "svg",
            "psd",
            "cdr",
            "pcd",
            "dxf",
            "ufo",
            "eps",
            "ai",
            "raw"
    };

    private ImageView mGenderImg;
    private TextView mFriendNameBiz;
    private TextView mPronunciation;
    private LinearLayout mBtnBizlayout;
    private LinearLayout mStatusLayout;
    private TextView mStatusTextView;

    private LinearLayout mLayout3;
    private LinearLayout mMomentLayout;

    private LinearLayout mDeptJobLayout;
    private LinearLayout mDeptLayout;
    private TextView mDeptTextView;
    private ImageView mDividerDeptJob;
    private LinearLayout mJobTitleLayout;
    private TextView mJobTitleTextView;

    private LinearLayout mContactWayLayout;
    private LinearLayout mPhoneLayout;
    private TextView mPhoneTextView;
    private ImageView mDividerPhobeMobile;
    private LinearLayout mMobileLayout;
    private TextView mMobileTextView;
    private ImageView mDividerMobileEmail;
    private LinearLayout mEmailLayout;
    private TextView mEmailTextView;

    private LinearLayout mBranchStoreEmployeeIdLayout;
    private LinearLayout mBranchStoreLayout;
    private TextView mBranchStoreTextView;
    private ImageView mDividerBranchStoreEmployeeId;
    private LinearLayout mEmployeeIdLayout;
    private TextView mEmployeeIdTextView;

    private Database dbHelper;

    private Person person; // access Person only if Buddy is unavailable.
    /**
     * 用于记录初始的buddy的部分信息，比较其是否发生变化，变化则通知联系人了列表界面刷新
     */
    private Buddy mInitBuddy;
    private Buddy buddy;
    private ArrayList<Moment> moments;

    private int buddyType;

    private MessageBox mMsgBox = null;

    private ImageView imgPhoto;

    private void resetViewsForBiz() {
        mGenderImg.setBackgroundResource(buddy.getSexFlag() == Buddy.SEX_FEMALE ? R.drawable.avatar_female : R.drawable.avatar_male);
        setFriendInfoForBiz();
        setStatus();
        setDeptJobTitle();
        setContactWay();
        setBranchStoreEmployeeId();
        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
    }

    /**
     * 头像右侧的个人信息
     */
    private void initFriendInfoForBiz() {
        AQuery q = new AQuery(this);

        mFriendNameBiz = (TextView) findViewById(R.id.friend_name_biz);
        mPronunciation = (TextView) findViewById(R.id.friend_name_pronunciation);
        mBtnBizlayout = (LinearLayout) findViewById(R.id.layout_btn_biz);

        q.find(R.id.btn_msg).clicked(this);
        q.find(R.id.btn_call).clicked(this);
        q.find(R.id.btn_video).clicked(this);

        setFriendInfoForBiz();
    }

    private void setFriendInfoForBiz() {
        // 默认男性
        mFriendNameBiz.setText(buddy.nickName);
        mPronunciation.setText(buddy.pronunciation);

        if (buddyType == BUDDY_TYPE_MYSELF) {
            mBtnBizlayout.setVisibility(View.GONE);
        }
    }

    private void initStatus() {
        mStatusLayout = (LinearLayout) findViewById(R.id.status_layout);
        mStatusTextView = (TextView) findViewById(R.id.txt_status);
        setStatus();
    }

    private void setStatus() {
        if (TextUtils.isEmpty(buddy.status)) {
            mStatusLayout.setVisibility(View.GONE);
        } else {
            mStatusLayout.setVisibility(View.VISIBLE);
            mStatusTextView.setText(buddy.status);
        }
    }

    /**
     * 部门，职位
     */
    private void initDeptJobTitle() {
        mDeptJobLayout = (LinearLayout)findViewById(R.id.layout_dept_job);
        mDeptLayout = (LinearLayout)findViewById(R.id.dept_layout);
        mDeptTextView = (TextView)findViewById(R.id.txt_dept);
        mDividerDeptJob = (ImageView) findViewById(R.id.divider_dept_job);
        mJobTitleLayout = (LinearLayout)findViewById(R.id.layout_job_title);
        mJobTitleTextView = (TextView)findViewById(R.id.txt_job_title);

        setDeptJobTitle();
    }

    private void setDeptJobTitle() {
        int emptyLayoutCount = 0;
        if (TextUtils.isEmpty(buddy.deptName)) {
            mDeptLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mDeptLayout.setVisibility(View.VISIBLE);
            mDeptTextView.setText(buddy.deptName);
        }

        if (TextUtils.isEmpty(buddy.jobTitle)) {
            mJobTitleLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mJobTitleLayout.setVisibility(View.VISIBLE);
            mJobTitleTextView.setText(buddy.jobTitle);
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mDeptJobLayout.getLayoutParams();
        switch (emptyLayoutCount) {
        case 0:
            mDeptJobLayout.setVisibility(View.VISIBLE);
            mDividerDeptJob.setVisibility(View.VISIBLE);
            params.height = 2 * getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mDeptJobLayout.setLayoutParams(params);
            break;
        case 1:
            mDeptJobLayout.setVisibility(View.VISIBLE);
            mDividerDeptJob.setVisibility(View.GONE);
            params.height = getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mDeptJobLayout.setLayoutParams(params);
            break;
        case 2:
            mDeptJobLayout.setVisibility(View.GONE);
            break;
        default:
            break;
        }
    }

    /**
     * 联系方式
     */
    private void initContactWay() {
        mContactWayLayout = (LinearLayout)findViewById(R.id.layout_contact_way);
        mPhoneLayout = (LinearLayout)findViewById(R.id.layout_phone);
        mPhoneTextView = (TextView)findViewById(R.id.txt_phone);
        mDividerPhobeMobile = (ImageView) findViewById(R.id.divider_phone_mobile);
        mMobileLayout = (LinearLayout)findViewById(R.id.layout_mobile);
        mMobileTextView = (TextView)findViewById(R.id.txt_mobile);
        mDividerMobileEmail = (ImageView) findViewById(R.id.divider_mobile_email);
        mEmailLayout = (LinearLayout)findViewById(R.id.layout_email);
        mEmailTextView = (TextView)findViewById(R.id.txt_email);

        mPhoneTextView.setOnClickListener(this);
        mMobileTextView.setOnClickListener(this);
        mEmailTextView.setOnClickListener(this);

        setContactWay();
    }

    private void setContactWay() {
        boolean isHasPhone = false;
        if (TextUtils.isEmpty(buddy.phoneNumber)) {
            mPhoneLayout.setVisibility(View.GONE);
        } else {
            isHasPhone = true;
            mPhoneLayout.setVisibility(View.VISIBLE);
            mPhoneTextView.setText(buddy.phoneNumber);
        }

        boolean isHasMobile = false;
        if (TextUtils.isEmpty(buddy.mobile)) {
            mMobileLayout.setVisibility(View.GONE);
        } else {
            isHasMobile = true;
            mMobileLayout.setVisibility(View.VISIBLE);
            mMobileTextView.setText(buddy.mobile);
        }

        boolean isHasEmail = false;
        if (TextUtils.isEmpty(buddy.getEmail())) {
            mEmailLayout.setVisibility(View.GONE);
        } else {
            isHasEmail = true;
            mEmailLayout.setVisibility(View.VISIBLE);
            mEmailTextView.setText(buddy.getEmail());
        }

        // 设置divider
        if (isHasPhone && (isHasMobile || isHasEmail)) {
            mDividerPhobeMobile.setVisibility(View.VISIBLE);
        } else {
            mDividerPhobeMobile.setVisibility(View.GONE);
        }
        if (isHasMobile && isHasEmail) {
            mDividerMobileEmail.setVisibility(View.VISIBLE);
        } else {
            mDividerMobileEmail.setVisibility(View.GONE);
        }

        if (!isHasPhone && !isHasMobile && !isHasEmail) {
            mContactWayLayout.setVisibility(View.GONE);
        } else {
            int heightCount = 0;
            if (isHasPhone) {
                heightCount++;
            }
            if (isHasMobile) {
                heightCount++;
            }
            if (isHasEmail) {
                heightCount++;
            }
            mContactWayLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mContactWayLayout.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.section_row_height) * heightCount;
            mContactWayLayout.setLayoutParams(params);
        }
    }

    /**
     * 支店，工号
     */
    private void initBranchStoreEmployeeId() {
        mBranchStoreEmployeeIdLayout = (LinearLayout)findViewById(R.id.layout_branchstore_employeeid);
        mBranchStoreLayout = (LinearLayout)findViewById(R.id.layout_branch_store);
        mBranchStoreTextView = (TextView)findViewById(R.id.txt_branch_store);
        mDividerBranchStoreEmployeeId = (ImageView) findViewById(R.id.divider_branchstore_employeeid);
        mEmployeeIdLayout = (LinearLayout)findViewById(R.id.layout_employee_id);
        mEmployeeIdTextView = (TextView)findViewById(R.id.txt_employee_id);

        setBranchStoreEmployeeId();
    }

    private void setBranchStoreEmployeeId() {
        int emptyLayoutCount = 0;
        if (TextUtils.isEmpty(buddy.area)) {
            mBranchStoreLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mBranchStoreLayout.setVisibility(View.VISIBLE);
            mBranchStoreTextView.setText(buddy.area);
        }

        if (TextUtils.isEmpty(buddy.employeeId)) {
            mEmployeeIdLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mEmployeeIdLayout.setVisibility(View.VISIBLE);
            mEmployeeIdTextView.setText(buddy.employeeId);
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mBranchStoreEmployeeIdLayout.getLayoutParams();
        switch (emptyLayoutCount) {
        case 0:
            mBranchStoreEmployeeIdLayout.setVisibility(View.VISIBLE);
            mDividerBranchStoreEmployeeId.setVisibility(View.VISIBLE);
            params.height = 2 * getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mBranchStoreEmployeeIdLayout.setLayoutParams(params);
            break;
        case 1:
            mBranchStoreEmployeeIdLayout.setVisibility(View.VISIBLE);
            mDividerBranchStoreEmployeeId.setVisibility(View.GONE);
            params.height = getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mBranchStoreEmployeeIdLayout.setLayoutParams(params);
            break;
        case 2:
            mBranchStoreEmployeeIdLayout.setVisibility(View.GONE);
            break;
        default:
            break;
        }
    }

    private void initLayout3() {
        getMomentsByUserId(buddy.userID);
    }

    private void initView() {
        AQuery q = new AQuery(this);

        imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
        mLayout3 = (LinearLayout) findViewById(R.id.layout3);//moment show
        mMomentLayout = (LinearLayout) findViewById(R.id.moment_layout);
        mGenderImg = (ImageView) findViewById(R.id.avatar_gender);
        mGenderImg.setBackgroundResource(buddy.getSexFlag() == Buddy.SEX_FEMALE
                ? R.drawable.avatar_female : R.drawable.avatar_male);
        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, buddy, true);

        initFriendInfoForBiz();
        initStatus();
        initDeptJobTitle();
        initContactWay();
        initBranchStoreEmployeeId();
        initLayout3();

        q.find(R.id.img_thumbnail).clicked(this);
        q.find(R.id.navbar_btn_left).clicked(this);
        q.find(R.id.btn_goto_moments).clicked(this);

        //if family buddy,no moment layout
//        if(buddy.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
//            findViewById(R.id.layout3).setVisibility(View.GONE);
//            findViewById(R.id.btn_goto_moments).setVisibility(View.GONE);
//        }
    }

    /**
     * 获取联系人的详情信息，刚开始没有下载，每次加载时下载
     */
    private void getBuddyInfoFromServer() {
        if (null == buddy || TextUtils.isEmpty(buddy.userID)) {
            return;
        }

        new AsyncTask<Void, Void, Integer> () {
            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WowTalkWebServerIF.getInstance(ContactInfoActivity.this)
                        .fGetBuddyWithUID(buddy.userID);
                if (resultCode == ErrorCode.OK) {
                    buddy = dbHelper.buddyWithUserID(buddy.userID);
                    // 获取buddy的部门,此部门为组合的部门信息，不是纯粹的单个部门
                    GroupChatRoom dept = dbHelper.getDeptInfoByMemberId(buddy.userID);
                    // 如果不属于任何子部门，则隐藏
                    if (null != dept && dept.isEditable) {
                        buddy.deptName = dept.groupNameOriginal;
                    } else {
                        buddy.deptName = null;
                    }
                }
                return resultCode;
            }

            protected void onPostExecute(Integer resultCode) {
                if (resultCode == ErrorCode.OK) {
                    resetViewsForBiz();
                }
            };
        }.execute((Void)null);
    }

	private void getMomentsByUserId(final String userId) {
        moments = dbHelper.fetchMomentsOfSingleBuddy(userId, 0, 100);
        if (moments == null || moments.size() == 0) {
            mLayout3.setVisibility(View.GONE);
            return;
        }
        ArrayList<WFile> listPath = new ArrayList<WFile>();
        for (Moment moment : moments) {
            for (WFile file : moment.multimedias) {
//                if(file.getExt().equals("jpg") || file.getExt().equals("png")) {
                if (Arrays.asList(IMAGE_TYPES).contains(file.getExt())) {
                    listPath.add(file);
                }
            }
        }
        if (listPath == null || listPath.size() == 0) {
            mLayout3.setVisibility(View.GONE);
        } else {
            mLayout3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimelineActivity.launch(ContactInfoActivity.this, buddy.userID, buddy.nickName);
                }
            });
            mLayout3.setVisibility(View.VISIBLE);
            int imageNum;
            if (listPath.size() <= MOMENT_NUMBER) {
                imageNum = listPath.size();
            } else {
                imageNum = MOMENT_NUMBER;
            }
            for (int i = 0; i < imageNum; i++) {
                final ImageView imageView = new ImageView(ContactInfoActivity.this);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                File file = new File(PhotoDisplayHelper.makeLocalFilePath(listPath.get(i).thumb_fileid,
                        listPath.get(i).getExt()));
                if (!file.exists()) {
                    imageView.setTag(file.getAbsolutePath());
                    imageView.setImageResource(R.drawable.default_avatar_90);
                    PhotoDisplayHelper.displayPhoto(ContactInfoActivity.this, imageView,
                            R.drawable.default_avatar_90, listPath.get(i).thumb_fileid, listPath.get(i).getExt(), null,
                            new PhotoDisplayHelper.OnFileDownloadedListener() {
                        @Override
                        public void onFileDownloaded(String localPath) {
                            imageView.setImageDrawable(new BitmapDrawable(ContactInfoActivity.this.getResources(), localPath));
                        }
                    });
                } else {
                    imageView.setImageDrawable(new BitmapDrawable(ContactInfoActivity.this.getResources(), file.getAbsolutePath()));
                }
                mMomentLayout.addView(imageView);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imageView.getLayoutParams();
                params.width = DensityUtil.dip2px(ContactInfoActivity.this, 50);
                params.height = DensityUtil.dip2px(ContactInfoActivity.this, 50);
                params.setMargins(0, 0, DensityUtil.dip2px(ContactInfoActivity.this, 5), 0);
                imageView.setLayoutParams(params);
            }
        }
	}

    public static void fSendSmsToInvite(Context context,Person person) {
        String phoneNumber = person.getGlobalPhoneNumber();
        Uri smsUri = Uri.parse("smsto:" + phoneNumber);
        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
        intent.putExtra("sms_body", context.getString(R.string.setting_tell_friend_sms_content));
        context.startActivity(intent);
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.navbar_btn_left:
                handleBackEvent();
                break;
            case R.id.btn_msg:
                chatWith(buddy.userID, buddy.nickName);
                break;
            case R.id.btn_call:
                CallMainActivity.startNewOutGoingCall(ContactInfoActivity.this, buddy.userID, buddy.nickName, false);
                break;
            case R.id.btn_video:
                CallMainActivity.startNewOutGoingCall(ContactInfoActivity.this, buddy.userID, buddy.nickName, true);
                break;
            case R.id.img_thumbnail:
                ImageViewActivity.launch(this, buddy);
                break;
            case R.id.txt_phone:
                String phoneNumber = mPhoneTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(phoneNumber)) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                    startActivity(callIntent);
                }
                break;
            case R.id.txt_mobile:
                String mobileNumber = mMobileTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(mobileNumber)) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mobileNumber));
                    startActivity(callIntent);
                }
                break;
            case R.id.txt_email:
                String emailAddr = mEmailTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(emailAddr)) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAddr));
                    startActivity(Intent.createChooser(emailIntent, null));
                }
                break;
            case R.id.btn_goto_moments:
                TimelineActivity.launch(this, buddy.userID, buddy.nickName);
                break;
            default:
                break;
        }
    }

    private void handleBackEvent() {
        boolean isChanged = true;
        // pronunciation,status,photoUploadTimeStamp全相同，则认为buddy详情未变化
        if (((TextUtils.isEmpty(mInitBuddy.pronunciation) && TextUtils.isEmpty(buddy.pronunciation))
                    || (!TextUtils.isEmpty(mInitBuddy.pronunciation) && mInitBuddy.pronunciation.equals(buddy.pronunciation)))
                && ((TextUtils.isEmpty(mInitBuddy.status) && TextUtils.isEmpty(buddy.status))
                    || (!TextUtils.isEmpty(mInitBuddy.status) && mInitBuddy.status.equals(buddy.status)))
                && mInitBuddy.photoUploadedTimeStamp == buddy.photoUploadedTimeStamp) {
            isChanged = false;
        }
        Intent data = new Intent();
        data.putExtra("uid", buddy.userID);
        data.putExtra("is_changed", isChanged);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        handleBackEvent();
    }

    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_info);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		buddyType = getIntent().getIntExtra(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_USER);
		person = getIntent().getParcelableExtra(EXTRA_BUDDY_DETAIL);
		Log.i("person signature : " + person.getPersonState()
				+ " rigion : " + person.getRigion()
				+ " photoUploadedTimestamp: " + person.photoUploadedTimestamp
				+ " wowtalkId : " + person.getWowTalkId());
        dbHelper = new Database(this);
		buddy = dbHelper.buddyWithUserID(person.getID());
		buddyType = getIntent().getIntExtra(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_USER);
		person = getIntent().getParcelableExtra(EXTRA_BUDDY_DETAIL);
		Database db = new Database(this);
		buddy = db.buddyWithUserID(person.getID());
		if(buddy == null) {
            buddy=person.toBuddy();
        } else {
            // 获取buddy的部门,此部门为组合的部门信息，不是纯粹的单个部门
            GroupChatRoom dept = dbHelper.getDeptInfoByMemberId(buddy.userID);
            // 如果不属于任何子部门，则隐藏
            if (null != dept && dept.isEditable) {
                buddy.deptName = dept.groupNameOriginal;
            } else {
                buddy.deptName = null;
            }
        }
        // 填充用于判断联系人的以下四个属性是否变化的Buddy
        mInitBuddy = new Buddy();
        mInitBuddy.userID = buddy.userID;
        mInitBuddy.pronunciation = buddy.pronunciation;
        mInitBuddy.photoUploadedTimeStamp = buddy.photoUploadedTimeStamp;
        mInitBuddy.status = buddy.status;

        mMsgBox = new MessageBox(this);

        initView();

        getBuddyInfoFromServer();
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

    private void chatWith(String uid, String phoneNumber) {
		Intent intent = new Intent(this,
				MessageComposerActivity.class);
		intent.putExtra(MessageComposerActivity.KEY_TARGET_UID, 
				uid);
		intent.putExtra(
				MessageComposerActivity.KEY_TARGET_DISPLAYNAME, 
				phoneNumber);
		startActivity(intent);
	}

	/**
	 * 
	 * @param context
	 * @param buddyUid
	 * @param buddyType BUDDY_TYPE_*
	 */
	public static void launch(final Context context, final String buddyUid, final int buddyType) {
		Database db = new Database(context);
		Buddy b = db.buddyWithUserID(buddyUid);
        if (b != null) {
            if (b.getAccountType() == Buddy.ACCOUNT_TYPE_NOTICE_MAN) {
                return;
            }
            launch(context, Person.fromBuddy(b), buddyType);
        } else {
            final MessageBox m = new MessageBox(context);
            m.showWait();
            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(context);
                    return web.fGetBuddyWithUID(buddyUid);
                }
                @Override
                protected void onPostExecute(Integer errno) {
                    m.dismissWait();
                    if (ErrorCode.OK == errno) {
                        Database db = new Database(context);
                        Buddy b = db.buddyWithUserID(buddyUid);
                        if (b != null) {
                            launch(context, Person.fromBuddy(b), buddyType);
                        }
                    }
                }
            }.execute((Void)null);
        }
	}

	/**
	 * 
	 * @param context
	 * @param person
	 * @param buddyType BUDDY_TYPE_*
	 */
	public static void launch(Context context, Person person,
			int buddyType) {
		
		Intent i = new Intent(context, ContactInfoActivity.class);
		Database db = new Database(context);
		Bundle bu = new Bundle();
        if (buddyType == BUDDY_TYPE_UNKNOWN) {
            Buddy b = db.buddyWithUserID(person.getID());
            if (b != null) {
                String myselfId = PrefUtil.getInstance(context).getUid();
                if (!TextUtils.isEmpty(myselfId) && myselfId.equals(b.userID)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_MYSELF);
                } else if (0 != (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_IS_FRIEND);
                } else {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
                }
            } else {
                bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
            }
        } else {
            bu.putInt(EXTRA_BUDDY_TYPE, buddyType);
        }
		bu.putParcelable(EXTRA_BUDDY_DETAIL, person);
		i.putExtras(bu);
		context.startActivity(i);
	}

    public static void launchForResult(Activity activity, int requestCode,
            Person person, int buddyType) {

        Intent intent = new Intent(activity, ContactInfoActivity.class);
        Database db = new Database(activity);
        Bundle bundle = new Bundle();
        if (buddyType == BUDDY_TYPE_UNKNOWN) {
            Buddy buddy = db.buddyWithUserID(person.getID());
            if (buddy != null) {
                String myselfId = PrefUtil.getInstance(activity).getUid();
                if (!TextUtils.isEmpty(myselfId) && myselfId.equals(buddy.userID)) {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_MYSELF);
                } else if (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_IS_FRIEND);
                } else {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
                }
            } else {
                bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
            }
        } else {
            bundle.putInt(EXTRA_BUDDY_TYPE, buddyType);
        }
        bundle.putParcelable(EXTRA_BUDDY_DETAIL, person);
        intent.putExtras(bundle);
        activity.startActivityForResult(intent, requestCode);
    }
}
